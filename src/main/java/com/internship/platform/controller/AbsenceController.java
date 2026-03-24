package com.internship.platform.controller;

import com.internship.platform.dto.absence.AbsenceDto;
import com.internship.platform.dto.absence.AbsenceRequest;
import com.internship.platform.security.CustomUserDetails;
import com.internship.platform.service.AbsenceService;
import com.internship.platform.service.StagiaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * RBAC: ENCADRANT peut uniquement gérer les absences de SES stagiaires.
 */
@RestController
@RequestMapping("/absences")
@RequiredArgsConstructor
@Tag(name = "Absences")
public class AbsenceController {

    private final AbsenceService absenceService;
    private final StagiaireService stagiaireService;

    @PostMapping("/stagiaire/{stagiaireId}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Enregistrer une absence")
    public ResponseEntity<AbsenceDto> enregistrer(
            @PathVariable Long stagiaireId,
            @Valid @RequestBody AbsenceRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal)) {
            stagiaireService.assertEncadrantOwns(stagiaireId, principal.getUser().getId());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(absenceService.enregistrerAbsence(stagiaireId, request));
    }

    @PostMapping(value = "/{id}/justificatif", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT', 'STAGIAIRE')")
    @Operation(summary = "Uploader un justificatif d'absence")
    public ResponseEntity<AbsenceDto> uploadJustificatif(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        // Justificatif upload: accessible à tous les rôles autorisés
        // (le stagiaire soumet pour sa propre absence, l'encadrant valide)
        return ResponseEntity.ok(absenceService.uploadJustificatif(id, file));
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Valider une absence")
    public ResponseEntity<AbsenceDto> valider(
            @PathVariable Long id,
            @RequestParam(required = false) String commentaire,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal)) {
            AbsenceDto absence = absenceService.getAbsenceById(id);
            stagiaireService.assertEncadrantOwns(absence.getStagiaireId(), principal.getUser().getId());
        }
        return ResponseEntity.ok(absenceService.validerAbsence(id, commentaire));
    }

    @GetMapping("/stagiaire/{stagiaireId}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT', 'STAGIAIRE')")
    @Operation(summary = "Absences d'un stagiaire")
    public ResponseEntity<Page<AbsenceDto>> getByStagiaire(
            @PathVariable Long stagiaireId,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal)) {
            stagiaireService.assertEncadrantOwns(stagiaireId, principal.getUser().getId());
        } else if (isStagiaire(principal)) {
            stagiaireService.assertStagiaireOwns(stagiaireId, principal.getUser().getId());
        }
        return ResponseEntity.ok(absenceService.getAbsencesByStagiaire(stagiaireId, pageable));
    }

    @GetMapping("/stagiaire/{stagiaireId}/assiduite")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT', 'STAGIAIRE')")
    @Operation(summary = "Taux d'assiduité d'un stagiaire")
    public ResponseEntity<Double> getAssiduite(
            @PathVariable Long stagiaireId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal)) {
            stagiaireService.assertEncadrantOwns(stagiaireId, principal.getUser().getId());
        } else if (isStagiaire(principal)) {
            stagiaireService.assertStagiaireOwns(stagiaireId, principal.getUser().getId());
        }
        return ResponseEntity.ok(absenceService.calculerTauxAssiduite(stagiaireId));
    }

    private boolean isEncadrant(CustomUserDetails p) {
        return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ENCADRANT"));
    }

    private boolean isStagiaire(CustomUserDetails p) {
        return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAGIAIRE"));
    }
}
