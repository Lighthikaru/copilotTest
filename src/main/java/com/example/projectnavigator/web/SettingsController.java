package com.example.projectnavigator.web;

import com.example.projectnavigator.dto.AppSettings;
import com.example.projectnavigator.service.SettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public AppSettings current() {
        return settingsService.current();
    }

    @PutMapping
    public AppSettings update(@RequestBody AppSettings request) {
        return settingsService.save(request);
    }
}
