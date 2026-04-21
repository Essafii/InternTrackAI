package com.internship.platform.entity;

import com.internship.platform.entity.enums.StatutLivrable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "livrables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Livrable extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stagiaire_id", nullable = false)
    private Stagiaire stagiaire;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tache_id")
    private Tache tache;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String fileName;

    @Column
    private String fileType;

    @Column
    private Long fileSize;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutLivrable statut = StatutLivrable.SOUMIS;

    @Column
    private String commentaireEncadrant;

    @Column
    private LocalDateTime dateValidation;

    @Column(name = "score_calcule")
    private Double scoreCalcule;

    @Column(name = "feedback_score", columnDefinition = "TEXT")
    private String feedbackScore;

    @Column(name = "niveau_qualite")
    private String niveauQualite;
}
