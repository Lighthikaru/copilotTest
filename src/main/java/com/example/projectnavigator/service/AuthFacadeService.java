package com.example.projectnavigator.service;

import com.example.projectnavigator.dto.AuthState;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuthFacadeService {

    private final StartupHealthService startupHealthService;
    private final CopilotGateway copilotGateway;

    public AuthFacadeService(
            StartupHealthService startupHealthService,
            CopilotGateway copilotGateway) {
        this.startupHealthService = startupHealthService;
        this.copilotGateway = copilotGateway;
    }

    public AuthState current() {
        boolean cliReady = startupHealthService.isCopilotCliReady();
        List<String> models = cliReady ? copilotGateway.listModels() : List.of();
        boolean loggedIn = cliReady && !models.isEmpty();
        boolean entitled = loggedIn;

        String status;
        if (!cliReady) {
            status = "Copilot CLI not found. Install the official `copilot` CLI first.";
        } else if (!loggedIn) {
            status = "Run `copilot login` in a terminal on this machine, complete the browser authorization, then click Refresh.";
        } else {
            status = "Copilot CLI is ready. Bind a local project folder and start asking questions.";
        }

        return new AuthState(
                cliReady,
                loggedIn,
                entitled,
                models,
                "copilot login",
                status);
    }
}
