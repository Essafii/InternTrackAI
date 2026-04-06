package com.internship.platform.dto.absence;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AbsenceDto {
    private Long id;
    private Long stagiaireId;
    private String stagiaireNom;
    private LocalDate dateAbsence;
    private String type;
    private String motif;
    private boolean justifiee;
    private boolean validee;
    private String justificatifNom;
    private double tauxAssiduite;
}
