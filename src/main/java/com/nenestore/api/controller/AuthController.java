package com.nenestore.api.controller;

import com.nenestore.api.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody Map<String, String> body) {
        return authService.register(
                body.get("username"),
                body.get("password"),
                body.get("role"));
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> body) {
        return authService.login(
                body.get("username"),
                body.get("password"));
    }
}