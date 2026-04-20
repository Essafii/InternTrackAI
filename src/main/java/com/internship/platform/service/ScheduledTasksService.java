package com.internship.platform.service;

import com.internship.platform.entity.ScoreSnapshot;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.enums.StatutStagiaire;
import com.internship.platform.repository.ScoreSnapshotRepository;
import com.internship.platform.repository.StagiaireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksService {

    private final ScoringDomainService scoringDomainService;
    private final StagiaireRepository stagiaireRepository;
    private final ScoreSnapshotRepository scoreSnapshotRepository;

    @Scheduled(cron = "0 0 9 * * MON")
    @Transactional
    public void recalculerClassementHebdomadaire() {
        List<Stagiaire> actifs = stagiaireRepository.findByStatut(StatutStagiaire.ACTIF);
        LocalDateTime now = LocalDateTime.now();

        for (Stagiaire s : actifs) {
            ScoringResult result = scoringDomainService.computeScore(s.getId());

            // Persist live score on the stagiaire for DB-level ranking
            s.setScoreCalcule(result.scoreGlobal());
            stagiaireRepository.save(s);

            // Create temporal snapshot for historical trend tracking
            ScoreSnapshot snapshot = ScoreSnapshot.builder()
                    .stagiaire(s)
                    .scoreGlobal(result.scoreGlobal())
                    .dateCalcul(now)
                    .build();
            scoreSnapshotRepository.save(snapshot);
        }

        log.info("Weekly Snapshot: Persisted scores for {} active interns.", actifs.size());
    }
}
