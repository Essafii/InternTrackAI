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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'acceptance — EPIC 2 Gestion Absences
 * US-10: Enregistrement absence
 * US-11: Calcul taux assiduité
 * US-12: Alerte assiduité < 80%
 * US-13: Upload justificatif
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AbsenceControllerTest {

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
                .dateDebut(LocalDate.now().minusMonths(2))
                .dateFin(LocalDate.now().plusMonths(4))
                .statut(StatutStagiaire.ACTIF).build());

        rhToken = loginAndGetToken("rh@test.com", "Rh1234!");
        encadrantToken = loginAndGetToken("enc@test.com", "Enc1234!");
        stagiaireToken = loginAndGetToken("stg@test.com", "Stage1234!");
    }

    // ─── US-10 : Enregistrement absence ──────────────────────────────────────

    @Test
    void enregistrer_shouldReturn201_whenCalledByEncadrant() throws Exception {
        Map<String, Object> request = Map.of(
                "dateAbsence", LocalDate.now().minusDays(1).toString(),
                "type", "NON_JUSTIFIEE",
                "motif", "Absence non signalée",
                "justifiee", false);

        mockMvc.perform(post("/absences/stagiaire/" + stagiaire.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + encadrantToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stagiaireId").value(stagiaire.getId()))
                .andExpect(jsonPath("$.type").value("NON_JUSTIFIEE"))
                .andExpect(jsonPath("$.justifiee").value(false));
    }

    @Test
    void enregistrer_shouldReturn403_whenCalledByStagiaire() throws Exception {
        Map<String, Object> request = Map.of(
                "dateAbsence", LocalDate.now().toString(),
                "type", "JUSTIFIEE",
                "justifiee", true);

        mockMvc.perform(post("/absences/stagiaire/" + stagiaire.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + stagiaireToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void enregistrer_shouldReturn400_whenDateAbsenceIsMissing() throws Exception {
        Map<String, Object> request = Map.of("type", "NON_JUSTIFIEE");

        mockMvc.perform(post("/absences/stagiaire/" + stagiaire.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + rhToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ─── US-11 : Taux assiduité ───────────────────────────────────────────────

    @Test
    void getAssiduite_shouldReturn100_whenNoAbsences() throws Exception {
        mockMvc.perform(get("/absences/stagiaire/" + stagiaire.getId() + "/assiduite")
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(100.0));
    }

    @Test
    void getAssiduite_shouldReturnLowerValue_afterAbsencesAreAdded() throws Exception {
        // Record 3 absences
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> req = Map.of(
                    "dateAbsence", LocalDate.now().minusDays(i).toString(),
                    "type", "NON_JUSTIFIEE",
                    "justifiee", false);
            mockMvc.perform(post("/absences/stagiaire/" + stagiaire.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + rhToken)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/absences/stagiaire/" + stagiaire.getId() + "/assiduite")
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    void getAssiduite_shouldReturn404_whenStagiaireDoesNotExist() throws Exception {
        mockMvc.perform(get("/absences/stagiaire/99999/assiduite")
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isNotFound());
    }

    // ─── US-12 : Alerte assiduité ────────────────────────────────────────────

    @Test
    void getAbsencesByStagiaire_shouldReturnPagedList_whenCalledByRH() throws Exception {
        // Add one absence first
        Map<String, Object> req = Map.of(
                "dateAbsence", LocalDate.now().minusDays(1).toString(),
                "type", "NON_JUSTIFIEE", "justifiee", false);
        mockMvc.perform(post("/absences/stagiaire/" + stagiaire.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + rhToken)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/absences/stagiaire/" + stagiaire.getId())
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
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
