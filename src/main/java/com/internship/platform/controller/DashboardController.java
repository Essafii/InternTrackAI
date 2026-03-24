package com.internship.platform.controller;

import com.internship.platform.dto.dashboard.DashboardEncadrantDto;
import com.internship.platform.dto.dashboard.DashboardRhDto;
import com.internship.platform.dto.dashboard.DashboardStagiaireDto;
import com.internship.platform.dto.dashboard.DashboardTrendsDto;
import com.internship.platform.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboards")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/rh")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Dashboard RH - KPIs globaux")
    public ResponseEntity<DashboardRhDto> getDashboardRh() {
        return ResponseEntity.ok(dashboardService.getDashboardRh());
    }

    @GetMapping("/rh/trends")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Analytics RH - tendances mensuelles")
    public ResponseEntity<DashboardTrendsDto> getDashboardRhTrends() {
        return ResponseEntity.ok(dashboardService.getMonthlyTrends());
    }

    @GetMapping("/encadrant/{encadrantId}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Dashboard Encadrant - Vue équipe")
    public ResponseEntity<DashboardEncadrantDto> getDashboardEncadrant(@PathVariable Long encadrantId) {
        return ResponseEntity.ok(dashboardService.getDashboardEncadrant(encadrantId));
    }

    @GetMapping("/stagiaire/{stagiaireId}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT', 'STAGIAIRE')")
    @Operation(summary = "Dashboard Stagiaire - Progression personnelle")
    public ResponseEntity<DashboardStagiaireDto> getDashboardStagiaire(@PathVariable Long stagiaireId) {
        return ResponseEntity.ok(dashboardService.getDashboardStagiaire(stagiaireId));
    }
}
