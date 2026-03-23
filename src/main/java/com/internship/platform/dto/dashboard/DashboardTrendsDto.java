package com.internship.platform.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTrendsDto {
    // Monthly trend: list of {mois, absences, taches}
    private List<MonthPoint> monthlyTrends;

    // Absences per equipe/departement
    private List<TeamStat> teamStats;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthPoint {
        private String mois; // "Jan", "Fév", etc.
        private long absences;
        private long taches;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TeamStat {
        private String encadrant;
        private int stagiaires;
        private double tauxAssiduite;
    }
}
