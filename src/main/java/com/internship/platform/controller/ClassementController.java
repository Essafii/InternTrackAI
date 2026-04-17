package com.internship.platform.controller;

import com.internship.platform.dto.classement.ClassementDto;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.enums.Role;
import com.internship.platform.exception.BusinessException;
import com.internship.platform.exception.ResourceNotFoundException;
import com.internship.platform.repository.StagiaireRepository;
import com.internship.platform.security.CustomUserDetails;
import com.internship.platform.service.ClassementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/classement")
@RequiredArgsConstructor
@Tag(name = "Classement")
public class ClassementController {

    private final ClassementService classementService;
    private final StagiaireRepository stagiaireRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Classement global des stagiaires (filtrable par équipe ou encadrant)")
    public ResponseEntity<List<ClassementDto>> getClassement(
            @RequestParam(required = false) Long encadrantId,
            @RequestParam(required = false) String equipe,
            @AuthenticationPrincipal CustomUserDetails principal) {
        if (encadrantId != null
                && principal.getUser().getRole() == Role.ENCADRANT
                && !principal.getUser().getId().equals(encadrantId)) {
            throw new BusinessException("Accès refusé : vous ne pouvez consulter que le classement de votre propre équipe");
        }
        return ResponseEntity.ok(classementService.getClassement(encadrantId, equipe));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Rang et score du stagiaire connecté")
    public ResponseEntity<ClassementDto> getMyRank(
            @AuthenticationPrincipal CustomUserDetails principal) {
        Stagiaire stagiaire = stagiaireRepository.findByUserId(principal.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire non trouvé pour cet utilisateur"));
        List<ClassementDto> all = classementService.getClassement(null, null);
        return ResponseEntity.ok(all.stream()
                .filter(d -> d.getStagiaireId().equals(stagiaire.getId()))
                .findFirst()
                .orElseGet(() -> classementService.computeScore(stagiaire)));
    }
}
