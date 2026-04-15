package com.example.projectnavigator.web;

import com.example.projectnavigator.dto.AuthState;
import com.example.projectnavigator.service.AuthFacadeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthFacadeService authFacadeService;

    public AuthController(AuthFacadeService authFacadeService) {
        this.authFacadeService = authFacadeService;
    }

    @GetMapping("/state")
    public AuthState state() {
        return authFacadeService.current();
    }
}
