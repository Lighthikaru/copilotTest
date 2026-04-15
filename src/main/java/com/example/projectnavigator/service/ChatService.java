package com.example.projectnavigator.service;

import com.example.projectnavigator.dto.ChatRequest;
import com.example.projectnavigator.dto.ChatResponse;
import com.example.projectnavigator.dto.ProjectConnection;
import com.example.projectnavigator.dto.ProjectIndex;
import com.example.projectnavigator.repository.ConversationRepository;
import com.example.projectnavigator.repository.ProjectIndexRepository;
import com.example.projectnavigator.repository.ProjectRepository;
import com.example.projectnavigator.util.JsonCodec;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ChatService {

    private final ProjectRepository projectRepository;
    private final ProjectIndexRepository projectIndexRepository;
    private final ConversationRepository conversationRepository;
    private final ChatContextAssembler contextAssembler;
    private final CopilotGateway copilotGateway;
    private final JsonCodec jsonCodec;

    public ChatService(
            ProjectRepository projectRepository,
            ProjectIndexRepository projectIndexRepository,
            ConversationRepository conversationRepository,
            ChatContextAssembler contextAssembler,
            CopilotGateway copilotGateway,
            JsonCodec jsonCodec) {
        this.projectRepository = projectRepository;
        this.projectIndexRepository = projectIndexRepository;
        this.conversationRepository = conversationRepository;
        this.contextAssembler = contextAssembler;
        this.copilotGateway = copilotGateway;
        this.jsonCodec = jsonCodec;
    }

    public void stream(ChatRequest request, SseEmitter emitter) {
        try {
            ProjectConnection connection = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.projectId()));
            ProjectIndex index = projectIndexRepository.findByProjectId(request.projectId())
                    .orElseThrow(() -> new IllegalArgumentException("Project is not indexed yet."));
            String conversationId = request.conversationId() == null || request.conversationId().isBlank()
                    ? UUID.randomUUID().toString()
                    : request.conversationId();
            String summary = conversationRepository.findSummary(conversationId).orElse(null);

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
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("lastQuestion", request.question());
        summary.put("lastAnswer", response.answer());
        summary.put("mode", request.mode());
        summary.put("sources", response.sources().stream().map(source -> source.path()).toList());
        conversationRepository.saveSummary(conversationId, request.projectId(), jsonCodec.write(summary));

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
