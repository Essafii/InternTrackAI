package com.internship.platform.dto.dashboard;

import lombok.Data;

@Data
public class DashboardEncadrantDto {
    private int totalStagiaires;
    private int stagiairesActifs;
    private int stagiairesEnRetard;
    private int stagiairesTermines;
    private int stagiairesArchives;
    private int evaluationsEnAttente;
    private int livrablesEnAttente;
}
