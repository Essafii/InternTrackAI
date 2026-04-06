package com.internship.platform.controller;

import com.internship.platform.dto.livrable.LivrableDto;
import com.internship.platform.dto.livrable.LivrableValidationRequest;
import com.internship.platform.security.CustomUserDetails;
import com.internship.platform.service.LivrableService;
import com.internship.platform.service.StagiaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * RBAC: ENCADRANT peut uniquement valider les livrables de SES stagiaires.
 */
@RestController
@RequestMapping("/livrables")
@RequiredArgsConstructor
@Tag(name = "Livrables")
public class LivrableController {

    private final LivrableService livrableService;
    private final StagiaireService stagiaireService;

    @PostMapping(value = "/stagiaire/{stagiaireId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('STAGIAIRE', 'ENCADRANT', 'RH', 'ADMIN')")
    @Operation(summary = "Soumettre un livrable (avec upload fichier)")
    public ResponseEntity<LivrableDto> soumettre(
            @PathVariable Long stagiaireId,
            @RequestParam String titre,
            @RequestParam(required = false) String description,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal)) {
            stagiaireService.assertEncadrantOwns(stagiaireId, principal.getUser().getId());
        } else if (isStagiaire(principal)) {
            stagiaireService.assertStagiaireOwns(stagiaireId, principal.getUser().getId());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(livrableService.soumettreLivrable(stagiaireId, titre, description, file));
    }

    @GetMapping("/stagiaire/{stagiaireId}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT', 'STAGIAIRE')")
    @Operation(summary = "Livrables d'un stagiaire")
    public ResponseEntity<Page<LivrableDto>> getByStagiaire(
            @PathVariable Long stagiaireId,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal)) {
            stagiaireService.assertEncadrantOwns(stagiaireId, principal.getUser().getId());
        } else if (isStagiaire(principal)) {
            stagiaireService.assertStagiaireOwns(stagiaireId, principal.getUser().getId());
        }
        return ResponseEntity.ok(livrableService.getLivrablesByStagiaire(stagiaireId, pageable));
    }

    @GetMapping("/encadrant/{encadrantId}/pending")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Livrables en attente de validation pour un encadrant")
    public ResponseEntity<Page<LivrableDto>> getPendingByEncadrant(
            @PathVariable Long encadrantId,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {

        // ENCADRANT: can only view their OWN pending livrables
        if (isEncadrant(principal) && !principal.getUser().getId().equals(encadrantId)) {
            throw new AccessDeniedException("Vous ne pouvez consulter que vos propres livrables en attente.");
        }
        return ResponseEntity.ok(livrableService.getPendingByEncadrant(encadrantId, pageable));
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasRole('ENCADRANT')")
    @Operation(summary = "Valider/rejeter un livrable (encadrant du stagiaire uniquement)")
    public ResponseEntity<LivrableDto> valider(
            @PathVariable Long id,
            @Valid @RequestBody LivrableValidationRequest request,
            Authentication auth,
            @AuthenticationPrincipal CustomUserDetails principal) {

        LivrableDto livrable = livrableService.getLivrableById(id);
        stagiaireService.assertEncadrantOwns(livrable.getStagiaireId(), principal.getUser().getId());
        return ResponseEntity.ok(livrableService.validerLivrable(id, request, auth.getName()));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT', 'STAGIAIRE')")
    @Operation(summary = "Télécharger le fichier d'un livrable")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        LivrableDto livrable = livrableService.getLivrableById(id);
        if (isEncadrant(principal)) {
            stagiaireService.assertEncadrantOwns(livrable.getStagiaireId(), principal.getUser().getId());
        } else if (isStagiaire(principal)) {
            stagiaireService.assertStagiaireOwns(livrable.getStagiaireId(), principal.getUser().getId());
        }
        Resource resource = livrableService.loadLivrableResource(id);
        String filename = livrableService.getLivrableFileName(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    private boolean isEncadrant(CustomUserDetails p) {
        return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ENCADRANT"));
    }

    private boolean isStagiaire(CustomUserDetails p) {
        return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAGIAIRE"));
    }
}
