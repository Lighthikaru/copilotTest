package com.example.projectnavigator.dto;

import java.time.Instant;
import java.util.List;

public record ConversationDetail(
        String id,
        String projectId,
        String title,
        Instant createdAt,
        Instant updatedAt,
        String lastModel,
        String lastMode,
        String sessionId,
        boolean archived,
        boolean summarized,
        String summary,
        List<ConversationMessage> messages) {
}
