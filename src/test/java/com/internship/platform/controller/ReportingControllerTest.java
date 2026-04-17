package com.internship.platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.platform.entity.User;
import com.internship.platform.entity.enums.Role;
import com.internship.platform.entity.enums.TypeRapport;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'acceptance — EPIC 5 Reporting
 * US-30: Générer un rapport PDF pour un stagiaire
 * US-32: Consulter la liste des rapports générés
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReportingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String rhToken;
    private String stagiaireToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.save(User.builder()
                .email("rh@reporting.test").password(passwordEncoder.encode("Rh1234!"))
                .firstName("RH").lastName("Report").role(Role.RH).enabled(true).build());

        userRepository.save(User.builder()
                .email("stg@reporting.test").password(passwordEncoder.encode("Stg1234!"))
                .firstName("Sta").lastName("Giaire").role(Role.STAGIAIRE).enabled(true).build());

        rhToken = loginAndGetToken("rh@reporting.test", "Rh1234!");
        stagiaireToken = loginAndGetToken("stg@reporting.test", "Stg1234!");
    }

    // ─── US-30 : Générer un rapport ────────────────────────────────────────────

    @Test
    void generateReport_shouldReturn202WithJob_whenCalledByRH() throws Exception {
        mockMvc.perform(post("/reporting/generate")
                        .header("Authorization", "Bearer " + rhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "RAPPORT_CAMPAGNE"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.type").value("RAPPORT_CAMPAGNE"))
                .andExpect(jsonPath("$.statut").isNotEmpty());
    }

    @Test
    void generateReport_shouldReturn403_whenCalledByStagiaire() throws Exception {
        mockMvc.perform(post("/reporting/generate")
                        .header("Authorization", "Bearer " + stagiaireToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "RAPPORT_CAMPAGNE"))))
                .andExpect(status().isForbidden());
    }

    // ─── US-32 : Consulter les jobs ────────────────────────────────────────────

    @Test
    void getJobs_shouldReturnPageOfJobs_afterGenerate() throws Exception {
        mockMvc.perform(post("/reporting/generate")
                        .header("Authorization", "Bearer " + rhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "RAPPORT_CAMPAGNE"))))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/reporting/jobs")
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    void getJobs_shouldReturn403_whenCalledByStagiaire() throws Exception {
        mockMvc.perform(get("/reporting/jobs")
                        .header("Authorization", "Bearer " + stagiaireToken))
                .andExpect(status().isForbidden());
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
