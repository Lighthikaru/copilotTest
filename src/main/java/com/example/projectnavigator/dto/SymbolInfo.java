package com.example.projectnavigator.dto;

public record SymbolInfo(
        String name,
        String kind,
        String sourcePath,
        String container) {
}
