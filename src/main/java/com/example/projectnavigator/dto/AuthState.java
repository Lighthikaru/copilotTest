package com.example.projectnavigator.dto;

import java.util.List;

public record AuthState(
        boolean copilotCliReady,
        boolean copilotLoggedIn,
        boolean copilotEntitled,
        List<String> availableModels,
        String loginCommand,
        String statusMessage) {
}
