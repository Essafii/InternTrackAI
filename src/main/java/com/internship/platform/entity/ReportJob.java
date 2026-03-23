package com.internship.platform.entity;

import com.internship.platform.entity.enums.StatutRapport;
import com.internship.platform.entity.enums.TypeRapport;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private User demandeur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeRapport type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutRapport statut = StatutRapport.EN_ATTENTE;

    @Column
    private Long stagiaireCibleId;

    @Column
    private String filePath;

    @Column
    private LocalDateTime dateGeneration;

    @Column
    private String erreur;
}
