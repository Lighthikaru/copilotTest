package com.example.projectnavigator.dto;

import java.time.Instant;

public record ConversationItem(
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
        int messageCount) {
}
