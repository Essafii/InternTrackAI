package com.internship.platform.controller;

import com.internship.platform.repository.AbsenceRepository;
import com.internship.platform.repository.StagiaireRepository;
import com.internship.platform.repository.TacheRepository;
import com.internship.platform.service.ExcelExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
@Tag(name = "Excel Export")
public class ExcelExportController {

    private static final MediaType XLSX_TYPE = MediaType
            .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExcelExportService excelExportService;
    private final StagiaireRepository stagiaireRepository;
    private final AbsenceRepository absenceRepository;
    private final TacheRepository tacheRepository;

    @GetMapping("/stagiaires")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Exporter tous les stagiaires en XLSX")
    public ResponseEntity<byte[]> exportStagiaires() throws Exception {
        byte[] bytes = excelExportService.exportStagiaires(stagiaireRepository.findAll());
        return ResponseEntity.ok()
                .contentType(XLSX_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stagiaires.xlsx\"")
                .body(bytes);
    }

    @GetMapping("/absences")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Exporter les absences en XLSX, optionnellement filtrées par stagiaire")
    public ResponseEntity<byte[]> exportAbsences(
            @RequestParam(required = false) Long stagiaireId) throws Exception {
        var absences = stagiaireId != null
                ? absenceRepository.findByStagiaireId(stagiaireId)
                : absenceRepository.findAll();
        byte[] bytes = excelExportService.exportAbsences(absences);
        return ResponseEntity.ok()
                .contentType(XLSX_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"absences.xlsx\"")
                .body(bytes);
    }

    @GetMapping("/taches")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Exporter les tâches en XLSX, optionnellement filtrées par stagiaire")
    public ResponseEntity<byte[]> exportTaches(
            @RequestParam(required = false) Long stagiaireId) throws Exception {
        var taches = stagiaireId != null
                ? tacheRepository.findByStagiaireId(stagiaireId)
                : tacheRepository.findAll();
        byte[] bytes = excelExportService.exportTaches(taches);
        return ResponseEntity.ok()
                .contentType(XLSX_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"taches.xlsx\"")
                .body(bytes);
    }
}
