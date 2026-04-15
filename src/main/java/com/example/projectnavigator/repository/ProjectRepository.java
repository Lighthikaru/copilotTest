package com.example.projectnavigator.repository;

import com.example.projectnavigator.dto.ProjectConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectRepository {

    private static final RowMapper<ProjectConnection> ROW_MAPPER = new RowMapper<>() {
        @Override
        public ProjectConnection mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ProjectConnection(
                    rs.getString("id"),
                    rs.getString("type"),
                    rs.getString("display_name"),
                    rs.getString("repo_url"),
                    rs.getString("branch_name"),
                    rs.getString("local_path"),
                    rs.getString("workspace_path"),
                    rs.getString("gitlab_credential_ref"),
                    Instant.parse(rs.getString("created_at")),
                    Instant.parse(rs.getString("updated_at")));
        }
    };

    private final JdbcTemplate jdbcTemplate;

    public ProjectRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(ProjectConnection connection) {
        jdbcTemplate.update(
                """
                INSERT INTO project_connection (
                    id, type, display_name, repo_url, branch_name, local_path, workspace_path,
                    gitlab_credential_ref, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    type = excluded.type,
                    display_name = excluded.display_name,
                    repo_url = excluded.repo_url,
                    branch_name = excluded.branch_name,
                    local_path = excluded.local_path,
                    workspace_path = excluded.workspace_path,
                    gitlab_credential_ref = excluded.gitlab_credential_ref,
                    updated_at = excluded.updated_at
                """,
                connection.id(),
                connection.type(),
                connection.displayName(),
                connection.repoUrl(),
                connection.branch(),
                connection.localPath(),
                connection.workspacePath(),
                connection.gitlabCredentialRef(),
                connection.createdAt().toString(),
                connection.updatedAt().toString());
    }

    public List<ProjectConnection> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM project_connection ORDER BY updated_at DESC",
                ROW_MAPPER);
    }

    public Optional<ProjectConnection> findById(String id) {
        List<ProjectConnection> results = jdbcTemplate.query(
                "SELECT * FROM project_connection WHERE id = ?",
                ROW_MAPPER,
                id);
        return results.stream().findFirst();
    }
}
