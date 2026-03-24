package com.internship.platform.controller;

import com.internship.platform.dto.auth.AuthRequest;
import com.internship.platform.dto.auth.AuthResponse;
import com.internship.platform.dto.auth.RefreshTokenRequest;
import com.internship.platform.dto.auth.RegisterRequest;
import com.internship.platform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Créer un compte (RH/ADMIN uniquement)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir le token d'accès")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshAccessToken(request.getRefreshToken()));
    }
}
