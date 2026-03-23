package com.internship.platform.service;

import com.internship.platform.dto.absence.AbsenceDto;
import com.internship.platform.dto.absence.AbsenceRequest;
import com.internship.platform.entity.Absence;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.enums.TypeNotification;
import com.internship.platform.exception.BusinessException;
import com.internship.platform.exception.ResourceNotFoundException;
import com.internship.platform.repository.AbsenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final StagiaireService stagiaireService;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Value("${app.notifications.absence-threshold:80}")
    private int absenceThreshold;

    @Transactional
    public AbsenceDto enregistrerAbsence(Long stagiaireId, AbsenceRequest request) {
        Stagiaire stagiaire = stagiaireService.findById(stagiaireId);
        if (absenceRepository.existsByStagiaireIdAndDateAbsence(stagiaireId, request.getDateAbsence())) {
            throw new BusinessException("Une absence est déjà enregistrée pour cette date");
        }
        Absence absence = Absence.builder()
                .stagiaire(stagiaire)
                .dateAbsence(request.getDateAbsence())
                .type(request.getType())
                .motif(request.getMotif())
                .justifiee(request.isJustifiee())
                .build();
        absenceRepository.save(absence);

        // Check assiduité threshold
        double taux = calculerTauxAssiduite(stagiaireId);
        if (taux < absenceThreshold) {
            notificationService.createNotification(
                    stagiaire.getEncadrant(),
                    "Alerte assiduité",
                    "Le taux d'assiduité de " + stagiaire.getUser().getFullName() + " est de "
                            + String.format("%.1f", taux) + "%",
                    TypeNotification.ALERTE_ASSIDUITE,
                    stagiaireId, "Stagiaire");
        }
        auditService.log("ABSENCE_ENREGISTREE", "Absence", absence.getId(), "Date: " + request.getDateAbsence());
        return toDto(absence, taux);
    }

    @Transactional
    public AbsenceDto uploadJustificatif(Long absenceId, MultipartFile file) {
        Absence absence = findById(absenceId);
        String path = fileStorageService.store(file, "justificatifs");
        absence.setJustificatifPath(path);
        absence.setJustificatifNom(file.getOriginalFilename());
        absence.setJustifiee(true);
        absenceRepository.save(absence);
        return toDto(absence, calculerTauxAssiduite(absence.getStagiaire().getId()));
    }

    @Transactional
    public AbsenceDto validerAbsence(Long absenceId, String commentaire) {
        Absence absence = findById(absenceId);
        absence.setValidee(true);
        absence.setCommentaireEncadrant(commentaire);
        absenceRepository.save(absence);
        return toDto(absence, calculerTauxAssiduite(absence.getStagiaire().getId()));
    }

    @Transactional(readOnly = true)
    public Page<AbsenceDto> getAbsencesByStagiaire(Long stagiaireId, Pageable pageable) {
        double taux = calculerTauxAssiduite(stagiaireId);
        return absenceRepository.findByStagiaireId(stagiaireId, pageable)
                .map(a -> toDto(a, taux));
    }

    @Transactional(readOnly = true)
    public double calculerTauxAssiduite(Long stagiaireId) {
        Stagiaire stagiaire = stagiaireService.findById(stagiaireId);
        LocalDate debut = stagiaire.getDateDebut();
        LocalDate fin = LocalDate.now().isBefore(stagiaire.getDateFin()) ? LocalDate.now() : stagiaire.getDateFin();
        long totalJours = ChronoUnit.DAYS.between(debut, fin) + 1;
        if (totalJours <= 0)
            return 100.0;
        long absences = absenceRepository.countAbsencesByPeriod(stagiaireId, debut, fin);
        return Math.max(0, ((totalJours - absences) / (double) totalJours) * 100);
    }

    @Transactional(readOnly = true)
    public Absence findById(Long id) {
        return absenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Absence", id));
    }

    @Transactional(readOnly = true)
    public AbsenceDto getAbsenceById(Long id) {
        Absence a = findById(id);
        return toDto(a, calculerTauxAssiduite(a.getStagiaire().getId()));
    }

    private AbsenceDto toDto(Absence a, double taux) {
        AbsenceDto dto = new AbsenceDto();
        dto.setId(a.getId());
        dto.setStagiaireId(a.getStagiaire().getId());
        dto.setStagiaireNom(a.getStagiaire().getUser().getFullName());
        dto.setDateAbsence(a.getDateAbsence());
        dto.setType(a.getType().name());
        dto.setMotif(a.getMotif());
        dto.setJustifiee(a.isJustifiee());
        dto.setValidee(a.isValidee());
        dto.setJustificatifNom(a.getJustificatifNom());
        dto.setTauxAssiduite(taux);
        return dto;
    }
}
