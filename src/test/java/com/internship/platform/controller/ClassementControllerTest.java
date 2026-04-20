package com.internship.platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.platform.entity.Evaluation;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.User;
import com.internship.platform.entity.enums.Role;
import com.internship.platform.entity.enums.StatutStagiaire;
import com.internship.platform.entity.enums.TypeEvaluation;
import com.internship.platform.repository.EvaluationRepository;
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
 * Phase 5: Rank accuracy & encadrant isolation
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
    @Autowired EvaluationRepository evaluationRepository;
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

    // ─── Phase 5: Rank accuracy with DB-level aggregation ─────────────────────

    @Test
    void getMyRank_shouldReturnCorrectRanks_forThreeInternsWithDifferentScores() throws Exception {
        // Seed 3 interns with validated evaluations producing different scores:
        //   A: noteMoyenne=18 → scoreGlobal ~96  → rank 1
        //   B: noteMoyenne=15 → scoreGlobal ~90  → rank 2
        //   C: noteMoyenne=5  → scoreGlobal ~65  → rank 3
        String tokenA = createScoredIntern("internA@classement.test", "InternA1!", "Alpha", "Top", 18.0);
        String tokenB = createScoredIntern("internB@classement.test", "InternB1!", "Beta", "Mid", 15.0);
        String tokenC = createScoredIntern("internC@classement.test", "InternC1!", "Gamma", "Low", 5.0);

        // Call /classement/me for each in descending score order
        // Each call computes + persists scoreCalcule, so subsequent rank queries see updated values

        // Intern A: highest score → rank 1
        mockMvc.perform(get("/classement/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rang").value(1));

        // Intern B: second highest → rank 2
        mockMvc.perform(get("/classement/me")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rang").value(2));

        // Intern C: lowest → rank 3 (original stagiaire has scoreCalcule=0 → below C)
        mockMvc.perform(get("/classement/me")
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rang").value(3));
    }

    // ─── Phase 5: Encadrant isolation ─────────────────────────────────────────

    @Test
    void getClassement_shouldReturn403_whenEncadrantQueriesOtherEncadrantsTeam() throws Exception {
        // Create a second encadrant
        User encadrantB = userRepository.save(User.builder()
                .email("encB@classement.test").password(passwordEncoder.encode("EncB1234!"))
                .firstName("EncB").lastName("Other").role(Role.ENCADRANT).enabled(true).build());

        // Encadrant A (from setUp) tries to query encadrant B's team → 403
        mockMvc.perform(get("/classement")
                        .param("encadrantId", encadrantB.getId().toString())
                        .header("Authorization", "Bearer " + encadrantToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getClassement_shouldReturn403_whenEncadrantFiltersOnlyByEquipe() throws Exception {
        // Encadrant tries to filter by equipe alone (bypassing encadrant restriction) → 403
        mockMvc.perform(get("/classement")
                        .param("equipe", "Équipe Beta")
                        .header("Authorization", "Bearer " + encadrantToken))
                .andExpect(status().isForbidden());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Creates a STAGIAIRE user + Stagiaire entity + a validated Evaluation with the given
     * noteMoyenne, and returns the JWT token for that user.
     */
    private String createScoredIntern(String email, String password,
                                      String firstName, String lastName,
                                      double noteMoyenne) throws Exception {
        User user = userRepository.save(User.builder()
                .email(email).password(passwordEncoder.encode(password))
                .firstName(firstName).lastName(lastName)
                .role(Role.STAGIAIRE).enabled(true).build());

        Stagiaire stag = stagiaireRepository.save(Stagiaire.builder()
                .user(user).encadrant(encadrant)
                .sujet("Sujet " + firstName).equipe("Équipe " + firstName)
                .dateDebut(LocalDate.now().minusMonths(2))
                .dateFin(LocalDate.now().plusMonths(4))
                .statut(StatutStagiaire.ACTIF).build());

        // Seed a validated evaluation with the specified noteMoyenne
        // The scoring service uses: evaluationRepository.findAverageNoteByStaigaire()
        // which filters on validee = true
        evaluationRepository.save(Evaluation.builder()
                .stagiaire(stag)
                .evaluateur(encadrant)
                .type(TypeEvaluation.MENSUELLE)
                .dateEvaluation(LocalDate.now())
                .mois(1)
                .noteTechnique(noteMoyenne)
                .noteProgression(noteMoyenne)
                .noteDelais(noteMoyenne)
                .noteQualite(noteMoyenne)
                .noteAutonomie(noteMoyenne)
                .noteCommunication(noteMoyenne)
                .noteMoyenne(noteMoyenne)
                .validee(true)
                .build());

        return loginAndGetToken(email, password);
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
