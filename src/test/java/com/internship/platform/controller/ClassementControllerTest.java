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
 * Tests d'acceptance — EPIC 6 Scoring IA & Classement
 * US-33: Calculer un score pondéré
 * US-35: Voir le classement global (RH)
 * US-36: Voir le classement de son équipe (Encadrant)
 * US-37: Détecter les stagiaires à risque (risqueIA)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClassementControllerTest {

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
        userRepository.save(User.builder()
                .email("rh@classement.test").password(passwordEncoder.encode("Rh1234!"))
                .firstName("RH").lastName("Admin").role(Role.RH).enabled(true).build());

        encadrant = userRepository.save(User.builder()
                .email("enc@classement.test").password(passwordEncoder.encode("Enc1234!"))
                .firstName("Enc").lastName("Adrant").role(Role.ENCADRANT).enabled(true).build());

        User stagUser = userRepository.save(User.builder()
                .email("stg@classement.test").password(passwordEncoder.encode("Stg1234!"))
                .firstName("Sta").lastName("Giaire").role(Role.STAGIAIRE).enabled(true).build());

        stagiaire = stagiaireRepository.save(Stagiaire.builder()
                .user(stagUser).encadrant(encadrant)
                .sujet("Développement IA").equipe("Équipe Beta")
                .dateDebut(LocalDate.now().minusMonths(1))
                .dateFin(LocalDate.now().plusMonths(5))
                .statut(StatutStagiaire.ACTIF).build());

        rhToken = loginAndGetToken("rh@classement.test", "Rh1234!");
        encadrantToken = loginAndGetToken("enc@classement.test", "Enc1234!");
        stagiaireToken = loginAndGetToken("stg@classement.test", "Stg1234!");
    }

    // ─── US-35 : Classement global ───────────────────────────────────────────

    @Test
    void getClassement_shouldReturn200WithList_whenCalledByRH() throws Exception {
        mockMvc.perform(get("/classement")
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].rang").value(1))
                .andExpect(jsonPath("$[0].scoreGlobal").isNumber())
                .andExpect(jsonPath("$[0].stagiaireNom").isNotEmpty());
    }

    @Test
    void getClassement_shouldReturn403_whenCalledByStagiaire() throws Exception {
        mockMvc.perform(get("/classement")
                        .header("Authorization", "Bearer " + stagiaireToken))
                .andExpect(status().isForbidden());
    }

    // ─── US-36 : Classement équipe ────────────────────────────────────────────

    @Test
    void getClassement_shouldReturnFilteredList_whenEncadrantIdProvided() throws Exception {
        mockMvc.perform(get("/classement")
                        .param("encadrantId", encadrant.getId().toString())
                        .header("Authorization", "Bearer " + encadrantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].equipe").value("Équipe Beta"));
    }

    // ─── US-37 : Stagiaire à risque ───────────────────────────────────────────

    @Test
    void getClassement_shouldIncludeRisqueIA_inResponse() throws Exception {
        mockMvc.perform(get("/classement")
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].risqueIA").isNumber())
                .andExpect(jsonPath("$[0].tauxAssiduite").isNumber());
    }

    // ─── GET /classement/me ───────────────────────────────────────────────────

    @Test
    void getMyRank_shouldReturn200WithOwnScore_whenStagiaire() throws Exception {
        mockMvc.perform(get("/classement/me")
                        .header("Authorization", "Bearer " + stagiaireToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stagiaireId").value(stagiaire.getId()))
                .andExpect(jsonPath("$.scoreGlobal").isNumber());
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
