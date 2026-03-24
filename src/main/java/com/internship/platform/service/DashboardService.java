package com.internship.platform.service;

import com.internship.platform.dto.dashboard.DashboardRhDto;
import com.internship.platform.dto.dashboard.DashboardEncadrantDto;
import com.internship.platform.dto.dashboard.DashboardStagiaireDto;
import com.internship.platform.dto.dashboard.DashboardTrendsDto;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.enums.EtatTache;
import com.internship.platform.entity.enums.StatutStagiaire;
import com.internship.platform.exception.ResourceNotFoundException;
import com.internship.platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

        private final StagiaireRepository stagiaireRepository;
        private final TacheRepository tacheRepository;
        private final AbsenceRepository absenceRepository;
        private final AbsenceService absenceService;
        private final EvaluationRepository evaluationRepository;
        private final LivrableRepository livrableRepository;
        private final NotificationRepository notificationRepository;

        public DashboardRhDto getDashboardRh() {
                DashboardRhDto dto = new DashboardRhDto();
                dto.setTotalStagiaires((int) stagiaireRepository.count());
                dto.setStagiairesActifs((int) stagiaireRepository.countByStatut(StatutStagiaire.ACTIF));
                dto.setStagiairesEnRetard((int) stagiaireRepository.countByStatut(StatutStagiaire.EN_RETARD));
                dto.setStagiairesTermines((int) stagiaireRepository.countByStatut(StatutStagiaire.TERMINE));
                dto.setStagiairesArchives((int) stagiaireRepository.countByStatut(StatutStagiaire.ARCHIVE));

                // Average assiduité
                List<Stagiaire> actifs = stagiaireRepository.findByStatut(StatutStagiaire.ACTIF);
                double avgAssiduite = actifs.stream()
                                .mapToDouble(s -> absenceService.calculerTauxAssiduite(s.getId()))
                                .average().orElse(100.0);
                dto.setTauxAssiduiteGlobal(avgAssiduite);
                return dto;
        }

        public DashboardTrendsDto getMonthlyTrends() {
                LocalDate since = LocalDate.now().minusMonths(11).withDayOfMonth(1);

                // Build month-label map for the last 12 months
                Map<Integer, String> monthLabels = new LinkedHashMap<>();
                for (int i = 11; i >= 0; i--) {
                        LocalDate m = LocalDate.now().minusMonths(i);
                        String label = m.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
                        monthLabels.put(m.getMonthValue(), label);
                }

                // Absence counts per month
                Map<Integer, Long> absenceMap = new HashMap<>();
                for (Object[] row : absenceRepository.countByMonth(since)) {
                        absenceMap.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
                }

                // Tache counts per month
                Map<Integer, Long> tacheMap = new HashMap<>();
                for (Object[] row : tacheRepository.countByMonth(since)) {
                        tacheMap.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
                }

                List<DashboardTrendsDto.MonthPoint> points = monthLabels.entrySet().stream()
                                .map(e -> new DashboardTrendsDto.MonthPoint(
                                                e.getValue(),
                                                absenceMap.getOrDefault(e.getKey(), 0L),
                                                tacheMap.getOrDefault(e.getKey(), 0L)))
                                .collect(Collectors.toList());

                // Team stats: group by encadrant
                List<DashboardTrendsDto.TeamStat> teamStats = stagiaireRepository
                                .findByStatut(StatutStagiaire.ACTIF)
                                .stream()
                                .filter(s -> s.getEncadrant() != null)
                                .collect(Collectors.groupingBy(s -> {
                                        String nom = s.getEncadrant().getFirstName() + " "
                                                        + s.getEncadrant().getLastName();
                                        return nom;
                                }))
                                .entrySet().stream()
                                .map(entry -> {
                                        List<Stagiaire> membres = entry.getValue();
                                        double avg = membres.stream()
                                                        .mapToDouble(s -> absenceService
                                                                        .calculerTauxAssiduite(s.getId()))
                                                        .average().orElse(100.0);
                                        return new DashboardTrendsDto.TeamStat(entry.getKey(), membres.size(),
                                                        Math.round(avg * 10.0) / 10.0);
                                })
                                .sorted(Comparator.comparingDouble(DashboardTrendsDto.TeamStat::getTauxAssiduite)
                                                .reversed())
                                .collect(Collectors.toList());

                return new DashboardTrendsDto(points, teamStats);
        }

        public DashboardEncadrantDto getDashboardEncadrant(Long encadrantId) {
                DashboardEncadrantDto dto = new DashboardEncadrantDto();
                List<Stagiaire> stagiaires = stagiaireRepository.findByEncadrantId(encadrantId);
                dto.setTotalStagiaires(stagiaires.size());
                dto.setStagiairesActifs((int) stagiaires.stream()
                                .filter(s -> s.getStatut() == StatutStagiaire.ACTIF).count());
                dto.setStagiairesEnRetard((int) stagiaires.stream()
                                .filter(s -> s.getStatut() == StatutStagiaire.EN_RETARD).count());
                dto.setStagiairesTermines((int) stagiaires.stream()
                                .filter(s -> s.getStatut() == StatutStagiaire.TERMINE).count());
                dto.setStagiairesArchives((int) stagiaires.stream()
                                .filter(s -> s.getStatut() == StatutStagiaire.ARCHIVE).count());
                dto.setEvaluationsEnAttente((int) evaluationRepository.findPendingByEncadrant(encadrantId,
                                org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
                dto.setLivrablesEnAttente((int) livrableRepository.findPendingByEncadrant(encadrantId,
                                org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
                return dto;
        }

        public DashboardStagiaireDto getDashboardStagiaire(Long stagiaireId) {
                Stagiaire stagiaire = stagiaireRepository.findById(stagiaireId)
                                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire", stagiaireId));
                DashboardStagiaireDto dto = new DashboardStagiaireDto();
                dto.setStagiaireId(stagiaireId);
                dto.setStatut(stagiaire.getStatut().name());
                dto.setTauxAssiduite(absenceService.calculerTauxAssiduite(stagiaireId));
                dto.setTachesTotal((int) tacheRepository.countByEtat(stagiaireId, EtatTache.A_FAIRE)
                                + (int) tacheRepository.countByEtat(stagiaireId, EtatTache.EN_COURS)
                                + (int) tacheRepository.countByEtat(stagiaireId, EtatTache.TERMINE)
                                + (int) tacheRepository.countByEtat(stagiaireId, EtatTache.EN_RETARD));
                dto.setTachesTerminees((int) tacheRepository.countByEtat(stagiaireId, EtatTache.TERMINE));
                dto.setTachesEnRetard((int) tacheRepository.countByEtat(stagiaireId, EtatTache.EN_RETARD));
                dto.setNotificationsNonLues((int) notificationRepository.countUnread(stagiaireId));
                double avgNote = evaluationRepository.findAverageNoteByStaigaire(stagiaireId).orElse(0.0);
                dto.setNoteMoyenne(avgNote);
                return dto;
        }
}
