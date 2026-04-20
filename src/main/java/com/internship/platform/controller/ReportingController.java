package com.internship.platform.controller;

import com.internship.platform.dto.reporting.ReportGenerateRequest;
import com.internship.platform.dto.reporting.ReportJobDto;
import com.internship.platform.entity.ReportJob;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.enums.Role;
import com.internship.platform.entity.enums.TypeRapport;
import com.internship.platform.exception.BusinessException;
import com.internship.platform.repository.StagiaireRepository;
import com.internship.platform.security.CustomUserDetails;
import com.internship.platform.service.ReportingService;
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

@RestController
@RequestMapping("/reporting")
@RequiredArgsConstructor
@Tag(name = "Reporting")
public class ReportingController {

    private final ReportingService reportingService;
    private final StagiaireRepository stagiaireRepository;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Declencher la generation d'un rapport PDF")
    public ResponseEntity<ReportJobDto> generateReport(
            @Valid @RequestBody ReportGenerateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Role role = principal.getUser().getRole();

        // ─── RAPPORT_CAMPAGNE: strictly RH and ADMIN only ─────────────────────
        if (request.getType() == TypeRapport.RAPPORT_CAMPAGNE && role == Role.ENCADRANT) {
            throw new AccessDeniedException(
                    "Accès refusé : seuls les RH et ADMIN peuvent générer un rapport de campagne");
        }

        // ─── ENCADRANT: can only generate RAPPORT_STAGIAIRE for their own stagiaires
        if (role == Role.ENCADRANT && request.getType() == TypeRapport.RAPPORT_STAGIAIRE) {
            if (request.getStagiaireCibleId() == null) {
                throw new BusinessException("stagiaireCibleId requis pour RAPPORT_STAGIAIRE");
            }
            Stagiaire cible = stagiaireRepository.findById(request.getStagiaireCibleId())
                    .orElseThrow(() -> new BusinessException("Stagiaire introuvable"));
            if (cible.getEncadrant() == null
                    || !cible.getEncadrant().getId().equals(principal.getUser().getId())) {
                throw new AccessDeniedException(
                        "Accès refusé : vous ne pouvez générer un rapport que pour vos propres stagiaires");
            }
        }

        ReportJobDto job = reportingService.createJob(
                principal.getUser(), request.getType(), request.getStagiaireCibleId());
        // createJob() transaction committed — safe to launch async generation
        reportingService.generateAsync(job.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    @GetMapping("/jobs/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Statut d'un job de generation")
    public ResponseEntity<ReportJobDto> getJobStatus(@PathVariable Long id) {
        return ResponseEntity.ok(reportingService.toDto(reportingService.getJobById(id)));
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Liste des jobs de generation de rapports")
    public ResponseEntity<Page<ReportJobDto>> getJobs(
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(
                reportingService.getJobsByDemandeur(principal.getUser().getId(), pageable));
    }
}
