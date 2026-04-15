package com.example.projectnavigator.dto;

public record SourceRef(
        String path,
        String kind,
        String reason,
        String excerpt) {
}
