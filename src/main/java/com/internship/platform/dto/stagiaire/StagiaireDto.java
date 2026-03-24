package com.internship.platform.dto.stagiaire;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StagiaireDto {
    private Long id;
    private Long userId;
    private String email;
    private String fullName;
    private String sujet;
    private String equipe;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String statut;
    private String etablissement;
    private String niveauEtude;
    private String specialite;
    private Long encadrantId;
    private String encadrantNom;
    private boolean welcomeEmailSent;
    private String welcomeEmailError;
}
