package com.internship.platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.platform.entity.Notification;
import com.internship.platform.entity.User;
import com.internship.platform.entity.enums.Role;
import com.internship.platform.entity.enums.TypeNotification;
import com.internship.platform.repository.NotificationRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'acceptance — EPIC 7 Notifications
 * US-39: Voir ses notifications (paginé)
 * US-40: Marquer une notification comme lue
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String userToken;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        testUser = userRepository.save(User.builder()
                .email("notif@test.com")
                .password(passwordEncoder.encode("Notif1234!"))
                .firstName("Test").lastName("User")
                .role(Role.ENCADRANT).enabled(true).build());

        notificationRepository.save(Notification.builder()
                .destinataire(testUser)
                .titre("Bienvenue")
                .message("Votre compte a été créé")
                .type(TypeNotification.ONBOARDING)
                .build());

        userToken = loginAndGetToken("notif@test.com", "Notif1234!");
    }

    // ─── US-39 : Voir ses notifications ──────────────────────────────────────

    @Test
    void getNotifications_shouldReturn200WithPage_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titre").value("Bienvenue"));
    }

    @Test
    void getNotifications_shouldReturn403_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUnreadCount_shouldReturn1_whenOneUnreadNotification() throws Exception {
        mockMvc.perform(get("/notifications/unread-count")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    // ─── US-40 : Marquer comme lue ────────────────────────────────────────────

    @Test
    void markAllAsRead_shouldReturn204_whenAuthenticated() throws Exception {
        mockMvc.perform(patch("/notifications/mark-all-read")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

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
