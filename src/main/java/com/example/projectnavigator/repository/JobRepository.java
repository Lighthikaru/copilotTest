package com.example.projectnavigator.repository;

import com.example.projectnavigator.dto.JobStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JobRepository {

    private final JdbcTemplate jdbcTemplate;

    public JobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(String projectId, JobStatus status) {
        jdbcTemplate.update(
                """
                INSERT INTO job_status (job_id, project_id, type, state, progress, started_at, ended_at, message)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(job_id) DO UPDATE SET
                    project_id = excluded.project_id,
                    type = excluded.type,
                    state = excluded.state,
                    progress = excluded.progress,
                    started_at = excluded.started_at,
                    ended_at = excluded.ended_at,
                    message = excluded.message
                """,
                status.jobId(),
                projectId,
                status.type(),
                status.state(),
                status.progress(),
                status.startedAt().toString(),
                status.endedAt() == null ? null : status.endedAt().toString(),
                status.message());
    }

    public Optional<JobStatus> findById(String jobId) {
        return jdbcTemplate.query(
                        "SELECT * FROM job_status WHERE job_id = ?",
                        (rs, rowNum) -> new JobStatus(
                                rs.getString("job_id"),
                                rs.getString("type"),
                                rs.getString("state"),
                                rs.getInt("progress"),
                                Instant.parse(rs.getString("started_at")),
                                rs.getString("ended_at") == null ? null : Instant.parse(rs.getString("ended_at")),
                                rs.getString("message")),
                        jobId)
                .stream()
                .findFirst();
    }

    public List<JobStatus> findRecentByProjectId(String projectId) {
        return jdbcTemplate.query(
                "SELECT * FROM job_status WHERE project_id = ? ORDER BY started_at DESC LIMIT 10",
                (rs, rowNum) -> new JobStatus(
                        rs.getString("job_id"),
                        rs.getString("type"),
                        rs.getString("state"),
                        rs.getInt("progress"),
                        Instant.parse(rs.getString("started_at")),
                        rs.getString("ended_at") == null ? null : Instant.parse(rs.getString("ended_at")),
                        rs.getString("message")),
                projectId);
    }

    public void deleteByProjectId(String projectId) {
        jdbcTemplate.update("DELETE FROM job_status WHERE project_id = ?", projectId);
    }
}
