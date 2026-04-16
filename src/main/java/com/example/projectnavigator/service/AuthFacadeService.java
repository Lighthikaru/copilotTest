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
        StartupHealthService.CommandStatus javaStatus = startupHealthService.inspectJava();
        StartupHealthService.CommandStatus copilotStatus = startupHealthService.inspectCopilot();
        boolean cliReady = copilotStatus.ready();
        List<String> models = cliReady ? copilotGateway.listModels() : List.of();
        boolean loggedIn = cliReady && !models.isEmpty();
        boolean entitled = loggedIn;

        String status;
        if (!javaStatus.ready()) {
            status = "找不到可用的 Java 21，請先在設定頁指定 java.exe。";
        } else if (!cliReady) {
            status = "找不到 Copilot CLI。請先執行 npm install -g @github/copilot，或在設定頁指定 copilot.cmd。";
        } else if (!loggedIn) {
            status = "請先在這台機器執行 copilot login，完成瀏覽器授權後再按重新整理。";
        } else {
            status = "Copilot 已就緒，可以綁定本地專案並開始問答。";
        }

        return new AuthState(
                cliReady,
                loggedIn,
                entitled,
                models,
                "copilot login",
                status,
                javaStatus.ready(),
                javaStatus.displayValue(),
                javaStatus.source(),
                copilotStatus.displayValue(),
                copilotStatus.source());
    }
}
