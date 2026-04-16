package com.example.projectnavigator.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ConversationMessage(
        String id,
        String role,
        String text,
        Instant timestamp,
        List<SourceRef> sources,
        String model,
        String sessionId,
        Map<String, Object> usage) {
}
