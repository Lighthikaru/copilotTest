package com.example.projectnavigator.service;

import com.example.projectnavigator.dto.JobStatus;
import com.example.projectnavigator.repository.JobRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class JobService {

    private record JobEnvelope(String projectId, JobStatus status) {
    }

    private final JobRepository jobRepository;
    private final Map<String, JobEnvelope> inMemoryJobs = new ConcurrentHashMap<>();
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public JobStatus start(String type, String projectId, String message) {
        JobStatus status = new JobStatus(
                UUID.randomUUID().toString(),
                type,
                "RUNNING",
                0,
                Instant.now(),
                null,
                message);
        persist(projectId, status);
        return status;
    }

    public JobStatus update(String jobId, int progress, String message) {
        JobEnvelope envelope = require(jobId);
        JobStatus next = new JobStatus(
                envelope.status().jobId(),
                envelope.status().type(),
                "RUNNING",
                progress,
                envelope.status().startedAt(),
                null,
                message);
        persist(envelope.projectId(), next);
        return next;
    }

    public JobStatus complete(String jobId, String message) {
        JobEnvelope envelope = require(jobId);
        JobStatus next = new JobStatus(
                envelope.status().jobId(),
                envelope.status().type(),
                "COMPLETED",
                100,
                envelope.status().startedAt(),
                Instant.now(),
                message);
        persist(envelope.projectId(), next);
        return next;
    }

    public JobStatus fail(String jobId, String message) {
        JobEnvelope envelope = require(jobId);
        JobStatus next = new JobStatus(
                envelope.status().jobId(),
                envelope.status().type(),
                "FAILED",
                Math.min(100, Math.max(0, envelope.status().progress())),
                envelope.status().startedAt(),
                Instant.now(),
                message);
        persist(envelope.projectId(), next);
        return next;
    }

    public Optional<JobStatus> find(String jobId) {
        JobEnvelope inMemory = inMemoryJobs.get(jobId);
        if (inMemory != null) {
            return Optional.of(inMemory.status());
        }
        return jobRepository.findById(jobId);
    }

    public List<JobStatus> recentForProject(String projectId) {
        return jobRepository.findRecentByProjectId(projectId);
    }

    public SseEmitter subscribe(String jobId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(jobId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> cleanup(jobId, emitter));
        emitter.onTimeout(() -> cleanup(jobId, emitter));
        find(jobId).ifPresent(status -> send(jobId, status, emitter));
        return emitter;
    }

    private void persist(String projectId, JobStatus status) {
        JobEnvelope envelope = new JobEnvelope(projectId, status);
        inMemoryJobs.put(status.jobId(), envelope);
        jobRepository.save(projectId, status);
        emitters.getOrDefault(status.jobId(), List.of())
                .forEach(emitter -> send(status.jobId(), status, emitter));
    }

    private JobEnvelope require(String jobId) {
        JobEnvelope envelope = inMemoryJobs.get(jobId);
        if (envelope != null) {
            return envelope;
        }
        JobStatus status = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        JobEnvelope restored = new JobEnvelope(null, status);
        inMemoryJobs.put(jobId, restored);
        return restored;
    }

    private void cleanup(String jobId, SseEmitter emitter) {
        emitters.getOrDefault(jobId, List.of()).remove(emitter);
    }

    private void send(String jobId, JobStatus status, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("job")
                    .id(jobId)
                    .data(status));
            if (!"RUNNING".equals(status.state())) {
                emitter.complete();
            }
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }
}
