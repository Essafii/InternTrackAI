package com.internship.platform.service;

public record ScoringResult(
        double noteMoyenne,
        double tauxAssiduite,
        double tauxTachesTerminees,
        double tauxRetard,
        double risque,       // 0–10 scale
        double scoreGlobal   // 0–100 scale
) {

    /** Normalize to 0–20 for livrable pre-evaluation. */
    public double scoreCalcule() {
        return Math.min(scoreGlobal / 5.0, 20.0);
    }

    public String niveau() {
        double sc = scoreCalcule();
        if (sc >= 17) return "EXCELLENT";
        else if (sc >= 14) return "BON";
        else if (sc >= 10) return "MOYEN";
        else return "INSUFFISANT";
    }

    public String feedback() {
        return String.format(
                "Score: %.1f/20 | Note moy: %.1f/20 | Assiduité: %.0f%% | Tâches: %.0f%% | Risque: %.0f/10",
                scoreCalcule(), noteMoyenne, tauxAssiduite, tauxTachesTerminees, risque);
    }
}
