package com.example.projectnavigator.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.projectnavigator.config.AppProperties;
import com.example.projectnavigator.dto.ProjectIndex;
import com.example.projectnavigator.util.PathRules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectScannerTest {

    @TempDir
    Path tempDir;

    @Test
    void detectsSpringBootSignals() throws Exception {
        Path root = tempDir.resolve("demo");
        Path javaPath = root.resolve("src/main/java/com/example/demo");
        Path resourcesPath = root.resolve("src/main/resources");
        Path nodeModules = root.resolve("frontend/node_modules/react");
        Files.createDirectories(javaPath);
        Files.createDirectories(resourcesPath);
        Files.createDirectories(nodeModules);
        Files.writeString(root.resolve("pom.xml"), """
                <project>
                  <artifactId>demo</artifactId>
                  <dependencies>
                    <dependency>
                      <artifactId>spring-boot-starter-web</artifactId>
                    </dependency>
                  </dependencies>
                </project>
                """);
        Files.writeString(resourcesPath.resolve("application.yml"), "spring:\n  application:\n    name: demo\n");
        Files.writeString(javaPath.resolve("DemoApplication.java"), """
                package com.example.demo;

                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;

                @SpringBootApplication
                public class DemoApplication {
                    public static void main(String[] args) {
                        SpringApplication.run(DemoApplication.class, args);
                    }
                }
                """);
        Files.writeString(javaPath.resolve("CustomerController.java"), """
                package com.example.demo;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/customers")
                public class CustomerController {
                    @GetMapping("/health")
                    public String health() {
                        return "ok";
                    }
                }
                """);
        Files.writeString(nodeModules.resolve("index.js"), "export const react = true;");

        AppProperties properties = new AppProperties();
        properties.setMaxIndexedFiles(100);
        properties.setMaxFileSizeBytes(200_000L);
        properties.setExcludedPatterns(List.of(".git", "target", "node_modules"));
        properties.setSensitiveFilePatterns(List.of(".env", ".pem"));

        ProjectScanner scanner = new ProjectScanner(properties, new PathRules(properties));
        ProjectIndex index = scanner.scan("demo", root);

        assertThat(index.stack()).isEqualTo("Java / Spring Boot");
        assertThat(index.entryPoints()).extracting("name").contains("DemoApplication");
        assertThat(index.routes()).extracting("path").contains("/customers/health");
        assertThat(index.moduleCards()).isNotEmpty();
        assertThat(index.configSignals()).extracting("path").contains("pom.xml");
        assertThat(index.excludedPaths()).contains("frontend/node_modules/ (excluded directory)");
        assertThat(index.excludedPaths()).noneMatch(path -> path.contains("frontend/node_modules/react/index.js"));
    }
}
