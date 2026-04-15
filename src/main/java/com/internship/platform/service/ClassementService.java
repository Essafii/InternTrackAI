package com.internship.platform.service;

import com.internship.platform.dto.classement.ClassementDto;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.enums.EtatTache;
import com.internship.platform.entity.enums.StatutStagiaire;
import com.internship.platform.repository.EvaluationRepository;
import com.internship.platform.repository.StagiaireRepository;
import com.internship.platform.repository.TacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassementService {

    private final StagiaireRepository stagiaireRepository;
    private final EvaluationRepository evaluationRepository;
    private final TacheRepository tacheRepository;
    private final AbsenceService absenceService;

    public List<ClassementDto> getClassement(Long encadrantId, String equipe) {
        List<Stagiaire> stagiaires;
        if (encadrantId != null) {
            stagiaires = stagiaireRepository.findByEncadrantId(encadrantId);
        } else if (equipe != null && !equipe.isBlank()) {
            stagiaires = stagiaireRepository.findByEquipe(equipe,
                    org.springframework.data.domain.Pageable.unpaged()).getContent();
        } else {
            stagiaires = stagiaireRepository.findAll();
        }

        List<ClassementDto> classement = stagiaires.stream()
                .filter(s -> s.getStatut() == StatutStagiaire.ACTIF || s.getStatut() == StatutStagiaire.TERMINE)
                .map(this::calculerScore)
                .sorted(Comparator.comparingDouble(ClassementDto::getScoreGlobal).reversed())
                .collect(Collectors.toList());

        // Assign ranks
        for (int i = 0; i < classement.size(); i++) {
            classement.get(i).setRang(i + 1);
        }
        return classement;
    }

    private ClassementDto calculerScore(Stagiaire s) {
        ClassementDto dto = new ClassementDto();
        dto.setStagiaireId(s.getId());
        dto.setStagiaireNom(s.getUser().getFullName());
        dto.setEquipe(s.getEquipe());

        // Note moyenne évaluations (40%)
        double noteMoyenne = evaluationRepository.findAverageNoteByStaigaire(s.getId()).orElse(0.0);
        dto.setNoteMoyenne(noteMoyenne);

        // Taux assiduité (25%)
        double assiduite = absenceService.calculerTauxAssiduite(s.getId());
        dto.setTauxAssiduite(assiduite);

        // Taux de tâches terminées (25%)
        long total = tacheRepository.findByStagiaireId(s.getId()).size();
        long terminees = tacheRepository.countByEtat(s.getId(), EtatTache.TERMINE);
        long enRetard = tacheRepository.countByEtat(s.getId(), EtatTache.EN_RETARD);
        double tauxTaches = total > 0 ? (terminees / (double) total) * 100 : 100.0;
        double tauxRetard = total > 0 ? (enRetard / (double) total) * 100 : 0.0;
        dto.setTauxTachesTerminees(tauxTaches);
        dto.setTauxRetard(tauxRetard);

        // Score global pondéré (sur 100)
        double scoreGlobal = (noteMoyenne / 20.0 * 40) + (assiduite / 100.0 * 25) + (tauxTaches / 100.0 * 25);

        // Indice de risque IA (10%) - simple heuristic
        double risqueIA = 0;
        if (assiduite < 80)
            risqueIA += 5;
        if (tauxRetard > 30)
            risqueIA += 5;
        if (noteMoyenne < 10)
            risqueIA += 5;
        dto.setRisqueIA(Math.min(risqueIA, 10));

        scoreGlobal += (10 - dto.getRisqueIA());
        dto.setScoreGlobal(Math.min(scoreGlobal, 100));

        return dto;
    }
}
