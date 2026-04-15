package com.example.projectnavigator.service;

import com.example.projectnavigator.config.AppProperties;
import com.example.projectnavigator.dto.FileSummary;
import com.example.projectnavigator.dto.ModuleCard;
import com.example.projectnavigator.dto.ProjectConnection;
import com.example.projectnavigator.dto.ProjectIndex;
import com.example.projectnavigator.dto.SourceRef;
import com.example.projectnavigator.util.PathRules;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChatContextAssembler {

    public record ContextPack(String prompt, List<SourceRef> sources, List<String> warnings) {
    }

    private final AppProperties properties;
    private final PathRules pathRules;

    public ChatContextAssembler(AppProperties properties, PathRules pathRules) {
        this.properties = properties;
        this.pathRules = pathRules;
    }

    public ContextPack build(ProjectConnection connection, ProjectIndex index, String question, String mode, String conversationSummary) {
        List<String> warnings = new ArrayList<>();
        List<String> terms = tokenize(question);
        List<ModuleCard> modules = rankModules(index.moduleCards(), terms);
        List<FileSummary> files = rankFiles(index.fileSummaries(), terms);
        List<SourceRef> sources = new ArrayList<>();
        int budget = properties.getMaxContextCharacters();

        StringBuilder prompt = new StringBuilder();
        appendWithinBudget(prompt, budget,
                "You are helping a business analyst understand a software project.\n"
                        + "Answer in Traditional Chinese.\n"
                        + "Mode: " + mode + "\n"
                        + "Project stack: " + index.stack() + "\n"
                        + "Question: " + question + "\n\n");

        if (conversationSummary != null && !conversationSummary.isBlank()) {
            appendWithinBudget(prompt, budget, "Recent conversation summary:\n" + conversationSummary + "\n\n");
        }

        appendWithinBudget(prompt, budget, "Top modules:\n");
        for (ModuleCard module : modules) {
            appendWithinBudget(prompt, budget,
                    "- " + module.name() + " [" + module.kind() + "] " + module.description()
                            + " paths=" + module.primaryPaths() + "\n");
        }

        appendWithinBudget(prompt, budget, "\nRelevant sources:\n");
        Path projectRoot = Path.of(connection.workspacePath() != null ? connection.workspacePath() : connection.localPath());
        for (FileSummary file : files) {
            if (sources.size() >= 6) {
                break;
            }
            try {
                Path relative = Path.of(file.path());
                if (pathRules.isSensitive(relative)) {
                    warnings.add("Skipped sensitive file: " + file.path());
                    continue;
                }
                String excerpt = excerpt(projectRoot.resolve(file.path()));
                if (excerpt.isBlank()) {
                    continue;
                }
                SourceRef source = new SourceRef(file.path(), "file", file.summary(), excerpt);
                appendWithinBudget(prompt, budget,
                        "### " + file.path() + "\n" + excerpt + "\n\n");
                sources.add(source);
            } catch (Exception ex) {
                warnings.add("Failed to read " + file.path() + ": " + ex.getMessage());
            }
        }

        if (sources.isEmpty()) {
            warnings.add("No file excerpts were attached; answer may be low confidence.");
        }

        return new ContextPack(prompt.toString(), sources, warnings);
    }

    private List<ModuleCard> rankModules(List<ModuleCard> modules, List<String> terms) {
        return modules.stream()
                .sorted(Comparator.comparingInt((ModuleCard module) -> score(module, terms)).reversed())
                .limit(4)
                .toList();
    }

    private List<FileSummary> rankFiles(List<FileSummary> files, List<String> terms) {
        return files.stream()
                .sorted(Comparator.comparingInt((FileSummary file) -> score(file, terms)).reversed())
                .limit(10)
                .toList();
    }

    private int score(ModuleCard module, List<String> terms) {
        String haystack = (module.name() + " " + module.description() + " " + String.join(" ", module.primaryPaths()))
                .toLowerCase(Locale.ROOT);
        return matchCount(haystack, terms);
    }

    private int score(FileSummary file, List<String> terms) {
        String haystack = (file.path() + " " + file.summary() + " " + String.join(" ", file.tags()))
                .toLowerCase(Locale.ROOT);
        return matchCount(haystack, terms);
    }

    private int matchCount(String haystack, List<String> terms) {
        int score = 0;
        for (String term : terms) {
            if (haystack.contains(term)) {
                score += 3;
            }
        }
        if (score == 0 && haystack.contains("controller")) {
            score = 1;
        }
        return score;
    }

    private List<String> tokenize(String question) {
        return Set.copyOf(List.of(question.toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}_]+")))
                .stream()
                .filter(term -> !term.isBlank() && term.length() > 1)
                .limit(12)
                .toList();
    }

    private String excerpt(Path path) throws Exception {
        if (Files.notExists(path)) {
            return "";
        }
        String content = Files.readString(path, StandardCharsets.UTF_8);
        List<String> lines = content.lines().limit(80).toList();
        return String.join("\n", lines);
    }

    private void appendWithinBudget(StringBuilder builder, int budget, String value) {
        if (builder.length() >= budget) {
            return;
        }
        int remaining = budget - builder.length();
        if (value.length() <= remaining) {
            builder.append(value);
            return;
        }
        builder.append(value, 0, Math.max(0, remaining));
    }
}
