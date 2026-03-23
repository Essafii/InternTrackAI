package com.internship.platform.controller;

import com.internship.platform.entity.ActionLog;
import com.internship.platform.repository.ActionLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Log")
public class AuditLogController {

    private final ActionLogRepository actionLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister toutes les entrées du journal d'audit")
    public ResponseEntity<Page<ActionLog>> getAll(Pageable pageable) {
        return ResponseEntity.ok(actionLogRepository.findAll(pageable));
    }

    @GetMapping("/entite/{type}/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Journal d'audit pour une entité spécifique")
    public ResponseEntity<Page<ActionLog>> getByEntite(
            @PathVariable String type,
            @PathVariable Long id,
            Pageable pageable) {
        return ResponseEntity.ok(
                actionLogRepository.findByEntiteTypeAndEntiteIdOrderByTimestampDesc(type, id, pageable));
    }

    @GetMapping("/acteur/{acteur}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Journal d'audit par acteur")
    public ResponseEntity<Page<ActionLog>> getByActeur(
            @PathVariable String acteur,
            Pageable pageable) {
        return ResponseEntity.ok(
                actionLogRepository.findByActeurOrderByTimestampDesc(acteur, pageable));
    }
}
