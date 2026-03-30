package com.internship.platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.platform.entity.User;
import com.internship.platform.entity.enums.Role;
import com.internship.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'acceptance — EPIC 1 Authentification
 * US-01: Créer un compte utilisateur
 * US-02: Connexion JWT
 * US-03: Gestion des rôles RBAC
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String RH_EMAIL = "rh.test@internship.com";
    private static final String RH_PASSWORD = "Rh1234!";

    @BeforeEach
    void setUp() {
        User rh = User.builder()
                .email(RH_EMAIL)
                .password(passwordEncoder.encode(RH_PASSWORD))
                .firstName("Marie").lastName("Dupont")
                .role(Role.RH).enabled(true).build();
        userRepository.save(rh);
    }

    // ─── US-02 : Connexion ────────────────────────────────────────────────────

    @Test
    void login_shouldReturn200WithJwt_whenCredentialsAreValid() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", RH_EMAIL, "password", RH_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("RH"))
                .andExpect(jsonPath("$.email").value(RH_EMAIL));
    }

    @Test
    void login_shouldReturn401_whenPasswordIsWrong() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", RH_EMAIL, "password", "wrongpassword"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_shouldReturn401_whenUserDoesNotExist() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "nobody@internship.com", "password", "any"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_shouldReturn400_whenEmailIsMissing() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("password", RH_PASSWORD))))
                .andExpect(status().isBadRequest());
    }

    // ─── US-01 : Création de compte ──────────────────────────────────────────

    @Test
    void register_shouldReturn200_whenCalledByRH() throws Exception {
        String token = loginAndGetToken(RH_EMAIL, RH_PASSWORD);

        Map<String, Object> request = Map.of(
                "email", "new.stagiaire@internship.com",
                "password", "Stage1234!",
                "firstName", "Karim",
                "lastName", "Amrani",
                "role", "STAGIAIRE");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new.stagiaire@internship.com"))
                .andExpect(jsonPath("$.role").value("STAGIAIRE"));
    }

    @Test
    void register_shouldReturn403_whenCalledWithoutToken() throws Exception {
        Map<String, Object> request = Map.of(
                "email", "unauthorized@internship.com",
                "password", "Stage1234!",
                "firstName", "Test", "lastName", "User",
                "role", "STAGIAIRE");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_shouldReturn400_whenEmailIsInvalid() throws Exception {
        String token = loginAndGetToken(RH_EMAIL, RH_PASSWORD);

        Map<String, Object> request = Map.of(
                "email", "not-an-email",
                "password", "Stage1234!",
                "firstName", "Test", "lastName", "User",
                "role", "STAGIAIRE");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ─── US-03 : RBAC ────────────────────────────────────────────────────────

    @Test
    void register_shouldReturn403_whenCalledByStagiaire() throws Exception {
        // Create a STAGIAIRE user first
        User stagiaire = User.builder()
                .email("stg.test@internship.com")
                .password(passwordEncoder.encode("Stage1234!"))
                .firstName("Karim").lastName("Test")
                .role(Role.STAGIAIRE).enabled(true).build();
        userRepository.save(stagiaire);

        String token = loginAndGetToken("stg.test@internship.com", "Stage1234!");

        Map<String, Object> request = Map.of(
                "email", "victim@internship.com",
                "password", "Stage1234!",
                "firstName", "Test", "lastName", "User",
                "role", "RH");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
