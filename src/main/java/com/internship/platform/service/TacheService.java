package com.internship.platform.service;

import com.internship.platform.dto.tache.TacheDto;
import com.internship.platform.dto.tache.TacheRequest;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.Tache;
import com.internship.platform.entity.enums.EtatTache;
import com.internship.platform.entity.enums.TypeNotification;
import com.internship.platform.exception.ResourceNotFoundException;
import com.internship.platform.repository.TacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TacheService {

    private final TacheRepository tacheRepository;
    private final StagiaireService stagiaireService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final EmailService emailService;

    @Transactional
    public TacheDto creerTache(Long stagiaireId, TacheRequest request) {
        Stagiaire stagiaire = stagiaireService.findById(stagiaireId);
        Tache tache = Tache.builder()
                .stagiaire(stagiaire)
                .titre(request.getTitre())
                .description(request.getDescription())
                .dateDebut(request.getDateDebut() != null ? request.getDateDebut() : LocalDate.now())
                .deadline(request.getDeadline())
                .priorite(request.getPriorite())
                .etat(EtatTache.A_FAIRE)
                .build();
        tacheRepository.save(tache);
        auditService.log("TACHE_CREEE", "Tache", tache.getId(), "Titre: " + request.getTitre());
        return toDto(tache);
    }

    @Transactional
    public TacheDto updateTache(Long id, TacheRequest request) {
        Tache tache = findById(id);
        if (request.getTitre() != null)
            tache.setTitre(request.getTitre());
        if (request.getDescription() != null)
            tache.setDescription(request.getDescription());
        if (request.getDeadline() != null)
            tache.setDeadline(request.getDeadline());
        if (request.getPriorite() != null)
            tache.setPriorite(request.getPriorite());
        if (request.getEtat() != null) {
            tache.setEtat(request.getEtat());
            if (request.getEtat() == EtatTache.TERMINE) {
                tache.setDateCompletion(LocalDate.now());
            }
        }
        if (request.getCommentaire() != null)
            tache.setCommentaire(request.getCommentaire());
        auditService.log("TACHE_MODIFIEE", "Tache", id, "Nouvel état: " + tache.getEtat());
        return toDto(tacheRepository.save(tache));
    }

    @Transactional(readOnly = true)
    public Page<TacheDto> getTachesByStagiaire(Long stagiaireId, Pageable pageable) {
        return tacheRepository.findByStagiaireId(stagiaireId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public TacheDto getTacheById(Long id) {
        return toDto(findById(id));
    }

    @Transactional
    public void deleteTache(Long id) {
        tacheRepository.deleteById(id);
    }

    // Scheduled: check overdue tasks daily
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkOverdueTaches() {
        List<Tache> overdue = tacheRepository.findOverdueTachesForAlert(LocalDate.now());
        for (Tache tache : overdue) {
            tache.setEtat(EtatTache.EN_RETARD);
            tache.setAlerteEnvoyee(true);
            tacheRepository.save(tache);
            notificationService.createNotification(
                    tache.getStagiaire().getUser(),
                    "Tâche en retard",
                    "La tâche '" + tache.getTitre() + "' est en retard (deadline: " + tache.getDeadline() + ")",
                    TypeNotification.RETARD_TACHE,
                    tache.getId(), "Tache");
            // Fire real email notification
            if (tache.getStagiaire().getUser().getEmail() != null) {
                emailService.sendLateTaskEmail(
                        tache.getStagiaire().getUser().getEmail(),
                        tache.getStagiaire().getUser().getFullName(),
                        tache.getTitre(),
                        tache.getDeadline().toString());
            }
            if (tache.getStagiaire().getEncadrant() != null) {
                notificationService.createNotification(
                        tache.getStagiaire().getEncadrant(),
                        "Tâche stagiaire en retard",
                        tache.getStagiaire().getUser().getFullName() + " a une tâche en retard: " + tache.getTitre(),
                        TypeNotification.RETARD_TACHE,
                        tache.getId(), "Tache");
            }
        }
    }

    @Transactional(readOnly = true)
    public Tache findById(Long id) {
        return tacheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tache", id));
    }

    public TacheDto toDto(Tache t) {
        TacheDto dto = new TacheDto();
        dto.setId(t.getId());
        dto.setStagiaireId(t.getStagiaire().getId());
        dto.setStagiaireNom(t.getStagiaire().getUser().getFullName());
        dto.setTitre(t.getTitre());
        dto.setDescription(t.getDescription());
        dto.setEtat(t.getEtat().name());
        dto.setDateDebut(t.getDateDebut());
        dto.setDeadline(t.getDeadline());
        dto.setDateCompletion(t.getDateCompletion());
        dto.setPriorite(t.getPriorite());
        dto.setCommentaire(t.getCommentaire());
        return dto;
    }
}
