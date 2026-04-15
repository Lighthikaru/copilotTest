package com.example.projectnavigator.service;

import com.example.projectnavigator.dto.ChatResponse;
import java.util.List;
import java.util.function.Consumer;

public interface CopilotGateway {

    List<String> listModels();

    ChatResponse answer(
            String prompt,
            List<com.example.projectnavigator.dto.SourceRef> sources,
            String selectedModel,
            String workingDirectory);

    void streamAnswer(
            String prompt,
            List<com.example.projectnavigator.dto.SourceRef> sources,
            String selectedModel,
            String workingDirectory,
            Consumer<String> chunkConsumer,
            Consumer<ChatResponse> completionConsumer);
}
