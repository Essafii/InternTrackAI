package com.internship.platform.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String email;
    private String role;
    private String fullName;
    private Long userId;
}
