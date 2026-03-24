package com.internship.platform.dto.stagiaire;

import com.internship.platform.entity.enums.StatutStagiaire;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StagiaireUpdateRequest {
    private String sujet;
    private String equipe;
    private LocalDate dateFin;
    private String description;
    private StatutStagiaire statut;
    private Long encadrantId;
}
