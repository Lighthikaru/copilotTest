package com.example.projectnavigator.dto;

public record RouteInfo(
        String method,
        String path,
        String controller,
        String sourcePath,
        String description) {
}
