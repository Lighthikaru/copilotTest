package com.example.projectnavigator.dto;

import java.util.List;

public record FileSummary(
        String path,
        String language,
        String summary,
        List<String> tags) {
}
