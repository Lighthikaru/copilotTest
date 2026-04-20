package com.example.projectnavigator.web;

import com.example.projectnavigator.dto.JobStatus;
import com.example.projectnavigator.dto.LocalProjectRequest;
import com.example.projectnavigator.dto.ProjectConnection;
import com.example.projectnavigator.dto.ProjectDetails;
import com.example.projectnavigator.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectConnection> listProjects() {
        return projectService.listProjects();
    }

    @GetMapping("/{projectId}")
    public ProjectDetails getProject(@PathVariable String projectId) {
        return projectService.getProjectDetails(projectId);
    }

    @PostMapping("/local")
    public JobStatus importLocal(@Valid @RequestBody LocalProjectRequest request) {
        return projectService.registerLocalProject(request);
    }

    @PostMapping("/{projectId}/reindex")
    public JobStatus reindex(@PathVariable String projectId) {
        return projectService.reindex(projectId);
    }

    @DeleteMapping("/{projectId}")
    public void deleteProject(@PathVariable String projectId) {
        projectService.deleteProject(projectId);
    }
}
