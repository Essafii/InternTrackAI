package com.internship.platform.dto.classement;

import lombok.Data;

@Data
public class ClassementDto {
    private int rang;
    private Long stagiaireId;
    private String stagiaireNom;
    private String equipe;
    private double noteMoyenne;
    private double tauxAssiduite;
    private double tauxTachesTerminees;
    private double tauxRetard;
    private double risqueIA;
    private double scoreGlobal;
}
