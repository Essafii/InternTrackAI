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
 * Tests d'acceptance — EPIC 3 Gestion des Tâches
 * US-14: Créer une tâche
 * US-15: Voir mes tâches
 * US-16: Mettre à jour l'état d'une tâche
 * US-17: Replanifier une tâche
 * US-18: Tâches EN_RETARD (via @Scheduled — testé unitairement dans TacheServiceTest)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TacheControllerTest {

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
                .dateDebut(LocalDate.now().minusMonths(1))
                .dateFin(LocalDate.now().plusMonths(5))
                .statut(StatutStagiaire.ACTIF).build());

        rhToken = loginAndGetToken("rh@test.com", "Rh1234!");
        encadrantToken = loginAndGetToken("enc@test.com", "Enc1234!");
        stagiaireToken = loginAndGetToken("stg@test.com", "Stage1234!");
    }

    // ─── US-14 : Créer une tâche ──────────────────────────────────────────────

    @Test
    void creer_shouldReturn201WithTache_whenCalledByEncadrant() throws Exception {
        Map<String, Object> request = Map.of(
                "titre", "Analyse des besoins fonctionnels",
                "description", "Rédiger le cahier des charges",
                "dateDebut", LocalDate.now().toString(),
                "deadline", LocalDate.now().plusWeeks(2).toString(),
                "priorite", 1);

        mockMvc.perform(post("/taches/stagiaire/" + stagiaire.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + encadrantToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titre").value("Analyse des besoins fonctionnels"))
                .andExpect(jsonPath("$.etat").value("A_FAIRE"))
                .andExpect(jsonPath("$.stagiaireId").value(stagiaire.getId()));
    }

    @Test
    void creer_shouldReturn403_whenCalledByStagiaire() throws Exception {
        Map<String, Object> request = Map.of(
                "titre", "Auto-assignation interdite",
                "dateDebut", LocalDate.now().toString(),
                "deadline", LocalDate.now().plusWeeks(1).toString());

        mockMvc.perform(post("/taches/stagiaire/" + stagiaire.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + stagiaireToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void creer_shouldReturn400_whenTitreIsMissing() throws Exception {
        Map<String, Object> request = Map.of(
                "deadline", LocalDate.now().plusWeeks(1).toString());

        mockMvc.perform(post("/taches/stagiaire/" + stagiaire.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + rhToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ─── US-15 : Voir mes tâches ─────────────────────────────────────────────

    @Test
    void getByStagiaire_shouldReturnPagedTaches_whenCalledByRH() throws Exception {
        // Create a task first
        createTache(rhToken, "Tâche visible RH");

        mockMvc.perform(get("/taches/stagiaire/" + stagiaire.getId())
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getByStagiaire_shouldReturnOwnTaches_whenCalledByStagiaire() throws Exception {
        createTache(rhToken, "Tâche pour stagiaire");

        mockMvc.perform(get("/taches/stagiaire/" + stagiaire.getId())
                        .header("Authorization", "Bearer " + stagiaireToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titre").value("Tâche pour stagiaire"));
    }

    @Test
    void getByStagiaire_shouldReturnEmptyPage_whenStagiaireHasNoTasks() throws Exception {
        mockMvc.perform(get("/taches/stagiaire/99999")
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ─── US-16 : Mettre à jour l'état ────────────────────────────────────────

    @Test
    void update_shouldReturn200WithTermine_whenStagiaireCompletesTask() throws Exception {
        Long tacheId = createTache(rhToken, "Tâche à compléter");

        Map<String, Object> updateRequest = Map.of("titre", "Tâche à compléter", "etat", "TERMINE");

        mockMvc.perform(put("/taches/" + tacheId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + stagiaireToken)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etat").value("TERMINE"))
                .andExpect(jsonPath("$.dateCompletion").isNotEmpty());
    }

    // ─── US-17 : Replanifier une tâche ───────────────────────────────────────

    @Test
    void update_shouldReturn200WithNewDeadline_whenEncadrantReschedules() throws Exception {
        Long tacheId = createTache(encadrantToken, "Tâche à replanifier");
        LocalDate newDeadline = LocalDate.now().plusMonths(2);

        Map<String, Object> updateRequest = Map.of(
                "titre", "Tâche à replanifier",
                "deadline", newDeadline.toString());

        mockMvc.perform(put("/taches/" + tacheId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + encadrantToken)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadline").value(newDeadline.toString()));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Long createTache(String token, String titre) throws Exception {
        Map<String, Object> request = Map.of(
                "titre", titre,
                "dateDebut", LocalDate.now().toString(),
                "deadline", LocalDate.now().plusWeeks(2).toString(),
                "priorite", 1);

        MvcResult result = mockMvc.perform(post("/taches/stagiaire/" + stagiaire.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

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
