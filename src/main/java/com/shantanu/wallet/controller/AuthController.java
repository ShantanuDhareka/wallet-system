package com.shantanu.wallet.controller;

import com.shantanu.wallet.dto.RegisterRequest;

import com.shantanu.wallet.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.shantanu.wallet.dto.AuthResponse;
import com.shantanu.wallet.dto.LoginRequest;
import com.shantanu.wallet.entity.User;
import com.shantanu.wallet.service.AuthService;
import com.shantanu.wallet.service.JwtService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(
            UserService userService,
            AuthService authService,
            JwtService jwtService) {

        this.userService = userService;
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {

        userService.register(request);

        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        User user = authService.authenticate(
                request.email(),
                request.password());

        String token = jwtService.generateToken(user.getEmail());

        return ResponseEntity.ok(new AuthResponse(token));
    }
}