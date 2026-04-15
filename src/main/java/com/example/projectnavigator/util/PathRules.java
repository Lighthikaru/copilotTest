package com.example.projectnavigator.util;

import com.example.projectnavigator.config.AppProperties;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PathRules {

    private final AppProperties properties;

    public PathRules(AppProperties properties) {
        this.properties = properties;
    }

    public boolean isExcluded(Path relativePath) {
        String normalized = relativePath.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        String[] segments = Arrays.stream(normalized.split("/"))
                .filter(segment -> !segment.isBlank())
                .toArray(String[]::new);
        return properties.getExcludedPatterns().stream()
                .map(pattern -> pattern.toLowerCase(Locale.ROOT))
                .anyMatch(pattern -> matchesSegment(pattern, segments));
    }

    public boolean isSensitive(Path relativePath) {
        String normalized = relativePath.getFileName() == null
                ? relativePath.toString().toLowerCase(Locale.ROOT)
                : relativePath.getFileName().toString().toLowerCase(Locale.ROOT);
        return properties.getSensitiveFilePatterns().stream()
                .map(pattern -> pattern.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::contains);
    }

    private boolean matchesSegment(String pattern, String[] segments) {
        for (String segment : segments) {
            if (segment.equals(pattern)) {
                return true;
            }
        }
        return false;
    }
}
