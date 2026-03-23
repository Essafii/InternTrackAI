package com.internship.platform.entity;

import com.internship.platform.entity.enums.TypeNotification;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private User destinataire;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeNotification type;

    @Column(nullable = false)
    @Builder.Default
    private boolean lue = false;

    @Column
    private String lienAction;

    @Column
    private Long entiteId;

    @Column
    private String entiteType;
}
