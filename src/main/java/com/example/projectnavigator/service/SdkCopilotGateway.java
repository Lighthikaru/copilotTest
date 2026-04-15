package com.example.projectnavigator.service;

import com.example.projectnavigator.config.AppProperties;
import com.example.projectnavigator.dto.ChatResponse;
import com.example.projectnavigator.dto.SourceRef;
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.CopilotSession;
import com.github.copilot.sdk.SystemMessageMode;
import com.github.copilot.sdk.events.AssistantMessageDeltaEvent;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SystemMessageConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class SdkCopilotGateway implements CopilotGateway {

    private final AppProperties properties;
    private final StubCopilotGateway fallbackGateway;

    public SdkCopilotGateway(AppProperties properties, StubCopilotGateway fallbackGateway) {
        this.properties = properties;
        this.fallbackGateway = fallbackGateway;
    }

    @Override
    public List<String> listModels() {
        try (CopilotClient client = new CopilotClient(buildOptions())) {
            client.start().get(30, TimeUnit.SECONDS);
            return client.listModels()
                    .get(30, TimeUnit.SECONDS)
                    .stream()
                    .map(model -> model.getId() == null || model.getId().isBlank() ? model.getName() : model.getId())
                    .filter(model -> model != null && !model.isBlank())
                    .distinct()
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    @Override
    public ChatResponse answer(
            String prompt,
            List<SourceRef> sources,
            String selectedModel,
            String workingDirectory) {
        try {
            return execute(prompt, sources, selectedModel, workingDirectory, null);
        } catch (Exception ex) {
            if (!properties.getChat().isAllowFallback()) {
                throw new IllegalStateException(describeFailure(ex), ex);
            }
            return withFallbackWarning(prompt, sources, selectedModel, workingDirectory, ex);
        }
    }

    @Override
    public void streamAnswer(
            String prompt,
            List<SourceRef> sources,
            String selectedModel,
            String workingDirectory,
            Consumer<String> chunkConsumer,
            Consumer<ChatResponse> completionConsumer) {
        try {
            completionConsumer.accept(execute(prompt, sources, selectedModel, workingDirectory, chunkConsumer));
        } catch (Exception ex) {
            if (!properties.getChat().isAllowFallback()) {
                throw new IllegalStateException(describeFailure(ex), ex);
            }
            fallbackGateway.streamAnswer(
                    prompt,
                    sources,
                    selectedModel,
                    workingDirectory,
                    chunkConsumer,
                    fallback -> completionConsumer.accept(new ChatResponse(
                            fallback.answer(),
                            fallback.sources(),
                            fallback.confidence(),
                            fallback.followUps(),
                            appendWarning(fallback.warnings(), "SDK fallback: " + ex.getMessage()),
                            fallback.usage())));
        }
    }

    private ChatResponse execute(
            String prompt,
            List<SourceRef> sources,
            String selectedModel,
            String workingDirectory,
            Consumer<String> chunkConsumer) throws Exception {
        try (CopilotClient client = new CopilotClient(buildOptions())) {
            client.start().get(30, TimeUnit.SECONDS);
            try (CopilotSession session = client.createSession(buildSession(selectedModel, workingDirectory, chunkConsumer != null))
                    .get(30, TimeUnit.SECONDS)) {
                if (chunkConsumer != null) {
                    session.on(AssistantMessageDeltaEvent.class, event -> {
                        if (event.getData() != null && event.getData().deltaContent() != null) {
                            chunkConsumer.accept(event.getData().deltaContent());
                        }
                    });
                }
                AssistantMessageEvent finalEvent = session.sendAndWait(buildMessage(prompt), 60_000L)
                        .get(90, TimeUnit.SECONDS);
                String answer = finalEvent.getData() == null ? null : finalEvent.getData().content();
                return buildResponse(answer, sources, selectedModel, prompt.length(), List.of());
            }
        }
    }

    private CopilotClientOptions buildOptions() {
        return new CopilotClientOptions()
                .setAutoStart(true)
                .setAutoRestart(false)
                .setUseStdio(true)
                .setUseLoggedInUser(true);
    }

    private SessionConfig buildSession(String selectedModel, String workingDirectory, boolean streaming) {
        return new SessionConfig()
                .setClientName("project-navigator")
                .setModel(selectedModel == null || selectedModel.isBlank() ? "gpt-5.4" : selectedModel)
                .setStreaming(streaming)
                .setWorkingDirectory(workingDirectory)
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setSystemMessage(new SystemMessageConfig()
                        .setMode(SystemMessageMode.APPEND)
                        .setContent("""
                                You are helping a business analyst and solution architect understand a codebase.
                                Use only the supplied project context.
                                Answer in Traditional Chinese and cite file paths when relevant.
                                If confidence is low, say so clearly.
                                """));
    }

    private MessageOptions buildMessage(String prompt) {
        return new MessageOptions()
                .setPrompt(prompt)
                .setMode("chat");
    }

    private ChatResponse buildResponse(
            String answer,
            List<SourceRef> sources,
            String selectedModel,
            int promptLength,
            List<String> warnings) {
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("model", selectedModel == null || selectedModel.isBlank() ? "gpt-5.4" : selectedModel);
        usage.put("contextCharacters", promptLength);
        usage.put("sourceCount", sources.size());

        return new ChatResponse(
                answer == null || answer.isBlank() ? "模型未回傳內容。" : answer,
                sources,
                sources.isEmpty() ? 0.4 : 0.82,
                List.of("可以再指定一支 API、排程或資料表，讓分析更聚焦。"),
                warnings,
                usage);
    }

    private ChatResponse withFallbackWarning(
            String prompt,
            List<SourceRef> sources,
            String selectedModel,
            String workingDirectory,
            Exception exception) {
        ChatResponse fallback = fallbackGateway.answer(prompt, sources, selectedModel, workingDirectory);
        return new ChatResponse(
                fallback.answer(),
                fallback.sources(),
                fallback.confidence(),
                fallback.followUps(),
                appendWarning(fallback.warnings(), "SDK fallback: " + exception.getMessage()),
                fallback.usage());
    }

    private List<String> appendWarning(List<String> existing, String warning) {
        return Stream.concat(existing.stream(), Stream.of(warning)).toList();
    }

    private String describeFailure(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getMessage();
        }
        if (message == null || message.isBlank()) {
            message = current.getClass().getSimpleName();
        }
        return "Copilot session failed: " + message;
    }
}
