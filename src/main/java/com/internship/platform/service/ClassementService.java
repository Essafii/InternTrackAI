package com.internship.platform.service;

import com.internship.platform.dto.classement.ClassementDto;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.enums.StatutStagiaire;
import com.internship.platform.exception.ResourceNotFoundException;
import com.internship.platform.repository.StagiaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassementService {

    private final StagiaireRepository stagiaireRepository;
    private final ScoringDomainService scoringDomainService;

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
                .map(this::toDto)
                .sorted(Comparator.comparingDouble(ClassementDto::getScoreGlobal).reversed())
                .collect(Collectors.toList());

        for (int i = 0; i < classement.size(); i++) {
            classement.get(i).setRang(i + 1);
        }
        return classement;
    }

    /**
     * DB-level rank calculation — O(1) memory instead of loading all stagiaires.
     * Fetches the stagiaire's scoreCalcule, delegates rank counting to a JPQL COUNT query.
     */
    @Transactional
    public ClassementDto getMyRank(Long userId) {
        Stagiaire stagiaire = stagiaireRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire non trouvé pour cet utilisateur"));

        // Compute live score and persist it for the rank query
        ScoringResult result = scoringDomainService.computeScore(stagiaire.getId());
        stagiaire.setScoreCalcule(result.scoreGlobal());
        stagiaireRepository.save(stagiaire);

        // DB-level rank: COUNT of stagiaires with higher score + 1
        long rank = stagiaireRepository.getRankByScore(stagiaire.getScoreCalcule());

        ClassementDto dto = toDto(stagiaire);
        dto.setRang((int) rank);
        return dto;
    }

    public ClassementDto computeScore(Stagiaire s) {
        return toDto(s);
    }

    private ClassementDto toDto(Stagiaire s) {
        ScoringResult result = scoringDomainService.computeScore(s.getId());
        ClassementDto dto = new ClassementDto();
        dto.setStagiaireId(s.getId());
        dto.setStagiaireNom(s.getUser().getFullName());
        dto.setEquipe(s.getEquipe());
        dto.setNoteMoyenne(result.noteMoyenne());
        dto.setTauxAssiduite(result.tauxAssiduite());
        dto.setTauxTachesTerminees(result.tauxTachesTerminees());
        dto.setTauxRetard(result.tauxRetard());
        dto.setRisqueIA(result.risque());
        dto.setScoreGlobal(result.scoreGlobal());
        return dto;
    }
}
