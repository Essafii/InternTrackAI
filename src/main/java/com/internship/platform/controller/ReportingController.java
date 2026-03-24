package com.internship.platform.controller;

import com.internship.platform.entity.ReportJob;
import com.internship.platform.entity.enums.TypeRapport;
import com.internship.platform.service.ReportingService;
import com.internship.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@RestController
@RequestMapping("/reporting")
@RequiredArgsConstructor
@Tag(name = "Reporting PDF")
public class ReportingController {

    private final ReportingService reportingService;
    private final UserService userService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Lancer la génération d'un rapport PDF (async)")
    public ResponseEntity<ReportJob> generateReport(
            @RequestParam TypeRapport type,
            @RequestParam(required = false) Long stagiaireId,
            Authentication auth) {
        var demandeur = userService.findByEmail(auth.getName());
        return ResponseEntity.accepted().body(reportingService.createReportJob(demandeur, type, stagiaireId));
    }

    @GetMapping("/jobs/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Statut d'un job de génération")
    public ResponseEntity<ReportJob> getJobStatus(@PathVariable Long id) {
        return ResponseEntity.ok(reportingService.getJobById(id));
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Mes jobs de génération")
    public ResponseEntity<Page<ReportJob>> getMyJobs(Authentication auth, Pageable pageable) {
        Long userId = userService.findByEmail(auth.getName()).getId();
        return ResponseEntity.ok(reportingService.getJobsByUser(userId, pageable));
    }

    @GetMapping("/stagiaire/{id}/pdf")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Télécharger directement le rapport PDF enrichi d'un stagiaire")
    public ResponseEntity<byte[]> exportStagiairePdf(@PathVariable Long id) throws Exception {
        byte[] bytes = reportingService.generateRapportStagiaireBytes(id);
        String filename = "rapport_stagiaire_" + id + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    @GetMapping("/jobs/{id}/download")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Télécharger le rapport PDF généré")
    public ResponseEntity<Resource> downloadReport(@PathVariable Long id) throws Exception {
        ReportJob job = reportingService.getJobById(id);
        if (job.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }
        Path filePath = reportingService.getJobById(id) != null
                ? java.nio.file.Paths.get("./uploads").resolve(job.getFilePath()).normalize()
                : null;
        Resource resource = new UrlResource(filePath.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName() + "\"")
                .body(resource);
    }
}
