package com.internship.platform.service;

import com.internship.platform.dto.livrable.LivrableDto;
import com.internship.platform.dto.livrable.LivrableValidationRequest;
import com.internship.platform.entity.Livrable;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.enums.StatutLivrable;
import com.internship.platform.entity.enums.TypeNotification;
import com.internship.platform.exception.BusinessException;
import com.internship.platform.exception.ResourceNotFoundException;
import com.internship.platform.repository.LivrableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LivrableService {

    private final LivrableRepository livrableRepository;
    private final StagiaireService stagiaireService;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final EmailService emailService;

    @Transactional
    public LivrableDto soumettreLivrable(Long stagiaireId, String titre, String description, MultipartFile file) {
        Stagiaire stagiaire = stagiaireService.findById(stagiaireId);

        // Determine version
        Integer maxVersion = livrableRepository.findMaxVersionByTitre(stagiaireId, titre);
        int version = (maxVersion != null ? maxVersion : 0) + 1;

        String filePath = fileStorageService.store(file, "livrables");

        Livrable livrable = Livrable.builder()
                .stagiaire(stagiaire)
                .titre(titre)
                .description(description)
                .filePath(filePath)
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .version(version)
                .statut(StatutLivrable.SOUMIS)
                .build();

        // AI pre-evaluation (simplified)
        performAiEvaluation(livrable, file);

        livrableRepository.save(livrable);

        // Notify encadrant
        if (stagiaire.getEncadrant() != null) {
            notificationService.createNotification(
                    stagiaire.getEncadrant(),
                    "Nouveau livrable soumis",
                    stagiaire.getUser().getFullName() + " a soumis le livrable: " + titre + " (v" + version + ")",
                    TypeNotification.RETARD_LIVRABLE,
                    livrable.getId(), "Livrable");
        }
        auditService.log("LIVRABLE_SOUMIS", "Livrable", livrable.getId(), "Titre: " + titre + " v" + version);
        return toDto(livrable);
    }

    @Transactional
    public LivrableDto validerLivrable(Long id, LivrableValidationRequest request, String encadrantEmail) {
        Livrable livrable = findById(id);
        // Verify encadrant is the stagiaire's encadrant
        if (livrable.getStagiaire().getEncadrant() == null ||
                !livrable.getStagiaire().getEncadrant().getEmail().equals(encadrantEmail)) {
            throw new BusinessException("Seul l'encadrant du stagiaire peut valider ce livrable");
        }
        livrable.setStatut(request.getStatut());
        livrable.setCommentaireEncadrant(request.getCommentaire());
        livrable.setDateValidation(LocalDateTime.now());
        livrableRepository.save(livrable);

        TypeNotification notifType = request.getStatut() == StatutLivrable.VALIDE
                ? TypeNotification.VALIDATION_LIVRABLE
                : TypeNotification.REJET_LIVRABLE;
        String notifTitre = request.getStatut() == StatutLivrable.VALIDE
                ? "Livrable validé"
                : "Livrable rejeté";
        String notifMsg = request.getStatut() == StatutLivrable.VALIDE
                ? "Votre livrable '" + livrable.getTitre() + "' a été validé"
                : "Votre livrable '" + livrable.getTitre() + "' a été rejeté/correction demandée";

        notificationService.createNotification(
                livrable.getStagiaire().getUser(), notifTitre, notifMsg,
                notifType, livrable.getId(), "Livrable");
        auditService.log("LIVRABLE_VALIDE", "Livrable", id, "Statut: " + request.getStatut());
        // Fire real email notification to the stagiaire
        String stagiEmail = livrable.getStagiaire().getUser().getEmail();
        if (stagiEmail != null) {
            emailService.sendLivrableStatusEmail(
                    stagiEmail,
                    livrable.getStagiaire().getUser().getFullName(),
                    livrable.getTitre(),
                    request.getStatut().name(),
                    request.getCommentaire());
        }
        return toDto(livrable);
    }

    public Page<LivrableDto> getLivrablesByStagiaire(Long stagiaireId, Pageable pageable) {
        return livrableRepository.findByStagiaireId(stagiaireId, pageable).map(this::toDto);
    }

    public Page<LivrableDto> getPendingByEncadrant(Long encadrantId, Pageable pageable) {
        return livrableRepository.findPendingByEncadrant(encadrantId, pageable).map(this::toDto);
    }

    private void performAiEvaluation(Livrable livrable, MultipartFile file) {
        // Simplified AI evaluation based on file size and name heuristics
        double score = 10.0;
        String feedback = "Livrable reçu. ";
        String niveau = "MOYEN";

        long sizeKb = file.getSize() / 1024;
        if (sizeKb > 100) {
            score += 2;
            feedback += "Document substantiel. ";
        }
        if (sizeKb > 500) {
            score += 2;
            feedback += "Document complet. ";
            niveau = "BON";
        }
        if (sizeKb > 1000) {
            score = Math.min(score + 2, 20);
            niveau = "EXCELLENT";
        }

        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (name.endsWith(".pdf")) {
            score += 1;
            feedback += "Format PDF recommandé. ";
        }
        if (name.contains("rapport") || name.contains("report")) {
            score += 1;
            feedback += "Rapport structuré détecté. ";
        }

        score = Math.min(score, 20.0);
        livrable.setScoreIA(score);
        livrable.setFeedbackIA(feedback.trim());
        livrable.setNiveauQualiteIA(niveau);

        if (score < 10) {
            livrable.setNiveauQualiteIA("INSUFFISANT");
        }
    }

    public Resource loadLivrableResource(Long id) {
        Livrable livrable = findById(id);
        if (livrable.getFilePath() == null) {
            throw new BusinessException("Ce livrable n'a pas de fichier associé");
        }
        Path file = fileStorageService.load(livrable.getFilePath());
        try {
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new BusinessException("Impossible de lire le fichier: " + livrable.getFileName());
            }
        } catch (MalformedURLException e) {
            throw new BusinessException("Erreur lors de la lecture du fichier: " + e.getMessage());
        }
    }

    public Livrable findById(Long id) {
        return livrableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livrable", id));
    }

    public LivrableDto getLivrableById(Long id) {
        return toDto(findById(id));
    }

    public LivrableDto toDto(Livrable l) {
        LivrableDto dto = new LivrableDto();
        dto.setId(l.getId());
        dto.setStagiaireId(l.getStagiaire().getId());
        dto.setStagiaireNom(l.getStagiaire().getUser().getFullName());
        dto.setTitre(l.getTitre());
        dto.setDescription(l.getDescription());
        dto.setFileName(l.getFileName());
        dto.setFileType(l.getFileType());
        dto.setFileSize(l.getFileSize());
        dto.setVersion(l.getVersion());
        dto.setStatut(l.getStatut().name());
        dto.setCommentaireEncadrant(l.getCommentaireEncadrant());
        dto.setDateValidation(l.getDateValidation());
        dto.setScoreIA(l.getScoreIA());
        dto.setFeedbackIA(l.getFeedbackIA());
        dto.setNiveauQualiteIA(l.getNiveauQualiteIA());
        return dto;
    }

    public String getLivrableFileName(Long id) {
        Livrable livrable = findById(id);
        return livrable.getFileName();
    }
}
