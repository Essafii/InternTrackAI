package com.internship.platform.service;

import com.internship.platform.entity.enums.EtatTache;
import com.internship.platform.repository.EvaluationRepository;
import com.internship.platform.repository.TacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScoringDomainService {

    private final EvaluationRepository evaluationRepository;
    private final TacheRepository tacheRepository;
    private final AbsenceService absenceService;

    public ScoringResult computeScore(Long stagiaireId) {
        double noteMoyenne = evaluationRepository
                .findAverageNoteByStaigaire(stagiaireId).orElse(0.0);
        double tauxAssiduite = absenceService.calculerTauxAssiduite(stagiaireId);

        long total = tacheRepository.findByStagiaireId(stagiaireId).size();
        long terminees = tacheRepository.countByEtat(stagiaireId, EtatTache.TERMINE);
        long enRetard = tacheRepository.countByEtat(stagiaireId, EtatTache.EN_RETARD);

        double tauxTaches = total > 0 ? (terminees * 100.0 / total) : 100.0;
        double tauxRetard = total > 0 ? (enRetard * 100.0 / total) : 0.0;

        double risque = 0.0;
        if (tauxAssiduite < 80) risque += 5.0;
        if (tauxRetard > 30)    risque += 5.0;
        if (noteMoyenne < 10)   risque += 5.0;
        risque = Math.min(risque, 10.0);

        double scoreGlobal = (noteMoyenne / 20.0 * 40.0)
                + (tauxAssiduite / 100.0 * 25.0)
                + (tauxTaches / 100.0 * 25.0)
                + (10.0 - risque);
        scoreGlobal = Math.min(scoreGlobal, 100.0);

        return new ScoringResult(noteMoyenne, tauxAssiduite, tauxTaches, tauxRetard, risque, scoreGlobal);
    }
}
