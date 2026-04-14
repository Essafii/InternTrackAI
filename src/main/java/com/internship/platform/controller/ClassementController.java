package com.internship.platform.controller;

import com.internship.platform.dto.classement.ClassementDto;
import com.internship.platform.service.ClassementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
            @RequestParam(required = false) String equipe) {
        return ResponseEntity.ok(classementService.getClassement(encadrantId, equipe));
    }
}
