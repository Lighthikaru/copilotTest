package com.example.projectnavigator.repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationRepository {

    private final JdbcTemplate jdbcTemplate;

    public ConversationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveSummary(String conversationId, String projectId, String summaryJson) {
        jdbcTemplate.update(
                """
                INSERT INTO conversation_summary (conversation_id, project_id, summary_json, updated_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(conversation_id) DO UPDATE SET
                    project_id = excluded.project_id,
                    summary_json = excluded.summary_json,
                    updated_at = excluded.updated_at
                """,
                conversationId,
                projectId,
                summaryJson,
                Instant.now().toString());
    }

    public Optional<String> findSummary(String conversationId) {
        return jdbcTemplate.query(
                        "SELECT summary_json FROM conversation_summary WHERE conversation_id = ?",
                        (rs, rowNum) -> rs.getString("summary_json"),
                        conversationId)
                .stream()
                .findFirst();
    }
}
