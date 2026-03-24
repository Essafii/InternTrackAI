package com.internship.platform.dto.tache;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TacheDto {
    private Long id;
    private Long stagiaireId;
    private String stagiaireNom;
    private String titre;
    private String description;
    private String etat;
    private LocalDate dateDebut;
    private LocalDate deadline;
    private LocalDate dateCompletion;
    private Integer priorite;
    private String commentaire;
}
