package com.example.projectnavigator.web;

import com.example.projectnavigator.service.StartupHealthService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final StartupHealthService startupHealthService;

    public HealthController(StartupHealthService startupHealthService) {
        this.startupHealthService = startupHealthService;
    }

    @GetMapping
    public Map<String, String> health() {
        return startupHealthService.snapshot();
    }
}
