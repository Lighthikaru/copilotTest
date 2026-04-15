package com.example.projectnavigator.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String projectId,
        String conversationId,
        @NotBlank String question,
        @NotBlank String mode,
        String selectedModel) {
}
