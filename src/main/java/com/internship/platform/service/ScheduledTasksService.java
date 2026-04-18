package com.internship.platform.service;

import com.internship.platform.entity.enums.StatutStagiaire;
import com.internship.platform.repository.StagiaireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksService {

    private final ClassementService classementService;
    private final StagiaireRepository stagiaireRepository;

    @Scheduled(cron = "0 0 9 * * MON")
    public void recalculerClassementHebdomadaire() {
        long count = stagiaireRepository.countByStatut(StatutStagiaire.ACTIF);
        classementService.getClassement(null, null);
        log.info("Weekly scoring recalculation completed for {} stagiaires", count);
    }
}
