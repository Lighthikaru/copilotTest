package com.example.projectnavigator.service;

import com.example.projectnavigator.dto.ChatRequest;
import com.example.projectnavigator.dto.ChatResponse;
import com.example.projectnavigator.dto.ConversationDetail;
import com.example.projectnavigator.dto.ProjectConnection;
import com.example.projectnavigator.dto.ProjectIndex;
import com.example.projectnavigator.repository.ProjectIndexRepository;
import com.example.projectnavigator.repository.ProjectRepository;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ChatService {

    private final ProjectRepository projectRepository;
    private final ProjectIndexRepository projectIndexRepository;
    private final ConversationService conversationService;
    private final ChatContextAssembler contextAssembler;
    private final CopilotGateway copilotGateway;

    public ChatService(
            ProjectRepository projectRepository,
            ProjectIndexRepository projectIndexRepository,
            ConversationService conversationService,
            ChatContextAssembler contextAssembler,
            CopilotGateway copilotGateway) {
        this.projectRepository = projectRepository;
        this.projectIndexRepository = projectIndexRepository;
        this.conversationService = conversationService;
        this.contextAssembler = contextAssembler;
        this.copilotGateway = copilotGateway;
    }

    public void stream(ChatRequest request, SseEmitter emitter) {
        try {
            ProjectConnection connection = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.projectId()));
            ProjectIndex index = projectIndexRepository.findByProjectId(request.projectId())
                    .orElseThrow(() -> new IllegalArgumentException("Project is not indexed yet."));
            ConversationDetail conversation = request.conversationId() == null || request.conversationId().isBlank()
                    ? conversationService.create(request.projectId(), null)
                    : conversationService.ensureConversation(request.projectId(), request.conversationId());
            String conversationId = conversation.id();
            conversationService.appendUserMessage(
                    request.projectId(),
                    conversationId,
                    request.question(),
                    request.selectedModel() == null || request.selectedModel().isBlank() ? "gpt-5.4" : request.selectedModel(),
                    request.mode());
            String summary = conversationService.contextSummary(request.projectId(), conversationId);

            ChatContextAssembler.ContextPack contextPack =
                    contextAssembler.build(connection, index, request.question(), request.mode(), summary);

            emitter.send(SseEmitter.event().name("meta").data(Map.of(
                    "conversationId", conversationId,
                    "warningCount", contextPack.warnings().size(),
                    "sourceCount", contextPack.sources().size())));

            copilotGateway.streamAnswer(
                    contextPack.prompt(),
                    contextPack.sources(),
                    request.selectedModel(),
                    connection.workspacePath() != null ? connection.workspacePath() : connection.localPath(),
                    chunk -> sendChunk(emitter, "chunk", chunk),
                    response -> completeConversation(request, conversationId, contextPack, response, emitter));
        } catch (Exception ex) {
            sendChunk(emitter, "error", ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "Chat request failed."
                    : ex.getMessage());
            emitter.complete();
        }
    }

    private void completeConversation(
            ChatRequest request,
            String conversationId,
            ChatContextAssembler.ContextPack contextPack,
            ChatResponse response,
            SseEmitter emitter) {
        conversationService.appendAssistantMessage(
                request.projectId(),
                conversationId,
                response.answer(),
                response.sources(),
                request.selectedModel() == null || request.selectedModel().isBlank() ? "gpt-5.4" : request.selectedModel(),
                response.usage());

        try {
            emitter.send(SseEmitter.event().name("complete").data(response));
            if (!contextPack.warnings().isEmpty()) {
                emitter.send(SseEmitter.event().name("warnings").data(contextPack.warnings()));
            }
            emitter.complete();
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    private void sendChunk(SseEmitter emitter, String event, String data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }
}
