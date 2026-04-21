package com.internship.platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.platform.entity.ReportJob;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.User;
import com.internship.platform.entity.enums.Role;
import com.internship.platform.entity.enums.StatutRapport;
import com.internship.platform.entity.enums.StatutStagiaire;
import com.internship.platform.repository.RefreshTokenRepository;
import com.internship.platform.repository.ReportJobRepository;
import com.internship.platform.repository.StagiaireRepository;
import com.internship.platform.repository.UserRepository;
import com.internship.platform.service.ReportingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'acceptance — EPIC 5 Reporting
 * US-30: Générer un rapport PDF pour un stagiaire
 * US-32: Consulter la liste des rapports générés
 *
 * NOT @Transactional — async generation thread needs to see committed data.
 * Cleanup handled manually in @AfterEach.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ReportJobRepository reportJobRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired StagiaireRepository stagiaireRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ReportingService reportingService;

    @Value("${app.storage.upload-dir:./uploads}")
    private String uploadDir;

    private String rhToken;
    private String stagiaireToken;
    private String encadrantToken;
    private User encadrantUser;

    @BeforeEach
    void setUp() throws Exception {
        // Idempotent setup — delete if left over from a previous run
        cleanUsers();

        userRepository.save(User.builder()
                .email("rh@reporting.test").password(passwordEncoder.encode("Rh1234!"))
                .firstName("RH").lastName("Report").role(Role.RH).enabled(true).build());

        userRepository.save(User.builder()
                .email("stg@reporting.test").password(passwordEncoder.encode("Stg1234!"))
                .firstName("Sta").lastName("Giaire").role(Role.STAGIAIRE).enabled(true).build());

        encadrantUser = userRepository.save(User.builder()
                .email("enc@reporting.test").password(passwordEncoder.encode("Enc1234!"))
                .firstName("Enc").lastName("Adrant").role(Role.ENCADRANT).enabled(true).build());

        rhToken = loginAndGetToken("rh@reporting.test", "Rh1234!");
        stagiaireToken = loginAndGetToken("stg@reporting.test", "Stg1234!");
        encadrantToken = loginAndGetToken("enc@reporting.test", "Enc1234!");
    }

    @AfterEach
    void tearDown() {
        cleanUsers();
    }

    // ─── US-30 : Générer un rapport ────────────────────────────────────────────

    @Test
    void generateReport_shouldReturn202WithPendingJob_whenCalledByRH() throws Exception {
        mockMvc.perform(post("/reporting/generate")
                        .header("Authorization", "Bearer " + rhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "RAPPORT_CAMPAGNE"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.type").value("RAPPORT_CAMPAGNE"))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
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

    @Test
    void generateReport_shouldCompleteWithTermineStatus_afterAsync() throws Exception {
        MvcResult result = mockMvc.perform(post("/reporting/generate")
                        .header("Authorization", "Bearer " + rhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "RAPPORT_CAMPAGNE"))))
                .andExpect(status().isAccepted())
                .andReturn();

        Long jobId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        // Poll DB until async thread finishes (max 5 s)
        long deadline = System.currentTimeMillis() + 5_000;
        ReportJob job = null;
        while (System.currentTimeMillis() < deadline) {
            job = reportJobRepository.findById(jobId).orElse(null);
            if (job != null
                    && job.getStatut() != StatutRapport.EN_ATTENTE
                    && job.getStatut() != StatutRapport.EN_COURS) {
                break;
            }
            Thread.sleep(200);
        }

        assertNotNull(job, "ReportJob should exist in DB");
        assertEquals(StatutRapport.TERMINE, job.getStatut(),
                "Async generation should complete with TERMINE — got: "
                        + (job != null ? job.getStatut() : "null"));
        assertNotNull(job.getFilePath(), "filePath must be set after generation");
        assertNotNull(job.getDateGeneration(), "dateGeneration must be set");
    }

    // ─── Phase 5: Verify PDF file physically exists ────────────────────────────

    @Test
    void generateReport_shouldCreatePhysicalPdfFile_afterAsyncCompletion() throws Exception {
        MvcResult result = mockMvc.perform(post("/reporting/generate")
                        .header("Authorization", "Bearer " + rhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "RAPPORT_CAMPAGNE"))))
                .andExpect(status().isAccepted())
                .andReturn();

        Long jobId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        ReportJob job = pollUntilTerminal(jobId);
        assertEquals(StatutRapport.TERMINE, job.getStatut(), "Job must be TERMINE");

        // Verify the physical PDF file exists on disk
        Path pdfPath = Paths.get(uploadDir, job.getFilePath()).toAbsolutePath().normalize();
        assertTrue(Files.exists(pdfPath),
                "PDF file should physically exist at: " + pdfPath);
        assertTrue(Files.size(pdfPath) > 0,
                "PDF file should not be empty");
    }

    // ─── Phase 5: Verify ERREUR failover ───────────────────────────────────────

    @Test
    void generateReport_shouldTransitionToErreur_whenPdfGenerationFails() throws Exception {
        // Override uploadDir to an invalid path to force file-write failure
        String originalDir = (String) ReflectionTestUtils.getField(reportingService, "uploadDir");
        ReflectionTestUtils.setField(reportingService, "uploadDir", "\0invalid_path");

        try {
            MvcResult result = mockMvc.perform(post("/reporting/generate")
                            .header("Authorization", "Bearer " + rhToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("type", "RAPPORT_CAMPAGNE"))))
                    .andExpect(status().isAccepted())
                    .andReturn();

            Long jobId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("id").asLong();

            ReportJob job = pollUntilTerminal(jobId);
            assertEquals(StatutRapport.ERREUR, job.getStatut(),
                    "Job should transition to ERREUR when PDF generation fails");
            assertNotNull(job.getErreur(),
                    "Error message should be stored on the job");
            assertFalse(job.getErreur().isBlank(),
                    "Error message should not be blank");
        } finally {
            // Restore original uploadDir so other tests are unaffected
            ReflectionTestUtils.setField(reportingService, "uploadDir", originalDir);
        }
    }

    // ─── Phase 5: Verify ENCADRANT cannot generate RAPPORT_CAMPAGNE ────────────

    @Test
    void generateReport_shouldReturn403_whenEncadrantRequestsRapportCampagne() throws Exception {
        mockMvc.perform(post("/reporting/generate")
                        .header("Authorization", "Bearer " + encadrantToken)
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

    private ReportJob pollUntilTerminal(Long jobId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        ReportJob job = null;
        while (System.currentTimeMillis() < deadline) {
            job = reportJobRepository.findById(jobId).orElse(null);
            if (job != null
                    && job.getStatut() != StatutRapport.EN_ATTENTE
                    && job.getStatut() != StatutRapport.EN_COURS) {
                break;
            }
            Thread.sleep(200);
        }
        assertNotNull(job, "ReportJob must exist in DB after polling");
        return job;
    }

    private void cleanUsers() {
        userRepository.findByEmail("rh@reporting.test").ifPresent(u -> {
            reportJobRepository.findByDemandeurIdOrderByCreatedAtDesc(u.getId(), Pageable.unpaged())
                    .forEach(reportJobRepository::delete);
            refreshTokenRepository.deleteByUser(u);
            userRepository.delete(u);
        });
        userRepository.findByEmail("stg@reporting.test").ifPresent(u -> {
            refreshTokenRepository.deleteByUser(u);
            userRepository.delete(u);
        });
        userRepository.findByEmail("enc@reporting.test").ifPresent(u -> {
            reportJobRepository.findByDemandeurIdOrderByCreatedAtDesc(u.getId(), Pageable.unpaged())
                    .forEach(reportJobRepository::delete);
            // Clean up any stagiaires created for encadrant tests
            stagiaireRepository.findByEncadrantId(u.getId())
                    .forEach(stagiaireRepository::delete);
            refreshTokenRepository.deleteByUser(u);
            userRepository.delete(u);
        });
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
