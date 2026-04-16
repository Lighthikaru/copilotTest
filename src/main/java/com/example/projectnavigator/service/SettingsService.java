package com.example.projectnavigator.service;

import com.example.projectnavigator.config.AppProperties;
import com.example.projectnavigator.dto.AppSettings;
import com.example.projectnavigator.util.JsonCodec;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private final AppProperties properties;
    private final Path bootstrapDataRoot;
    private final JsonCodec jsonCodec;

    public SettingsService(AppProperties properties, Path bootstrapDataRoot, JsonCodec jsonCodec) {
        this.properties = properties;
        this.bootstrapDataRoot = bootstrapDataRoot;
        this.jsonCodec = jsonCodec;
    }

    public synchronized AppSettings current() {
        Path settingsFile = settingsFile();
        if (Files.notExists(settingsFile)) {
            return new AppSettings(null, null, bootstrapDataRoot.toString());
        }
        try {
            return jsonCodec.read(Files.readString(settingsFile), AppSettings.class);
        } catch (Exception ex) {
            throw new IllegalStateException("無法讀取設定檔：" + ex.getMessage(), ex);
        }
    }

    public synchronized AppSettings save(AppSettings request) {
        AppSettings normalized = new AppSettings(
                blankToNull(request.javaPath()),
                blankToNull(request.copilotCliPath()),
                blankToNull(request.dataRoot()));
        try {
            Files.createDirectories(bootstrapDataRoot);
            Files.writeString(settingsFile(), jsonCodec.write(normalized));
            Files.createDirectories(effectiveDataRoot());
            Files.createDirectories(conversationsRoot());
            return normalized;
        } catch (Exception ex) {
            throw new IllegalStateException("無法儲存設定檔：" + ex.getMessage(), ex);
        }
    }

    public Path effectiveDataRoot() {
        AppSettings settings = current();
        String configured = settings.dataRoot();
        Path root = configured == null || configured.isBlank() ? bootstrapDataRoot : Path.of(configured);
        try {
            Files.createDirectories(root);
        } catch (Exception ex) {
            throw new IllegalStateException("無法建立資料目錄：" + root, ex);
        }
        return root;
    }

    public Path conversationsRoot() {
        Path root = effectiveDataRoot().resolve("conversations");
        try {
            Files.createDirectories(root);
        } catch (Exception ex) {
            throw new IllegalStateException("無法建立對話目錄：" + root, ex);
        }
        return root;
    }

    public String resolveJavaPath() {
        AppSettings settings = current();
        if (settings.javaPath() != null && !settings.javaPath().isBlank()) {
            return settings.javaPath();
        }
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            Path candidate = Path.of(javaHome, "bin", isWindows() ? "java.exe" : "java");
            if (Files.exists(candidate)) {
                return candidate.toString();
            }
        }
        return "java";
    }

    public String resolveCopilotCliPath() {
        AppSettings settings = current();
        if (settings.copilotCliPath() != null && !settings.copilotCliPath().isBlank()) {
            return settings.copilotCliPath();
        }
        return "copilot";
    }

    public String javaSourceLabel() {
        AppSettings settings = current();
        if (settings.javaPath() != null && !settings.javaPath().isBlank()) {
            return "使用設定頁指定路徑";
        }
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            return "使用 JAVA_HOME";
        }
        return "使用系統 PATH";
    }

    public String copilotSourceLabel() {
        AppSettings settings = current();
        if (settings.copilotCliPath() != null && !settings.copilotCliPath().isBlank()) {
            return "使用設定頁指定路徑";
        }
        return "使用系統 PATH";
    }

    private Path settingsFile() {
        return bootstrapDataRoot.resolve("settings.json");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
