package com.internship.platform.entity;

import com.internship.platform.entity.enums.EtatTache;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "taches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tache extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stagiaire_id", nullable = false)
    private Stagiaire stagiaire;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EtatTache etat = EtatTache.A_FAIRE;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate deadline;

    @Column
    private LocalDate dateCompletion;

    @Column
    private Integer priorite;

    @Column
    private String commentaire;

    @Column(nullable = false)
    @Builder.Default
    private boolean alerteEnvoyee = false;
}
