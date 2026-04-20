package com.internship.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "score_snapshots", indexes = {
        @Index(name = "idx_snapshot_stagiaire", columnList = "stagiaire_id"),
        @Index(name = "idx_snapshot_date", columnList = "date_calcul")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stagiaire_id", nullable = false)
    private Stagiaire stagiaire;

    @Column(name = "score_global", nullable = false)
    private Double scoreGlobal;

    @Column(name = "date_calcul", nullable = false)
    private LocalDateTime dateCalcul;
}
