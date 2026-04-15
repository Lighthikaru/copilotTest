package com.example.projectnavigator.dto;

import java.time.Instant;

public record ProjectConnection(
        String id,
        String type,
        String displayName,
        String repoUrl,
        String branch,
        String localPath,
        String workspacePath,
        String gitlabCredentialRef,
        Instant createdAt,
        Instant updatedAt) {
}
