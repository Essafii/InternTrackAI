package com.internship.platform.dto.livrable;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LivrableDto {
    private Long id;
    private Long stagiaireId;
    private String stagiaireNom;
    private String titre;
    private String description;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer version;
    private String statut;
    private String commentaireEncadrant;
    private LocalDateTime dateValidation;
    private Double scoreIA;
    private String feedbackIA;
    private String niveauQualiteIA;
}
