package com.internship.platform.service;

import com.internship.platform.entity.ScoreSnapshot;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.User;
import com.internship.platform.entity.enums.Role;
import com.internship.platform.entity.enums.StatutStagiaire;
import com.internship.platform.repository.ScoreSnapshotRepository;
import com.internship.platform.repository.StagiaireRepository;
import com.internship.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScheduledTasksService — Phase 4 temporal snapshot persistence.
 * Validates that recalculerClassementHebdomadaire() creates ScoreSnapshot
 * records and updates scoreCalcule for all active interns.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScheduledTasksServiceTest {

    @Autowired ScheduledTasksService scheduledTasksService;
    @Autowired StagiaireRepository stagiaireRepository;
    @Autowired ScoreSnapshotRepository scoreSnapshotRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Stagiaire stagiaireA;
    private Stagiaire stagiaireB;

    @BeforeEach
    void setUp() {
        User encadrant = userRepository.save(User.builder()
                .email("enc@scheduled.test").password(passwordEncoder.encode("Enc1234!"))
                .firstName("Enc").lastName("Adrant").role(Role.ENCADRANT).enabled(true).build());

        User userA = userRepository.save(User.builder()
                .email("stgA@scheduled.test").password(passwordEncoder.encode("StgA1234!"))
                .firstName("Alice").lastName("Active").role(Role.STAGIAIRE).enabled(true).build());

        User userB = userRepository.save(User.builder()
                .email("stgB@scheduled.test").password(passwordEncoder.encode("StgB1234!"))
                .firstName("Bob").lastName("Active").role(Role.STAGIAIRE).enabled(true).build());

        // Intern C is TERMINE — should NOT get a snapshot
        User userC = userRepository.save(User.builder()
                .email("stgC@scheduled.test").password(passwordEncoder.encode("StgC1234!"))
                .firstName("Charlie").lastName("Terminated").role(Role.STAGIAIRE).enabled(true).build());

        stagiaireA = stagiaireRepository.save(Stagiaire.builder()
                .user(userA).encadrant(encadrant)
                .sujet("Sujet A").equipe("Équipe Alpha")
                .dateDebut(LocalDate.now().minusMonths(2))
                .dateFin(LocalDate.now().plusMonths(4))
                .statut(StatutStagiaire.ACTIF).build());

        stagiaireB = stagiaireRepository.save(Stagiaire.builder()
                .user(userB).encadrant(encadrant)
                .sujet("Sujet B").equipe("Équipe Alpha")
                .dateDebut(LocalDate.now().minusMonths(1))
                .dateFin(LocalDate.now().plusMonths(5))
                .statut(StatutStagiaire.ACTIF).build());

        stagiaireRepository.save(Stagiaire.builder()
                .user(userC).encadrant(encadrant)
                .sujet("Sujet C").equipe("Équipe Alpha")
                .dateDebut(LocalDate.now().minusMonths(6))
                .dateFin(LocalDate.now().minusMonths(1))
                .statut(StatutStagiaire.TERMINE).build());
    }

    @Test
    void recalculerClassement_shouldCreateSnapshotsForAllActiveInterns() {
        // No snapshots should exist before the run
        long snapshotsBefore = scoreSnapshotRepository.count();

        scheduledTasksService.recalculerClassementHebdomadaire();

        // Exactly 2 active interns → 2 new snapshots
        List<ScoreSnapshot> allSnapshots = scoreSnapshotRepository.findAll();
        long newSnapshots = allSnapshots.size() - snapshotsBefore;
        assertEquals(2, newSnapshots,
                "Should create exactly 2 snapshots (one per ACTIF intern, not for TERMINE)");

        // Verify snapshots exist for both active interns
        List<ScoreSnapshot> snapshotsA = scoreSnapshotRepository
                .findByStagiaireIdOrderByDateCalculAsc(stagiaireA.getId());
        List<ScoreSnapshot> snapshotsB = scoreSnapshotRepository
                .findByStagiaireIdOrderByDateCalculAsc(stagiaireB.getId());

        assertEquals(1, snapshotsA.size(), "Stagiaire A should have 1 snapshot");
        assertEquals(1, snapshotsB.size(), "Stagiaire B should have 1 snapshot");

        // Verify snapshot data is populated
        ScoreSnapshot snapA = snapshotsA.get(0);
        assertNotNull(snapA.getScoreGlobal(), "scoreGlobal must be set");
        assertNotNull(snapA.getDateCalcul(), "dateCalcul must be set");
        assertTrue(snapA.getScoreGlobal() >= 0, "scoreGlobal should be non-negative");
    }

    @Test
    void recalculerClassement_shouldUpdateScoreCalculeOnStagiaire() {
        // Before: scoreCalcule is default (0.0)
        assertEquals(0.0, stagiaireA.getScoreCalcule(), 0.001);
        assertEquals(0.0, stagiaireB.getScoreCalcule(), 0.001);

        scheduledTasksService.recalculerClassementHebdomadaire();

        // Reload from DB to verify persistence
        Stagiaire reloadedA = stagiaireRepository.findById(stagiaireA.getId()).orElseThrow();
        Stagiaire reloadedB = stagiaireRepository.findById(stagiaireB.getId()).orElseThrow();

        assertTrue(reloadedA.getScoreCalcule() > 0,
                "scoreCalcule should be updated to a positive value for Stagiaire A");
        assertTrue(reloadedB.getScoreCalcule() > 0,
                "scoreCalcule should be updated to a positive value for Stagiaire B");
    }

    @Test
    void recalculerClassement_shouldBeIdempotent_creatingNewSnapshotsEachRun() {
        scheduledTasksService.recalculerClassementHebdomadaire();
        long countAfterFirst = scoreSnapshotRepository.count();

        scheduledTasksService.recalculerClassementHebdomadaire();
        long countAfterSecond = scoreSnapshotRepository.count();

        assertEquals(countAfterFirst * 2, countAfterSecond,
                "Each run should create a new set of snapshots (temporal log, not upsert)");
    }
}
