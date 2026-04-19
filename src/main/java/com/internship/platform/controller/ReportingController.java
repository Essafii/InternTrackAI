package com.internship.platform.controller;

import com.internship.platform.dto.reporting.ReportGenerateRequest;
import com.internship.platform.dto.reporting.ReportJobDto;
import com.internship.platform.entity.ReportJob;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reporting")
@RequiredArgsConstructor
@Tag(name = "Reporting")
public class ReportingController {

    private final ReportingService reportingService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Declencher la generation d'un rapport PDF")
    public ResponseEntity<ReportJobDto> generateReport(
            @Valid @RequestBody ReportGenerateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
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
