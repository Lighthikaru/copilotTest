package com.example.projectnavigator.service;

import com.example.projectnavigator.dto.ChatResponse;
import com.example.projectnavigator.dto.SourceRef;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class StubCopilotGateway implements CopilotGateway {

    @Override
    public List<String> listModels() {
        return List.of(
                "gpt-5.4",
                "gpt-5.4-mini",
                "gpt-5.3-codex",
                "claude-sonnet-4.6",
                "gemini-3-pro-preview");
    }

    @Override
    public ChatResponse answer(
            String prompt,
            List<SourceRef> sources,
            String selectedModel,
            String workingDirectory) {
        StringBuilder answer = new StringBuilder();
        answer.append("目前這題我會先從專案地圖和附帶來源推論。");
        if (!sources.isEmpty()) {
            answer.append("最相關的來源集中在 ");
            answer.append(sources.stream().limit(3).map(SourceRef::path).toList());
            answer.append("。");
        } else {
            answer.append("目前沒有可引用的程式碼片段，所以我只會給出低信心摘要。");
        }
        answer.append(" 若你要做影響分析，建議再追 controller -> service -> repository -> config 的鏈路。");

        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("model", selectedModel == null || selectedModel.isBlank() ? "gpt-5.4" : selectedModel);
        usage.put("contextCharacters", prompt.length());
        usage.put("sourceCount", sources.size());

        return new ChatResponse(
                answer.toString(),
                sources,
                sources.isEmpty() ? 0.35 : 0.72,
                List.of("請再問一次具體功能，例如登入、排程或某支 API。", "若要做 impact 分析，可指定欄位、資料表或 API 名稱。"),
                sources.isEmpty() ? List.of("Copilot gateway is running in scaffold mode.") : List.of(),
                usage);
    }

    @Override
    public void streamAnswer(
            String prompt,
            List<SourceRef> sources,
            String selectedModel,
            String workingDirectory,
            Consumer<String> chunkConsumer,
            Consumer<ChatResponse> completionConsumer) {
        ChatResponse response = answer(prompt, sources, selectedModel, workingDirectory);
        for (String token : response.answer().split("(?<=。)|(?<=，)|(?<= )")) {
            if (!token.isBlank()) {
                chunkConsumer.accept(token);
            }
        }
        completionConsumer.accept(response);
    }
}
