package com.example.projectnavigator.dto;

import java.util.List;

public record ModuleCard(
        String name,
        String kind,
        String description,
        List<String> primaryPaths,
        List<String> technologies,
        double confidence) {
}
