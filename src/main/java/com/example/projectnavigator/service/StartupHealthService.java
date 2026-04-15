package com.example.projectnavigator.service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StartupHealthService {

    private final Map<String, String> componentStatus = new LinkedHashMap<>();

    @PostConstruct
    void initialize() {
        componentStatus.put("copilot", detectVersion("copilot", "--version"));
    }

    public boolean isCopilotCliReady() {
        String status = componentStatus.get("copilot");
        return status != null && !status.startsWith("missing:");
    }

    public Map<String, String> snapshot() {
        return Map.copyOf(componentStatus);
    }

    private String detectVersion(String command, String arg) {
        try {
            Process process = new ProcessBuilder(command, arg).start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                int exit = process.waitFor();
                if (exit == 0 && line != null && !line.isBlank()) {
                    return line.trim();
                }
                return "missing: exit code " + exit;
            }
        } catch (Exception ex) {
            return "missing: " + ex.getMessage();
        }
    }
}
