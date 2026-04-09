package com.internship.platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.User;
import com.internship.platform.entity.enums.Role;
import com.internship.platform.entity.enums.StatutStagiaire;
import com.internship.platform.repository.StagiaireRepository;
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

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'acceptance — EPIC 2 Gestion Stagiaires
 * US-04: Onboarding stagiaire
 * US-07: Liste/filtrage stagiaires
 * US-08: Fiche détaillée stagiaire
 * US-09: Mise à jour profil stagiaire
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StagiaireControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired StagiaireRepository stagiaireRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String rhToken;
    private String encadrantToken;
    private String stagiaireToken;
    private User encadrant;
    private Stagiaire stagiaire;

    @BeforeEach
    void setUp() throws Exception {
        User rh = userRepository.save(User.builder()
                .email("rh@test.com").password(passwordEncoder.encode("Rh1234!"))
                .firstName("Marie").lastName("Dupont").role(Role.RH).enabled(true).build());

        encadrant = userRepository.save(User.builder()
                .email("enc@test.com").password(passwordEncoder.encode("Enc1234!"))
                .firstName("Ahmed").lastName("Benali").role(Role.ENCADRANT).enabled(true).build());

        User stagUser = userRepository.save(User.builder()
                .email("stg@test.com").password(passwordEncoder.encode("Stage1234!"))
                .firstName("Karim").lastName("Amrani").role(Role.STAGIAIRE).enabled(true).build());

        stagiaire = stagiaireRepository.save(Stagiaire.builder()
                .user(stagUser).encadrant(encadrant)
                .sujet("Développement API REST").equipe("Équipe Alpha")
                .dateDebut(LocalDate.now().minusMonths(1))
                .dateFin(LocalDate.now().plusMonths(5))
                .statut(StatutStagiaire.ACTIF).build());

        rhToken = loginAndGetToken("rh@test.com", "Rh1234!");
        encadrantToken = loginAndGetToken("enc@test.com", "Enc1234!");
        stagiaireToken = loginAndGetToken("stg@test.com", "Stage1234!");
    }

    // ─── US-04 : Onboarding ──────────────────────────────────────────────────

    @Test
    void onboard_shouldReturn201_whenCalledByRH() throws Exception {
        Map<String, Object> request = Map.of(
                "email", "new.intern@internship.com",
                "password", "Stage1234!",
                "firstName", "Fatima", "lastName", "Zahra",
                "sujet", "Machine Learning avec Python",
                "equipe", "Équipe Beta",
                "dateDebut", LocalDate.now().toString(),
                "dateFin", LocalDate.now().plusMonths(6).toString(),
                "encadrantId", encadrant.getId());

        mockMvc.perform(post("/stagiaires/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + rhToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.intern@internship.com"))
                .andExpect(jsonPath("$.sujet").value("Machine Learning avec Python"))
                .andExpect(jsonPath("$.statut").value("ACTIF"));
    }

    @Test
    void onboard_shouldReturn403_whenCalledByStagiaire() throws Exception {
        Map<String, Object> request = Map.of(
                "email", "unauthorized@internship.com",
                "password", "Stage1234!",
                "firstName", "Test", "lastName", "User",
                "sujet", "Sujet test",
                "dateDebut", LocalDate.now().toString(),
                "dateFin", LocalDate.now().plusMonths(6).toString());

        mockMvc.perform(post("/stagiaires/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + stagiaireToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void onboard_shouldReturn400_whenRequiredFieldsMissing() throws Exception {
        Map<String, Object> request = Map.of(
                "email", "incomplete@internship.com",
                "password", "Stage1234!",
                "firstName", "Test", "lastName", "User");
        // Missing sujet, dateDebut, dateFin

        mockMvc.perform(post("/stagiaires/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + rhToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ─── US-07 : Liste stagiaires ─────────────────────────────────────────────

    @Test
    void getAll_shouldReturnPagedList_whenCalledByRH() throws Exception {
        mockMvc.perform(get("/stagiaires")
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void getAll_shouldReturn403_whenCalledWithoutToken() throws Exception {
        mockMvc.perform(get("/stagiaires"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_shouldReturnOnlyOwnStagiaires_whenCalledByEncadrant() throws Exception {
        mockMvc.perform(get("/stagiaires")
                        .header("Authorization", "Bearer " + encadrantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].encadrantId").value(encadrant.getId()));
    }

    // ─── US-08 : Fiche stagiaire ──────────────────────────────────────────────

    @Test
    void getById_shouldReturnStagiaire_whenCalledByRH() throws Exception {
        mockMvc.perform(get("/stagiaires/" + stagiaire.getId())
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(stagiaire.getId()))
                .andExpect(jsonPath("$.sujet").value("Développement API REST"))
                .andExpect(jsonPath("$.statut").value("ACTIF"));
    }

    @Test
    void getById_shouldReturn404_whenStagiaireDoesNotExist() throws Exception {
        mockMvc.perform(get("/stagiaires/99999")
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isNotFound());
    }

    // ─── US-09 : Mise à jour stagiaire ───────────────────────────────────────

    @Test
    void update_shouldReturn200_whenCalledByRH() throws Exception {
        Map<String, Object> updateRequest = Map.of(
                "sujet", "Nouveau sujet mis à jour",
                "equipe", "Équipe Gamma");

        mockMvc.perform(put("/stagiaires/" + stagiaire.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + rhToken)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sujet").value("Nouveau sujet mis à jour"));
    }

    @Test
    void update_shouldReturn403_whenCalledByStagiaire() throws Exception {
        Map<String, Object> updateRequest = Map.of("sujet", "Tentative non autorisée");

        mockMvc.perform(put("/stagiaires/" + stagiaire.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + stagiaireToken)
                        .content(objectMapper.writeValueAsString(updateRequest)))
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
