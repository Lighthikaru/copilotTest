package com.example.projectnavigator.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.projectnavigator.config.AppProperties;
import com.example.projectnavigator.dto.ConfigSignal;
import com.example.projectnavigator.dto.EntryPointInfo;
import com.example.projectnavigator.dto.FileSummary;
import com.example.projectnavigator.dto.ModuleCard;
import com.example.projectnavigator.dto.ProjectConnection;
import com.example.projectnavigator.dto.ProjectIndex;
import com.example.projectnavigator.dto.RouteInfo;
import com.example.projectnavigator.dto.ScheduledJobInfo;
import com.example.projectnavigator.dto.SymbolInfo;
import com.example.projectnavigator.util.PathRules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChatContextAssemblerTest {

    @TempDir
    Path tempDir;

    @Test
    void assemblesPromptAndSourcesWithinBudget() throws Exception {
        Files.createDirectories(tempDir.resolve("src/main/java/com/example/demo"));
        Files.writeString(tempDir.resolve("src/main/java/com/example/demo/AuthController.java"), """
                package com.example.demo;

                @RestController
                public class AuthController {
                    @PostMapping("/login")
                    public String login() {
                        return "ok";
                    }
                }
                """);

        AppProperties properties = new AppProperties();
        properties.setMaxContextCharacters(4000);
        properties.setExcludedPatterns(List.of(".git", "node_modules"));
        properties.setSensitiveFilePatterns(List.of(".env"));

        ChatContextAssembler assembler = new ChatContextAssembler(properties, new PathRules(properties));
        ProjectConnection connection = new ProjectConnection(
                "demo",
                "local",
                "demo",
                null,
                "local",
                tempDir.toString(),
                tempDir.toString(),
                null,
                Instant.now(),
                Instant.now());
        ProjectIndex index = new ProjectIndex(
                "demo",
                "Java / Spring Boot",
                List.of(new ModuleCard("com.example.demo", "controller", "Authentication endpoints", List.of("src/main/java/com/example/demo/AuthController.java"), List.of("Java", "Spring"), 0.9)),
                List.of(new EntryPointInfo("DemoApplication", "src/main/java/com/example/demo/DemoApplication.java", "application", "Bootstrap")),
                List.of(new RouteInfo("POST", "/login", "AuthController", "src/main/java/com/example/demo/AuthController.java", "Login endpoint")),
                List.<ScheduledJobInfo>of(),
                List.of(new ConfigSignal("application-config", "src/main/resources/application.yml", "demo")),
                List.of(new SymbolInfo("AuthController", "class", "src/main/java/com/example/demo/AuthController.java", "com.example.demo")),
                List.of(new FileSummary("src/main/java/com/example/demo/AuthController.java", "java", "Authentication controller", List.of("java", "controller"))),
                Instant.now(),
                1,
                List.of(),
                List.of());

        ChatContextAssembler.ContextPack context = assembler.build(connection, index, "登入流程在哪裡", "structure", null);

        assertThat(context.prompt()).contains("登入流程在哪裡");
        assertThat(context.sources()).isNotEmpty();
        assertThat(context.prompt().length()).isLessThanOrEqualTo(4000);
    }
}
