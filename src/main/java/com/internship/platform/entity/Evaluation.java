package com.internship.platform.entity;

import com.internship.platform.entity.enums.TypeEvaluation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evaluation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stagiaire_id", nullable = false)
    private Stagiaire stagiaire;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "evaluateur_id", nullable = false)
    private User evaluateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeEvaluation type;

    @Column(nullable = false)
    private LocalDate dateEvaluation;

    @Column
    private Integer mois; // for monthly evaluations

    // Critères (0-20)
    @Column(nullable = false)
    @Builder.Default
    private Double noteTechnique = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double noteProgression = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double noteDelais = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double noteQualite = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double noteAutonomie = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double noteCommunication = 0.0;

    @Column
    private Double noteMoyenne;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(columnDefinition = "TEXT")
    private String pointsForts;

    @Column(columnDefinition = "TEXT")
    private String pointsAmeliorer;

    @Column(nullable = false)
    @Builder.Default
    private boolean validee = false;

    // Modification trace
    @Column(columnDefinition = "TEXT")
    private String justificationModification;

    @Column
    private String modifiePar;
}
