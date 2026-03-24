package com.internship.platform.repository;

import com.internship.platform.entity.Evaluation;
import com.internship.platform.entity.enums.TypeEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Page<Evaluation> findByStagiaireId(Long stagiaireId, Pageable pageable);

    List<Evaluation> findByStagiaireId(Long stagiaireId);

    List<Evaluation> findByStagiaireIdAndType(Long stagiaireId, TypeEvaluation type);

    Optional<Evaluation> findByStagiaireIdAndTypeAndMois(Long stagiaireId, TypeEvaluation type, Integer mois);

    @Query("SELECT COUNT(e) FROM Evaluation e WHERE e.stagiaire.id = :stagiaireId AND e.type = 'MENSUELLE' AND e.validee = true")
    long countValidatedMensuelles(@Param("stagiaireId") Long stagiaireId);

    @Query("SELECT e FROM Evaluation e WHERE e.stagiaire.encadrant.id = :encadrantId AND e.validee = false")
    Page<Evaluation> findPendingByEncadrant(@Param("encadrantId") Long encadrantId, Pageable pageable);

    @Query("SELECT AVG(e.noteMoyenne) FROM Evaluation e WHERE e.stagiaire.id = :stagiaireId AND e.validee = true")
    Optional<Double> findAverageNoteByStaigaire(@Param("stagiaireId") Long stagiaireId);
}
