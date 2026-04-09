package com.internship.platform.service;

import com.internship.platform.dto.tache.TacheDto;
import com.internship.platform.dto.tache.TacheRequest;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.Tache;
import com.internship.platform.entity.enums.EtatTache;
import com.internship.platform.exception.ResourceNotFoundException;
import com.internship.platform.repository.TacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TacheService {

    private final TacheRepository tacheRepository;
    private final StagiaireService stagiaireService;
    private final AuditService auditService;

    @Transactional
    public TacheDto creerTache(Long stagiaireId, TacheRequest request) {
        Stagiaire stagiaire = stagiaireService.findById(stagiaireId);
        Tache tache = Tache.builder()
                .stagiaire(stagiaire)
                .titre(request.getTitre())
                .description(request.getDescription())
                .dateDebut(request.getDateDebut())
                .deadline(request.getDeadline())
                .priorite(request.getPriorite())
                .etat(request.getEtat() != null ? request.getEtat() : EtatTache.A_FAIRE)
                .commentaire(request.getCommentaire())
                .build();
        tacheRepository.save(tache);
        auditService.log("TACHE_CREEE", "Tache", tache.getId(), "Titre: " + tache.getTitre());
        return toDto(tache);
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
    public TacheDto updateTache(Long id, TacheRequest request) {
        Tache tache = findById(id);
        if (request.getTitre() != null) tache.setTitre(request.getTitre());
        if (request.getDescription() != null) tache.setDescription(request.getDescription());
        if (request.getDateDebut() != null) tache.setDateDebut(request.getDateDebut());
        if (request.getDeadline() != null) tache.setDeadline(request.getDeadline());
        if (request.getPriorite() != null) tache.setPriorite(request.getPriorite());
        if (request.getEtat() != null) {
            tache.setEtat(request.getEtat());
            if (request.getEtat() == EtatTache.TERMINE && tache.getDateCompletion() == null) {
                tache.setDateCompletion(LocalDate.now());
            }
        }
        if (request.getCommentaire() != null) tache.setCommentaire(request.getCommentaire());
        tacheRepository.save(tache);
        auditService.log("TACHE_MODIFIEE", "Tache", id, "Titre: " + tache.getTitre());
        return toDto(tache);
    }

    @Transactional
    public void deleteTache(Long id) {
        Tache tache = findById(id);
        tacheRepository.delete(tache);
        auditService.log("TACHE_SUPPRIMEE", "Tache", id, "Titre: " + tache.getTitre());
    }

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