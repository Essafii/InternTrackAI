package com.internship.platform.dto.evaluation;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EvaluationDto {
    private Long id;
    private Long stagiaireId;
    private String stagiaireNom;
    private String evaluateurNom;
    private String type;
    private LocalDate dateEvaluation;
    private Integer mois;
    private Double noteTechnique;
    private Double noteProgression;
    private Double noteDelais;
    private Double noteQualite;
    private Double noteAutonomie;
    private Double noteCommunication;
    private Double noteMoyenne;
    private String commentaire;
    private String pointsForts;
    private String pointsAmeliorer;
    private boolean validee;
}
