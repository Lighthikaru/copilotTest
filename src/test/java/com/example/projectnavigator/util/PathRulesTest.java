package com.example.projectnavigator.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.projectnavigator.config.AppProperties;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathRulesTest {

    @Test
    void matchesExcludedSegmentsWithoutFalsePositives() {
        AppProperties properties = new AppProperties();
        properties.setExcludedPatterns(List.of("out", "target", "node_modules"));
        properties.setSensitiveFilePatterns(List.of(".env"));

        PathRules rules = new PathRules(properties);

        assertThat(rules.isExcluded(Path.of("frontend/node_modules/react/index.js"))).isTrue();
        assertThat(rules.isExcluded(Path.of("target/app.jar"))).isTrue();
        assertThat(rules.isExcluded(Path.of("src/main/java/com/example/demo/RouteInfo.java"))).isFalse();
        assertThat(rules.isExcluded(Path.of("src/main/java/com/example/demo/AboutController.java"))).isFalse();
    }
}
