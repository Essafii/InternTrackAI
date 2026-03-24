package com.internship.platform.dto.dashboard;

import lombok.Data;

@Data
public class DashboardStagiaireDto {
    private Long stagiaireId;
    private String statut;
    private double tauxAssiduite;
    private int tachesTotal;
    private int tachesTerminees;
    private int tachesEnRetard;
    private int notificationsNonLues;
    private double noteMoyenne;
}
