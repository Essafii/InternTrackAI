package com.internship.platform.service;

import com.internship.platform.dto.evaluation.EvaluationDto;
import com.internship.platform.dto.evaluation.EvaluationRequest;
import com.internship.platform.entity.Evaluation;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.User;
import com.internship.platform.entity.enums.TypeEvaluation;
import com.internship.platform.entity.enums.TypeNotification;
import com.internship.platform.exception.BusinessException;
import com.internship.platform.exception.ResourceNotFoundException;
import com.internship.platform.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final StagiaireService stagiaireService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final EmailService emailService;

    @Transactional
    public EvaluationDto creerEvaluation(Long stagiaireId, EvaluationRequest request, String evaluateurEmail) {
        Stagiaire stagiaire = stagiaireService.findById(stagiaireId);
        User evaluateur = userService.findByEmail(evaluateurEmail);

        // Rule: finale only if all mensuelles are validated
        if (request.getType() == TypeEvaluation.FINALE) {
            long validatedMensuelles = evaluationRepository.countValidatedMensuelles(stagiaireId);
            if (validatedMensuelles == 0) {
                throw new BusinessException("L'évaluation finale nécessite au moins une évaluation mensuelle validée");
            }
        }

        // Check for duplicate monthly evaluation
        if (request.getType() == TypeEvaluation.MENSUELLE && request.getMois() != null) {
            if (evaluationRepository
                    .findByStagiaireIdAndTypeAndMois(stagiaireId, TypeEvaluation.MENSUELLE, request.getMois())
                    .isPresent()) {
                throw new BusinessException("Une évaluation mensuelle existe déjà pour le mois " + request.getMois());
            }
        }

        double moyenne = calculerMoyenne(request);
        Evaluation evaluation = Evaluation.builder()
                .stagiaire(stagiaire)
                .evaluateur(evaluateur)
                .type(request.getType())
                .dateEvaluation(LocalDate.now())
                .mois(request.getMois())
                .noteTechnique(request.getNoteTechnique())
                .noteProgression(request.getNoteProgression())
                .noteDelais(request.getNoteDelais())
                .noteQualite(request.getNoteQualite())
                .noteAutonomie(request.getNoteAutonomie())
                .noteCommunication(request.getNoteCommunication())
                .noteMoyenne(moyenne)
                .commentaire(request.getCommentaire())
                .pointsForts(request.getPointsForts())
                .pointsAmeliorer(request.getPointsAmeliorer())
                .validee(false)
                .build();

        evaluationRepository.save(evaluation);
        notificationService.createNotification(
                stagiaire.getUser(),
                "Nouvelle évaluation",
                "Une nouvelle évaluation " + request.getType().name() + " a été créée",
                TypeNotification.NOUVELLE_EVALUATION,
                evaluation.getId(), "Evaluation");
        auditService.log("EVALUATION_CREEE", "Evaluation", evaluation.getId(), "Type: " + request.getType());
        return toDto(evaluation);
    }

    @Transactional
    public EvaluationDto validerEvaluation(Long id) {
        Evaluation evaluation = findById(id);
        evaluation.setValidee(true);
        evaluationRepository.save(evaluation);
        auditService.log("EVALUATION_VALIDEE", "Evaluation", id, "Validée");
        // Fire real email notification to the stagiaire
        String email = evaluation.getStagiaire().getUser().getEmail();
        if (email != null) {
            emailService.sendEvaluationValideeEmail(
                    email,
                    evaluation.getStagiaire().getUser().getFullName(),
                    evaluation.getType().name(),
                    evaluation.getNoteMoyenne());
        }
        return toDto(evaluation);
    }

    @Transactional
    public EvaluationDto modifierEvaluation(Long id, EvaluationRequest request, String justification,
            String modifiePar) {
        Evaluation evaluation = findById(id);
        if (evaluation.isValidee() && (justification == null || justification.isBlank())) {
            throw new BusinessException("Une justification est requise pour modifier une évaluation validée");
        }
        evaluation.setNoteTechnique(request.getNoteTechnique());
        evaluation.setNoteProgression(request.getNoteProgression());
        evaluation.setNoteDelais(request.getNoteDelais());
        evaluation.setNoteQualite(request.getNoteQualite());
        evaluation.setNoteAutonomie(request.getNoteAutonomie());
        evaluation.setNoteCommunication(request.getNoteCommunication());
        evaluation.setNoteMoyenne(calculerMoyenne(request));
        evaluation.setCommentaire(request.getCommentaire());
        evaluation.setJustificationModification(justification);
        evaluation.setModifiePar(modifiePar);
        evaluationRepository.save(evaluation);
        auditService.log("EVALUATION_MODIFIEE", "Evaluation", id,
                "Modifiée par: " + modifiePar + " - " + justification);
        return toDto(evaluation);
    }

    public Page<EvaluationDto> getEvaluationsByStagiaire(Long stagiaireId, Pageable pageable) {
        return evaluationRepository.findByStagiaireId(stagiaireId, pageable).map(this::toDto);
    }

    public Page<EvaluationDto> getPendingByEncadrant(Long encadrantId, Pageable pageable) {
        return evaluationRepository.findPendingByEncadrant(encadrantId, pageable).map(this::toDto);
    }

    private double calculerMoyenne(EvaluationRequest r) {
        return (r.getNoteTechnique() + r.getNoteProgression() + r.getNoteDelais()
                + r.getNoteQualite() + r.getNoteAutonomie() + r.getNoteCommunication()) / 6.0;
    }

    public Evaluation findById(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation", id));
    }

    public EvaluationDto getEvaluationById(Long id) {
        return toDto(findById(id));
    }

    public EvaluationDto toDto(Evaluation e) {
        EvaluationDto dto = new EvaluationDto();
        dto.setId(e.getId());
        dto.setStagiaireId(e.getStagiaire().getId());
        dto.setStagiaireNom(e.getStagiaire().getUser().getFullName());
        dto.setEvaluateurNom(e.getEvaluateur().getFullName());
        dto.setType(e.getType().name());
        dto.setDateEvaluation(e.getDateEvaluation());
        dto.setMois(e.getMois());
        dto.setNoteTechnique(e.getNoteTechnique());
        dto.setNoteProgression(e.getNoteProgression());
        dto.setNoteDelais(e.getNoteDelais());
        dto.setNoteQualite(e.getNoteQualite());
        dto.setNoteAutonomie(e.getNoteAutonomie());
        dto.setNoteCommunication(e.getNoteCommunication());
        dto.setNoteMoyenne(e.getNoteMoyenne());
        dto.setCommentaire(e.getCommentaire());
        dto.setPointsForts(e.getPointsForts());
        dto.setPointsAmeliorer(e.getPointsAmeliorer());
        dto.setValidee(e.isValidee());
        return dto;
    }
}
