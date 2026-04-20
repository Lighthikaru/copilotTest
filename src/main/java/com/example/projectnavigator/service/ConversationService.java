package com.example.projectnavigator.service;

import com.example.projectnavigator.dto.ConversationDetail;
import com.example.projectnavigator.dto.ConversationItem;
import com.example.projectnavigator.dto.ConversationMessage;
import com.example.projectnavigator.dto.SourceRef;
import com.example.projectnavigator.util.JsonCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.IOException;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final SettingsService settingsService;
    private final JsonCodec jsonCodec;

    public ConversationService(SettingsService settingsService, JsonCodec jsonCodec) {
        this.settingsService = settingsService;
        this.jsonCodec = jsonCodec;
    }

    public List<ConversationItem> list(String projectId) {
        Path dir = projectDirectory(projectId);
        if (Files.notExists(dir)) {
            return List.of();
        }
        try {
            List<ConversationItem> items = new ArrayList<>();
            try (var paths = Files.list(dir)) {
                paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .forEach(path -> items.add(toItem(readDocument(path))));
            }
            return items.stream()
                    .sorted(Comparator.comparing(ConversationItem::updatedAt).reversed())
                    .toList();
        } catch (Exception ex) {
            throw new IllegalStateException("無法讀取對話清單：" + ex.getMessage(), ex);
        }
    }

    public ConversationDetail get(String projectId, String conversationId) {
        return toDetail(readDocument(conversationPath(projectId, conversationId)));
    }

    public ConversationDetail create(String projectId, String title) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();
        ConversationDocument document = new ConversationDocument(
                id,
                projectId,
                title == null || title.isBlank() ? "新對話" : title.trim(),
                now,
                now,
                null,
                "structure",
                sessionId,
                false,
                false,
                null,
                new ArrayList<>());
        writeDocument(document);
        return toDetail(document);
    }

    public ConversationDetail update(String projectId, String conversationId, String title, Boolean archived) {
        ConversationDocument document = readDocument(conversationPath(projectId, conversationId));
        ConversationDocument updated = new ConversationDocument(
                document.id(),
                document.projectId(),
                title == null || title.isBlank() ? document.title() : title.trim(),
                document.createdAt(),
                Instant.now(),
                document.lastModel(),
                document.lastMode(),
                document.sessionId(),
                archived == null ? document.archived() : archived,
                document.summarized(),
                document.summary(),
                new ArrayList<>(document.messages()));
        writeDocument(updated);
        return toDetail(updated);
    }

    public ConversationDetail clearMessages(String projectId, String conversationId) {
        ConversationDocument document = readDocument(conversationPath(projectId, conversationId));
        ConversationDocument cleared = new ConversationDocument(
                document.id(),
                document.projectId(),
                document.title(),
                document.createdAt(),
                Instant.now(),
                document.lastModel(),
                document.lastMode(),
                document.sessionId(),
                document.archived(),
                false,
                null,
                new ArrayList<>());
        writeDocument(cleared);
        return toDetail(cleared);
    }

    public ConversationDetail restartSession(String projectId, String conversationId) {
        ConversationDocument document = readDocument(conversationPath(projectId, conversationId));
        ConversationDocument restarted = new ConversationDocument(
                document.id(),
                document.projectId(),
                document.title(),
                document.createdAt(),
                Instant.now(),
                document.lastModel(),
                document.lastMode(),
                UUID.randomUUID().toString(),
                document.archived(),
                document.summarized(),
                document.summary(),
                new ArrayList<>(document.messages()));
        writeDocument(restarted);
        return toDetail(restarted);
    }

    public ConversationDetail compress(String projectId, String conversationId) {
        ConversationDocument document = readDocument(conversationPath(projectId, conversationId));
        if (document.messages().size() <= 4) {
            return toDetail(document);
        }
        int keepFrom = Math.max(document.messages().size() - 4, 0);
        List<ConversationMessage> recent = new ArrayList<>(document.messages().subList(keepFrom, document.messages().size()));
        String summary = summarize(document.messages().subList(0, keepFrom));
        ConversationDocument compressed = new ConversationDocument(
                document.id(),
                document.projectId(),
                document.title(),
                document.createdAt(),
                Instant.now(),
                document.lastModel(),
                document.lastMode(),
                document.sessionId(),
                document.archived(),
                true,
                summary,
                recent);
        writeDocument(compressed);
        return toDetail(compressed);
    }

    public ConversationDetail appendUserMessage(
            String projectId,
            String conversationId,
            String question,
            String model,
            String mode) {
        ConversationDocument document = readDocument(conversationPath(projectId, conversationId));
        List<ConversationMessage> messages = new ArrayList<>(document.messages());
        messages.add(new ConversationMessage(
                UUID.randomUUID().toString(),
                "user",
                question,
                Instant.now(),
                List.of(),
                model,
                document.sessionId(),
                Map.of()));
        String title = "新對話".equals(document.title()) ? deriveTitle(question) : document.title();
        ConversationDocument updated = new ConversationDocument(
                document.id(),
                document.projectId(),
                title,
                document.createdAt(),
                Instant.now(),
                model,
                mode,
                document.sessionId(),
                document.archived(),
                document.summarized(),
                document.summary(),
                messages);
        writeDocument(updated);
        return toDetail(updated);
    }

    public ConversationDetail appendAssistantMessage(
            String projectId,
            String conversationId,
            String answer,
            List<SourceRef> sources,
            String model,
            Map<String, Object> usage) {
        ConversationDocument document = readDocument(conversationPath(projectId, conversationId));
        List<ConversationMessage> messages = new ArrayList<>(document.messages());
        messages.add(new ConversationMessage(
                UUID.randomUUID().toString(),
                "assistant",
                answer,
                Instant.now(),
                sources == null ? List.of() : sources,
                model,
                document.sessionId(),
                usage == null ? Map.of() : usage));
        ConversationDocument updated = new ConversationDocument(
                document.id(),
                document.projectId(),
                document.title(),
                document.createdAt(),
                Instant.now(),
                model,
                document.lastMode(),
                document.sessionId(),
                document.archived(),
                document.summarized(),
                document.summary(),
                messages);
        writeDocument(updated);
        return toDetail(updated);
    }

    public String contextSummary(String projectId, String conversationId) {
        ConversationDocument document = readDocument(conversationPath(projectId, conversationId));
        StringBuilder builder = new StringBuilder();
        if (document.summary() != null && !document.summary().isBlank()) {
            builder.append("歷史摘要：").append(document.summary()).append("\n");
        }
        List<ConversationMessage> recent = document.messages().stream()
                .skip(Math.max(0, document.messages().size() - 6))
                .toList();
        if (!recent.isEmpty()) {
            builder.append("最近對話：\n");
            for (ConversationMessage message : recent) {
                builder.append("[").append("assistant".equals(message.role()) ? "助手" : "使用者").append("] ")
                        .append(message.text()).append("\n");
            }
        }
        return builder.toString().trim();
    }

    public ConversationDetail ensureConversation(String projectId, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return create(projectId, null);
        }
        return get(projectId, conversationId);
    }

    public void deleteConversation(String projectId, String conversationId) {
        Path path = conversationPath(projectId, conversationId);
        try {
            Files.deleteIfExists(path);
        } catch (Exception ex) {
            throw new IllegalStateException("無法刪除對話檔：" + ex.getMessage(), ex);
        }
    }

    public void deleteProjectConversations(String projectId) {
        Path dir = projectDirectory(projectId);
        if (Files.notExists(dir)) {
            return;
        }
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public java.nio.file.FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return java.nio.file.FileVisitResult.CONTINUE;
                }

                @Override
                public java.nio.file.FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
                    Files.deleteIfExists(directory);
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception ex) {
            throw new IllegalStateException("無法刪除專案對話資料：" + ex.getMessage(), ex);
        }
    }

    private String summarize(List<ConversationMessage> messages) {
        StringBuilder builder = new StringBuilder();
        for (ConversationMessage message : messages) {
            if (builder.length() > 1600) {
                break;
            }
            builder.append("assistant".equals(message.role()) ? "助手：" : "使用者：")
                    .append(message.text())
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private String deriveTitle(String question) {
        String normalized = question.trim();
        if (normalized.length() <= 20) {
            return normalized;
        }
        return normalized.substring(0, 20) + "...";
    }

    private ConversationItem toItem(ConversationDocument document) {
        return new ConversationItem(
                document.id(),
                document.projectId(),
                document.title(),
                document.createdAt(),
                document.updatedAt(),
                document.lastModel(),
                document.lastMode(),
                document.sessionId(),
                document.archived(),
                document.summarized(),
                document.messages().size());
    }

    private ConversationDetail toDetail(ConversationDocument document) {
        return new ConversationDetail(
                document.id(),
                document.projectId(),
                document.title(),
                document.createdAt(),
                document.updatedAt(),
                document.lastModel(),
                document.lastMode(),
                document.sessionId(),
                document.archived(),
                document.summarized(),
                document.summary(),
                document.messages());
    }

    private ConversationDocument readDocument(Path path) {
        try {
            Map<String, Object> raw = jsonCodec.read(Files.readString(path), MAP_TYPE);
            String id = stringValue(raw.get("id"));
            String projectId = stringValue(raw.get("projectId"));
            String title = stringValue(raw.get("title"));
            Instant createdAt = Instant.parse(stringValue(raw.get("createdAt")));
            Instant updatedAt = Instant.parse(stringValue(raw.get("updatedAt")));
            String lastModel = stringValue(raw.get("lastModel"));
            String lastMode = stringValue(raw.get("lastMode"));
            String sessionId = stringValue(raw.get("sessionId"));
            boolean archived = booleanValue(raw.get("archived"));
            boolean summarized = booleanValue(raw.get("summarized"));
            String summary = stringValue(raw.get("summary"));
            List<ConversationMessage> messages = raw.containsKey("messages")
                    ? jsonCodec.read(jsonCodec.write(raw.get("messages")), new TypeReference<List<ConversationMessage>>() {
                    })
                    : List.of();
            return new ConversationDocument(
                    id,
                    projectId,
                    title,
                    createdAt,
                    updatedAt,
                    lastModel,
                    lastMode,
                    sessionId,
                    archived,
                    summarized,
                    summary,
                    new ArrayList<>(messages));
        } catch (Exception ex) {
            throw new IllegalStateException("無法讀取對話檔：" + path + "，原因：" + ex.getMessage(), ex);
        }
    }

    private void writeDocument(ConversationDocument document) {
        try {
            Files.createDirectories(projectDirectory(document.projectId()));
            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("id", document.id());
            raw.put("projectId", document.projectId());
            raw.put("title", document.title());
            raw.put("createdAt", document.createdAt().toString());
            raw.put("updatedAt", document.updatedAt().toString());
            raw.put("lastModel", document.lastModel());
            raw.put("lastMode", document.lastMode());
            raw.put("sessionId", document.sessionId());
            raw.put("archived", document.archived());
            raw.put("summarized", document.summarized());
            raw.put("summary", document.summary());
            raw.put("messages", document.messages());
            Files.writeString(conversationPath(document.projectId(), document.id()), jsonCodec.write(raw));
        } catch (Exception ex) {
            throw new IllegalStateException("無法儲存對話檔：" + ex.getMessage(), ex);
        }
    }

    private Path projectDirectory(String projectId) {
        return settingsService.conversationsRoot().resolve(projectId);
    }

    private Path conversationPath(String projectId, String conversationId) {
        return projectDirectory(projectId).resolve(conversationId + ".json");
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        return Boolean.parseBoolean(stringValue(value));
    }

    private record ConversationDocument(
            String id,
            String projectId,
            String title,
            Instant createdAt,
            Instant updatedAt,
            String lastModel,
            String lastMode,
            String sessionId,
            boolean archived,
            boolean summarized,
            String summary,
            List<ConversationMessage> messages) {
    }
}
