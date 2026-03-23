package com.internship.platform.controller;

import com.internship.platform.dto.stagiaire.OnboardingRequest;
import com.internship.platform.dto.stagiaire.StagiaireDto;
import com.internship.platform.dto.stagiaire.StagiaireUpdateRequest;
import com.internship.platform.entity.enums.StatutStagiaire;
import com.internship.platform.repository.ActionLogRepository;
import com.internship.platform.security.CustomUserDetails;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Stagiaire resources.
 *
 * RBAC rules enforced at the method level (server-side, not just frontend):
 * - RH / ADMIN : access to ALL stagiaires.
 * - ENCADRANT : access ONLY to their own stagiaires (encadrant_id == caller's
 * user id).
 * - STAGIAIRE : access only to their own profile.
 */
@RestController
@RequestMapping("/stagiaires")
@RequiredArgsConstructor
@Tag(name = "Stagiaires")
public class StagiaireController {

    private final StagiaireService stagiaireService;
    private final ActionLogRepository actionLogRepository;

    // ── Write (RH/ADMIN only) ──────────────────────────────────────────────────

    @PostMapping("/onboarding")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Onboarder un nouveau stagiaire")
    public ResponseEntity<StagiaireDto> onboard(@Valid @RequestBody OnboardingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stagiaireService.onboardStagiaire(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Modifier un stagiaire")
    public ResponseEntity<StagiaireDto> update(
            @PathVariable Long id,
            @RequestBody StagiaireUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        // ENCADRANT: can only update stagiaires they supervise
        if (isEncadrant(principal)) {
            stagiaireService.assertEncadrantOwns(id, principal.getUser().getId());
        }
        return ResponseEntity.ok(stagiaireService.updateStagiaire(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Archiver un stagiaire")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        stagiaireService.deleteStagiaire(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/resend-email")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Renvoyer l'email de bienvenue à un stagiaire")
    public ResponseEntity<Void> resendWelcomeEmail(@PathVariable Long id) {
        stagiaireService.resendWelcomeEmail(id, null);
        return ResponseEntity.ok().build();
    }

    // ── Read – list endpoints ──────────────────────────────────────────────────

    /**
     * GET /stagiaires
     *
     * - RH/ADMIN : returns ALL stagiaires (paginated).
     * - ENCADRANT: automatically returns ONLY their stagiaires (same as
     * /stagiaires/encadrant/{their-id}) — no global list exposed.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Lister les stagiaires (filtrés par rôle)")
    public ResponseEntity<Page<StagiaireDto>> getAll(
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal)) {
            // ENCADRANT: silently restrict to their own team
            return ResponseEntity.ok(
                    stagiaireService.getStagiairesByEncadrant(principal.getUser().getId(), pageable));
        }
        return ResponseEntity.ok(stagiaireService.getAllStagiaires(pageable));
    }

    /**
     * GET /stagiaires/encadrant/{encadrantId}
     *
     * ENCADRANT may only query their OWN id — querying another encadrant's id
     * returns 403. RH/ADMIN can query any id.
     */
    @GetMapping("/encadrant/{encadrantId}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Stagiaires d'un encadrant")
    public ResponseEntity<Page<StagiaireDto>> getByEncadrant(
            @PathVariable Long encadrantId,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal) && !principal.getUser().getId().equals(encadrantId)) {
            throw new AccessDeniedException(
                    "Un encadrant ne peut consulter que ses propres stagiaires.");
        }
        return ResponseEntity.ok(stagiaireService.getStagiairesByEncadrant(encadrantId, pageable));
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Filtrer les stagiaires par statut")
    public ResponseEntity<Page<StagiaireDto>> getByStatut(
            @PathVariable StatutStagiaire statut, Pageable pageable) {
        return ResponseEntity.ok(stagiaireService.getStagiairesByStatut(statut, pageable));
    }

    // ── Read – single resource ─────────────────────────────────────────────────

    /**
     * GET /stagiaires/{id}
     *
     * ENCADRANT: only if the stagiaire is in their team.
     * STAGIAIRE: only their own profile (checked by ownership of userId).
     * RH/ADMIN: unrestricted.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT', 'STAGIAIRE')")
    @Operation(summary = "Obtenir un stagiaire par ID")
    public ResponseEntity<StagiaireDto> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (isEncadrant(principal)) {
            stagiaireService.assertEncadrantOwns(id, principal.getUser().getId());
        } else if (isStagiaire(principal)) {
            // A stagiaire can only fetch their own record
            stagiaireService.assertStagiaireOwns(id, principal.getUser().getId());
        }
        return ResponseEntity.ok(stagiaireService.getStagiaireById(id));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT', 'STAGIAIRE')")
    @Operation(summary = "Obtenir le profil stagiaire par userId")
    public ResponseEntity<StagiaireDto> getByUserId(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        // A STAGIAIRE can only fetch their OWN profile by userId
        if (isStagiaire(principal) && !principal.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Accès refusé : vous ne pouvez consulter que votre propre profil.");
        }
        return ResponseEntity.ok(stagiaireService.getStagiaireByUserId(userId));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Historique des actions d'un stagiaire")
    public ResponseEntity<?> getAuditLog(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(
                actionLogRepository.findByEntiteTypeAndEntiteIdOrderByTimestampDesc("Stagiaire", id, pageable));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isEncadrant(CustomUserDetails p) {
        return p.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ENCADRANT"));
    }

    private boolean isStagiaire(CustomUserDetails p) {
        return p.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAGIAIRE"));
    }
}
