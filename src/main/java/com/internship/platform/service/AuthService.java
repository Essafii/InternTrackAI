package com.internship.platform.service;

import com.internship.platform.dto.auth.AuthRequest;
import com.internship.platform.dto.auth.AuthResponse;
import com.internship.platform.dto.auth.RegisterRequest;
import com.internship.platform.entity.RefreshToken;
import com.internship.platform.entity.User;
import com.internship.platform.entity.enums.Role;
import com.internship.platform.exception.BusinessException;
import com.internship.platform.repository.RefreshTokenRepository;
import com.internship.platform.repository.UserRepository;
import com.internship.platform.security.CustomUserDetails;
import com.internship.platform.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs; // 7 days default

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email déjà utilisé: " + request.getEmail());
        }
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole() != null ? request.getRole() : Role.STAGIAIRE)
                .phone(request.getPhone())
                .department(request.getDepartment())
                .enabled(true)
                .build();
        userRepository.save(user);
        String token = jwtUtil.generateToken(new CustomUserDetails(user));
        String refreshToken = createRefreshToken(user);
        return new AuthResponse(token, refreshToken, user.getEmail(), user.getRole().name(), user.getFullName(),
                user.getId());
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);
        User user = userDetails.getUser();
        String refreshToken = createRefreshToken(user);
        return new AuthResponse(token, refreshToken, user.getEmail(), user.getRole().name(), user.getFullName(),
                user.getId());
    }

    @Transactional
    public AuthResponse refreshAccessToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException("Refresh token invalide"));
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException("Refresh token expiré, veuillez vous reconnecter");
        }
        User user = refreshToken.getUser();
        String newAccessToken = jwtUtil.generateToken(new CustomUserDetails(user));
        // Rotate: delete old, create new refresh token
        refreshTokenRepository.delete(refreshToken);
        String newRefreshToken = createRefreshToken(user);
        return new AuthResponse(newAccessToken, newRefreshToken, user.getEmail(), user.getRole().name(),
                user.getFullName(), user.getId());
    }

    private String createRefreshToken(User user) {
        // Delete existing tokens for this user
        refreshTokenRepository.deleteByUser(user);
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();
        return refreshTokenRepository.save(token).getToken();
    }
}
