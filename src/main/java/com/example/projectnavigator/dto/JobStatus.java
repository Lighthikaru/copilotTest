package com.example.projectnavigator.dto;

import java.time.Instant;

public record JobStatus(
        String jobId,
        String type,
        String state,
        int progress,
        Instant startedAt,
        Instant endedAt,
        String message) {
}
