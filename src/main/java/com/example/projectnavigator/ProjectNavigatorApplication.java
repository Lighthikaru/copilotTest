package com.example.projectnavigator;

import com.example.projectnavigator.config.AppProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ProjectNavigatorApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        ensureDataDirectories();
        SpringApplication.run(ProjectNavigatorApplication.class, args);
    }

    @Bean
    Path appDataRoot(AppProperties properties) throws Exception {
        Path dataRoot = properties.dataRoot();
        Files.createDirectories(dataRoot);
        Files.createDirectories(properties.workspaceRoot());
        return dataRoot;
    }

    private static void ensureDataDirectories() {
        try {
            String explicitRoot = System.getenv("NAVIGATOR_DATA_ROOT");
            String dataRootValue = (explicitRoot == null || explicitRoot.isBlank())
                    ? System.getProperty("user.home") + "/.project-navigator"
                    : explicitRoot;
            Path dataRoot = Path.of(dataRootValue);
            Files.createDirectories(dataRoot);
            Files.createDirectories(dataRoot.resolve("workspaces"));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize navigator data directories", ex);
        }
    }
}
