package com.internship.platform.controller;

import com.internship.platform.dto.classement.ClassementDto;
import com.internship.platform.entity.enums.Role;
import com.internship.platform.security.CustomUserDetails;
import org.springframework.security.access.AccessDeniedException;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Classement global des stagiaires (filtrable par équipe ou encadrant)")
    public ResponseEntity<List<ClassementDto>> getClassement(
            @RequestParam(required = false) Long encadrantId,
            @RequestParam(required = false) String equipe,
            @AuthenticationPrincipal CustomUserDetails principal) {

        // ─── Encadrant isolation: force their own encadrantId, block equipe bypass ───
        if (principal.getUser().getRole() == Role.ENCADRANT) {
            Long ownId = principal.getUser().getId();

            // If encadrantId is provided but doesn't match their own → deny
            if (encadrantId != null && !ownId.equals(encadrantId)) {
                throw new AccessDeniedException(
                        "Accès refusé : vous ne pouvez consulter que le classement de votre propre équipe");
            }

            // If equipe is provided without encadrantId, the encadrant could see
            // stagiaires outside their scope → deny
            if (equipe != null && !equipe.isBlank() && encadrantId == null) {
                throw new AccessDeniedException(
                        "Accès refusé : les encadrants ne peuvent pas filtrer par équipe sans restriction d'encadrant");
            }

            // Force isolation: always restrict to own encadrantId
            encadrantId = ownId;
        }

        return ResponseEntity.ok(classementService.getClassement(encadrantId, equipe));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Rang et score du stagiaire connecté")
    public ResponseEntity<ClassementDto> getMyRank(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(classementService.getMyRank(principal.getUser().getId()));
    }
}
