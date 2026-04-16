package com.example.projectnavigator.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StartupHealthService {

    private final SettingsService settingsService;

    public StartupHealthService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public boolean isCopilotCliReady() {
        return inspectCopilot().ready();
    }

    public CommandStatus inspectJava() {
        return detectVersion(settingsService.resolveJavaPath(), "-version", settingsService.javaSourceLabel());
    }

    public CommandStatus inspectCopilot() {
        return detectVersion(settingsService.resolveCopilotCliPath(), "--version", settingsService.copilotSourceLabel());
    }

    public Map<String, String> snapshot() {
        CommandStatus java = inspectJava();
        CommandStatus copilot = inspectCopilot();
        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("java", java.displayValue());
        snapshot.put("javaSource", java.source());
        snapshot.put("copilot", copilot.displayValue());
        snapshot.put("copilotSource", copilot.source());
        return Map.copyOf(snapshot);
    }

    private CommandStatus detectVersion(String command, String arg, String sourceLabel) {
        try {
            ProcessBuilder processBuilder = buildProcess(command, arg);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                int exit = process.waitFor();
                if (exit == 0 && line != null && !line.isBlank()) {
                    return new CommandStatus(true, line.trim(), sourceLabel, null);
                }
                return new CommandStatus(false, null, sourceLabel, "exit code " + exit);
            }
        } catch (Exception ex) {
            return new CommandStatus(false, null, sourceLabel, ex.getMessage());
        }
    }

    private ProcessBuilder buildProcess(String command, String arg) {
        if (isWindows() && (command.endsWith(".cmd") || command.endsWith(".bat"))) {
            return new ProcessBuilder("cmd.exe", "/c", command, arg);
        }
        return isWindows()
                ? new ProcessBuilder("cmd.exe", "/c", command, arg)
                : new ProcessBuilder(command, arg);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public record CommandStatus(
            boolean ready,
            String value,
            String source,
            String error) {
        public String displayValue() {
            if (ready && value != null && !value.isBlank()) {
                return value;
            }
            return "missing: " + (error == null || error.isBlank() ? "unknown" : error);
        }
    }
}
