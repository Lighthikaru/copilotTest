package com.example.projectnavigator.dto;

import java.time.Instant;
import java.util.List;

public record ProjectIndex(
        String projectId,
        String stack,
        List<ModuleCard> moduleCards,
        List<EntryPointInfo> entryPoints,
        List<RouteInfo> routes,
        List<ScheduledJobInfo> jobs,
        List<ConfigSignal> configSignals,
        List<SymbolInfo> symbolTable,
        List<FileSummary> fileSummaries,
        Instant lastIndexedAt,
        int indexVersion,
        List<String> excludedPaths,
        List<String> analysisWarnings) {
}
