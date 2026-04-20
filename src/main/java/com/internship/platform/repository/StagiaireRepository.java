package com.internship.platform.repository;

import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.enums.StatutStagiaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StagiaireRepository extends JpaRepository<Stagiaire, Long> {
    Optional<Stagiaire> findByUserId(Long userId);

    List<Stagiaire> findByEncadrantId(Long encadrantId);

    Page<Stagiaire> findByEncadrantId(Long encadrantId, Pageable pageable);

    Page<Stagiaire> findByStatut(StatutStagiaire statut, Pageable pageable);

    List<Stagiaire> findByStatut(StatutStagiaire statut);

    @Query("SELECT s FROM Stagiaire s WHERE s.equipe = :equipe")
    Page<Stagiaire> findByEquipe(@Param("equipe") String equipe, Pageable pageable);

    @Query("SELECT s FROM Stagiaire s WHERE s.encadrant.id = :encadrantId AND s.statut = :statut")
    List<Stagiaire> findByEncadrantIdAndStatut(@Param("encadrantId") Long encadrantId,
            @Param("statut") StatutStagiaire statut);

    @Query("SELECT COUNT(s) FROM Stagiaire s WHERE s.statut = :statut")
    long countByStatut(@Param("statut") StatutStagiaire statut);

    @Query("SELECT COUNT(s) + 1 FROM Stagiaire s WHERE s.scoreCalcule > :score AND s.statut IN ('ACTIF', 'TERMINE')")
    long getRankByScore(@Param("score") Double score);
}
