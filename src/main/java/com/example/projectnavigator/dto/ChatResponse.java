package com.example.projectnavigator.dto;

import java.util.List;
import java.util.Map;

public record ChatResponse(
        String answer,
        List<SourceRef> sources,
        double confidence,
        List<String> followUps,
        List<String> warnings,
        Map<String, Object> usage) {
}
