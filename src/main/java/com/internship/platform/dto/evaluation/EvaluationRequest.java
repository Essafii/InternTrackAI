package com.internship.platform.dto.evaluation;

import com.internship.platform.entity.enums.TypeEvaluation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EvaluationRequest {
    @NotNull
    private TypeEvaluation type;

    private Integer mois;

    @NotNull
    @Min(0)
    @Max(20)
    private Double noteTechnique;

    @NotNull
    @Min(0)
    @Max(20)
    private Double noteProgression;

    @NotNull
    @Min(0)
    @Max(20)
    private Double noteDelais;

    @NotNull
    @Min(0)
    @Max(20)
    private Double noteQualite;

    @NotNull
    @Min(0)
    @Max(20)
    private Double noteAutonomie;

    @NotNull
    @Min(0)
    @Max(20)
    private Double noteCommunication;

    private String commentaire;
    private String pointsForts;
    private String pointsAmeliorer;
}
