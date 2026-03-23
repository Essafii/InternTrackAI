package com.internship.platform.entity;

import com.internship.platform.entity.enums.StatutStagiaire;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stagiaires")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stagiaire extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "encadrant_id")
    private User encadrant;

    @Column(nullable = false)
    private String sujet;

    @Column
    private String equipe;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutStagiaire statut = StatutStagiaire.ACTIF;

    @Column
    private String etablissement;

    @Column
    private String niveauEtude;

    @Column
    private String specialite;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "stagiaire", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Absence> absences = new ArrayList<>();

    @OneToMany(mappedBy = "stagiaire", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Tache> taches = new ArrayList<>();

    @OneToMany(mappedBy = "stagiaire", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Livrable> livrables = new ArrayList<>();

    @OneToMany(mappedBy = "stagiaire", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Evaluation> evaluations = new ArrayList<>();
}
