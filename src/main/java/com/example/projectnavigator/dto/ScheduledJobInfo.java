package com.example.projectnavigator.dto;

public record ScheduledJobInfo(
        String name,
        String cron,
        String sourcePath,
        String description) {
}
