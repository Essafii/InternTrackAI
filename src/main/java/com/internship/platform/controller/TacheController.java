package com.internship.platform.controller;

import com.internship.platform.dto.tache.TacheDto;
import com.internship.platform.dto.tache.TacheRequest;
import com.internship.platform.security.CustomUserDetails;
import com.internship.platform.service.StagiaireService;
import com.internship.platform.service.TacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * RBAC: ENCADRANT peut uniquement gérer les tâches de SES stagiaires.
 */
@RestController
@RequestMapping("/taches")
@RequiredArgsConstructor
@Tag(name = "Tâches")
public class TacheController {

    private final TacheService tacheService;
    private final StagiaireService stagiaireService;

    @PostMapping("/stagiaire/{stagiaireId}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Créer une tâche pour un stagiaire")
    public ResponseEntity<TacheDto> creer(
            @PathVariable Long stagiaireId,
            @Valid @RequestBody TacheRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal)) {
            stagiaireService.assertEncadrantOwns(stagiaireId, principal.getUser().getId());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tacheService.creerTache(stagiaireId, request));
    }

    @GetMapping("/stagiaire/{stagiaireId}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT', 'STAGIAIRE')")
    @Operation(summary = "Tâches d'un stagiaire")
    public ResponseEntity<Page<TacheDto>> getByStagiaire(
            @PathVariable Long stagiaireId,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal)) {
            stagiaireService.assertEncadrantOwns(stagiaireId, principal.getUser().getId());
        } else if (isStagiaire(principal)) {
            stagiaireService.assertStagiaireOwns(stagiaireId, principal.getUser().getId());
        }
        return ResponseEntity.ok(tacheService.getTachesByStagiaire(stagiaireId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT', 'STAGIAIRE')")
    @Operation(summary = "Obtenir une tâche par ID")
    public ResponseEntity<TacheDto> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        TacheDto tache = tacheService.getTacheById(id);
        if (isEncadrant(principal)) {
            stagiaireService.assertEncadrantOwns(tache.getStagiaireId(), principal.getUser().getId());
        } else if (isStagiaire(principal)) {
            stagiaireService.assertStagiaireOwns(tache.getStagiaireId(), principal.getUser().getId());
        }
        return ResponseEntity.ok(tache);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT', 'STAGIAIRE')")
    @Operation(summary = "Modifier/replanifier une tâche")
    public ResponseEntity<TacheDto> update(
            @PathVariable Long id,
            @RequestBody TacheRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        TacheDto tache = tacheService.getTacheById(id);
        if (isEncadrant(principal)) {
            stagiaireService.assertEncadrantOwns(tache.getStagiaireId(), principal.getUser().getId());
        } else if (isStagiaire(principal)) {
            stagiaireService.assertStagiaireOwns(tache.getStagiaireId(), principal.getUser().getId());
        }
        return ResponseEntity.ok(tacheService.updateTache(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Supprimer une tâche")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal)) {
            TacheDto tache = tacheService.getTacheById(id);
            stagiaireService.assertEncadrantOwns(tache.getStagiaireId(), principal.getUser().getId());
        }
        tacheService.deleteTache(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isEncadrant(CustomUserDetails p) {
        return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ENCADRANT"));
    }

    private boolean isStagiaire(CustomUserDetails p) {
        return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAGIAIRE"));
    }
}
