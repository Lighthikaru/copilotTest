package com.example.projectnavigator.service;

import com.example.projectnavigator.dto.ProjectConnection;
import com.example.projectnavigator.dto.ProjectIndex;
import com.example.projectnavigator.repository.ProjectIndexRepository;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

@Service
public class IndexingService {

    private final ProjectScanner projectScanner;
    private final ProjectIndexRepository projectIndexRepository;

    public IndexingService(ProjectScanner projectScanner, ProjectIndexRepository projectIndexRepository) {
        this.projectScanner = projectScanner;
        this.projectIndexRepository = projectIndexRepository;
    }

    public ProjectIndex index(ProjectConnection connection) throws Exception {
        Path root = Path.of(connection.workspacePath() != null ? connection.workspacePath() : connection.localPath());
        ProjectIndex index = projectScanner.scan(connection.id(), root);
        projectIndexRepository.save(index);
        return index;
    }
}
