package com.internship.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "action_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String acteur;

    @Column(nullable = false)
    private String action;

    @Column
    private String entiteType;

    @Column
    private Long entiteId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column
    private String ipAddress;
}
