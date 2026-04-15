package com.example.projectnavigator.service;

import com.example.projectnavigator.dto.JobStatus;
import com.example.projectnavigator.dto.LocalProjectRequest;
import com.example.projectnavigator.dto.ProjectConnection;
import com.example.projectnavigator.dto.ProjectDetails;
import com.example.projectnavigator.repository.ProjectIndexRepository;
import com.example.projectnavigator.repository.ProjectRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectIndexRepository projectIndexRepository;
    private final GitService gitService;
    private final IndexingService indexingService;
    private final JobService jobService;
    private final Executor applicationTaskExecutor;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectIndexRepository projectIndexRepository,
            GitService gitService,
            IndexingService indexingService,
            JobService jobService,
            Executor applicationTaskExecutor) {
        this.projectRepository = projectRepository;
        this.projectIndexRepository = projectIndexRepository;
        this.gitService = gitService;
        this.indexingService = indexingService;
        this.jobService = jobService;
        this.applicationTaskExecutor = applicationTaskExecutor;
    }

    public List<ProjectConnection> listProjects() {
        return projectRepository.findAll();
    }

    public ProjectDetails getProjectDetails(String projectId) {
        ProjectConnection connection = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        return new ProjectDetails(
                connection,
                projectIndexRepository.findByProjectId(projectId).orElse(null),
                jobService.recentForProject(projectId));
    }

    public JobStatus registerLocalProject(LocalProjectRequest request) {
        String projectId = UUID.randomUUID().toString();
        JobStatus job = jobService.start("local-import", projectId, "Queued local project registration");

        applicationTaskExecutor.execute(() -> {
            try {
                Path localPath = Path.of(request.localPath());
                gitService.validateLocalPath(localPath);

                Instant now = Instant.now();
                ProjectConnection connection = new ProjectConnection(
                        projectId,
                        "local",
                        request.displayName(),
                        null,
                        "local",
                        localPath.toString(),
                        localPath.toString(),
                        null,
                        now,
                        now);
                projectRepository.save(connection);

                jobService.update(job.jobId(), 40, "Scanning local repository");
                indexingService.index(connection);
                jobService.complete(job.jobId(), "Local project registered and indexed");
            } catch (Exception ex) {
                jobService.fail(job.jobId(), "Local registration failed: " + ex.getMessage());
            }
        });

        return job;
    }

    public JobStatus reindex(String projectId) {
        ProjectConnection connection = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        JobStatus job = jobService.start("reindex", projectId, "Queued project reindex");

        applicationTaskExecutor.execute(() -> {
            try {
                jobService.update(job.jobId(), 30, "Refreshing project map");
                indexingService.index(connection);
                jobService.complete(job.jobId(), "Project index refreshed");
            } catch (Exception ex) {
                jobService.fail(job.jobId(), "Reindex failed: " + ex.getMessage());
            }
        });

        return job;
    }

}
