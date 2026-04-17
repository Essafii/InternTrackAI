package com.internship.platform.service;

import com.internship.platform.dto.livrable.LivrableDto;
import com.internship.platform.dto.livrable.LivrableValidationRequest;
import com.internship.platform.entity.Livrable;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.Tache;
import com.internship.platform.entity.enums.EtatTache;
import com.internship.platform.entity.enums.StatutLivrable;
import com.internship.platform.entity.enums.TypeNotification;
import com.internship.platform.exception.BusinessException;
import com.internship.platform.exception.ResourceNotFoundException;
import com.internship.platform.repository.EvaluationRepository;
import com.internship.platform.repository.LivrableRepository;
import com.internship.platform.repository.TacheRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LivrableService {

    private final LivrableRepository livrableRepository;
    private final StagiaireService stagiaireService;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final EmailService emailService;
    private final AbsenceService absenceService;
    private final EvaluationRepository evaluationRepository;
    private final TacheRepository tacheRepository;

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
        Long id = livrable.getStagiaire().getId();

        double noteMoyenne = evaluationRepository.findAverageNoteByStaigaire(id).orElse(10.0);
        double tauxAssiduite = absenceService.calculerTauxAssiduite(id);

        List<Tache> taches = tacheRepository.findByStagiaireId(id);
        long total = taches.size();
        long terminees = tacheRepository.countByEtat(id, EtatTache.TERMINE);
        long overdues = taches.stream()
                .filter(t -> t.getEtat() != EtatTache.TERMINE && t.getDeadline().isBefore(LocalDate.now()))
                .count();

        double tauxTaches = total > 0 ? (terminees * 100.0 / total) : 100.0;
        double tauxRetard = total > 0 ? (overdues * 100.0 / total) : 0.0;
        double risqueIA = Math.min(1.0,
                (tauxAssiduite < 80 ? 0.5 : 0.0) + (tauxRetard > 30 ? 0.5 : 0.0));

        double score100 = 0.40 * (noteMoyenne * 5.0)
                + 0.25 * tauxAssiduite
                + 0.25 * tauxTaches
                + 0.10 * (1.0 - risqueIA) * 100.0;
        double scoreIA = Math.min(score100 / 5.0, 20.0);

        String niveau;
        if (scoreIA >= 17) niveau = "EXCELLENT";
        else if (scoreIA >= 14) niveau = "BON";
        else if (scoreIA >= 10) niveau = "MOYEN";
        else niveau = "INSUFFISANT";

        String feedback = String.format(
                "Score global: %.1f/20 | Note moy: %.1f/20 | Assiduité: %.0f%% | Tâches: %.0f%% | Risque: %.1f",
                scoreIA, noteMoyenne, tauxAssiduite, tauxTaches, risqueIA);

        livrable.setScoreIA(scoreIA);
        livrable.setFeedbackIA(feedback);
        livrable.setNiveauQualiteIA(niveau);
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
