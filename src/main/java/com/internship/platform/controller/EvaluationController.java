package com.internship.platform.controller;

import com.internship.platform.dto.evaluation.EvaluationDto;
import com.internship.platform.dto.evaluation.EvaluationRequest;
import com.internship.platform.security.CustomUserDetails;
import com.internship.platform.service.EvaluationService;
import com.internship.platform.service.StagiaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * RBAC: ENCADRANT peut uniquement évaluer SES stagiaires.
 */
@RestController
@RequestMapping("/evaluations")
@RequiredArgsConstructor
@Tag(name = "Évaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final StagiaireService stagiaireService;

    @PostMapping("/stagiaire/{stagiaireId}")
    @PreAuthorize("hasAnyRole('ENCADRANT', 'RH', 'ADMIN')")
    @Operation(summary = "Créer une évaluation")
    public ResponseEntity<EvaluationDto> creer(
            @PathVariable Long stagiaireId,
            @Valid @RequestBody EvaluationRequest request,
            Authentication auth,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal)) {
            stagiaireService.assertEncadrantOwns(stagiaireId, principal.getUser().getId());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(evaluationService.creerEvaluation(stagiaireId, request, auth.getName()));
    }

    @GetMapping("/stagiaire/{stagiaireId}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT', 'STAGIAIRE')")
    @Operation(summary = "Évaluations d'un stagiaire")
    public ResponseEntity<Page<EvaluationDto>> getByStagiaire(
            @PathVariable Long stagiaireId,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal)) {
            stagiaireService.assertEncadrantOwns(stagiaireId, principal.getUser().getId());
        } else if (isStagiaire(principal)) {
            stagiaireService.assertStagiaireOwns(stagiaireId, principal.getUser().getId());
        }
        return ResponseEntity.ok(evaluationService.getEvaluationsByStagiaire(stagiaireId, pageable));
    }

    @GetMapping("/encadrant/{encadrantId}/pending")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Évaluations en attente de validation pour un encadrant")
    public ResponseEntity<Page<EvaluationDto>> getPendingByEncadrant(
            @PathVariable Long encadrantId,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal) && !principal.getUser().getId().equals(encadrantId)) {
            throw new AccessDeniedException("Vous ne pouvez consulter que vos propres évaluations en attente.");
        }
        return ResponseEntity.ok(evaluationService.getPendingByEncadrant(encadrantId, pageable));
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('ENCADRANT', 'RH', 'ADMIN')")
    @Operation(summary = "Valider une évaluation")
    public ResponseEntity<EvaluationDto> valider(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal)) {
            EvaluationDto eval = evaluationService.getEvaluationById(id);
            stagiaireService.assertEncadrantOwns(eval.getStagiaireId(), principal.getUser().getId());
        }
        return ResponseEntity.ok(evaluationService.validerEvaluation(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ENCADRANT', 'RH', 'ADMIN')")
    @Operation(summary = "Modifier une évaluation (justification requise si validée)")
    public ResponseEntity<EvaluationDto> modifier(
            @PathVariable Long id,
            @Valid @RequestBody EvaluationRequest request,
            @RequestParam(required = false) String justification,
            Authentication auth,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal)) {
            EvaluationDto eval = evaluationService.getEvaluationById(id);
            stagiaireService.assertEncadrantOwns(eval.getStagiaireId(), principal.getUser().getId());
        }
        return ResponseEntity.ok(evaluationService.modifierEvaluation(id, request, justification, auth.getName()));
    }

    private boolean isEncadrant(CustomUserDetails p) {
        return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ENCADRANT"));
    }

    private boolean isStagiaire(CustomUserDetails p) {
        return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAGIAIRE"));
    }
}
