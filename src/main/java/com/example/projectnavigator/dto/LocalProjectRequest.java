package com.example.projectnavigator.dto;

import jakarta.validation.constraints.NotBlank;

public record LocalProjectRequest(
        @NotBlank String displayName,
        @NotBlank String localPath) {
}
