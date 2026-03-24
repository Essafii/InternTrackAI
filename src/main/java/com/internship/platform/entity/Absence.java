package com.internship.platform.entity;

import com.internship.platform.entity.enums.TypeAbsence;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "absences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Absence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stagiaire_id", nullable = false)
    private Stagiaire stagiaire;

    @Column(nullable = false)
    private LocalDate dateAbsence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAbsence type;

    @Column
    private String motif;

    @Column
    private String justificatifPath;

    @Column
    private String justificatifNom;

    @Column(nullable = false)
    @Builder.Default
    private boolean justifiee = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean validee = false;

    @Column
    private String commentaireEncadrant;
}
