package com.example.projectnavigator.repository;

import com.example.projectnavigator.dto.ProjectIndex;
import com.example.projectnavigator.util.JsonCodec;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectIndexRepository {

    private final JdbcTemplate jdbcTemplate;
    private final JsonCodec jsonCodec;

    public ProjectIndexRepository(JdbcTemplate jdbcTemplate, JsonCodec jsonCodec) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonCodec = jsonCodec;
    }

    public void save(ProjectIndex index) {
        jdbcTemplate.update(
                """
                INSERT INTO project_index (project_id, stack, payload_json, last_indexed_at, index_version)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(project_id) DO UPDATE SET
                    stack = excluded.stack,
                    payload_json = excluded.payload_json,
                    last_indexed_at = excluded.last_indexed_at,
                    index_version = excluded.index_version
                """,
                index.projectId(),
                index.stack(),
                jsonCodec.write(index),
                index.lastIndexedAt().toString(),
                index.indexVersion());
    }

    public Optional<ProjectIndex> findByProjectId(String projectId) {
        return jdbcTemplate.query(
                        "SELECT payload_json FROM project_index WHERE project_id = ?",
                        (rs, rowNum) -> jsonCodec.read(rs.getString("payload_json"), ProjectIndex.class),
                        projectId)
                .stream()
                .findFirst();
    }
}
