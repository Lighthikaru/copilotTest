package com.example.projectnavigator.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "navigator")
public class AppProperties {

    private Path dataRoot;
    private Path workspaceRoot;
    private long maxFileSizeBytes = 200_000L;
    private int maxContextCharacters = 12_000;
    private int maxIndexedFiles = 400;
    private List<String> excludedPatterns = new ArrayList<>();
    private List<String> sensitiveFilePatterns = new ArrayList<>();
    private Chat chat = new Chat();

    public Path dataRoot() {
        return dataRoot;
    }

    public void setDataRoot(Path dataRoot) {
        this.dataRoot = dataRoot;
    }

    public Path workspaceRoot() {
        return workspaceRoot;
    }

    public void setWorkspaceRoot(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public int getMaxContextCharacters() {
        return maxContextCharacters;
    }

    public void setMaxContextCharacters(int maxContextCharacters) {
        this.maxContextCharacters = maxContextCharacters;
    }

    public int getMaxIndexedFiles() {
        return maxIndexedFiles;
    }

    public void setMaxIndexedFiles(int maxIndexedFiles) {
        this.maxIndexedFiles = maxIndexedFiles;
    }

    public List<String> getExcludedPatterns() {
        return excludedPatterns;
    }

    public void setExcludedPatterns(List<String> excludedPatterns) {
        this.excludedPatterns = excludedPatterns;
    }

    public List<String> getSensitiveFilePatterns() {
        return sensitiveFilePatterns;
    }

    public void setSensitiveFilePatterns(List<String> sensitiveFilePatterns) {
        this.sensitiveFilePatterns = sensitiveFilePatterns;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public static class Chat {
        private boolean allowFallback = false;

        public boolean isAllowFallback() {
            return allowFallback;
        }

        public void setAllowFallback(boolean allowFallback) {
            this.allowFallback = allowFallback;
        }
    }
}
