package com.example.projectnavigator.service;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

@Service
public class GitService {

    public void validateLocalPath(Path path) {
        if (path == null || Files.notExists(path) || !Files.isDirectory(path)) {
            throw new IllegalArgumentException("Local path does not exist: " + path);
        }
    }
}
