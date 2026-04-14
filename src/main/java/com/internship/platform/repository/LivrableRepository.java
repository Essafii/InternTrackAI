package com.internship.platform.repository;

import com.internship.platform.entity.Livrable;
import com.internship.platform.entity.enums.StatutLivrable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LivrableRepository extends JpaRepository<Livrable, Long> {
    Page<Livrable> findByStagiaireId(Long stagiaireId, Pageable pageable);

    List<Livrable> findByStagiaireId(Long stagiaireId);

    List<Livrable> findByStagiaireIdAndStatut(Long stagiaireId, StatutLivrable statut);

    @Query("SELECT MAX(l.version) FROM Livrable l WHERE l.stagiaire.id = :stagiaireId AND l.titre = :titre")
    Integer findMaxVersionByTitre(@Param("stagiaireId") Long stagiaireId, @Param("titre") String titre);

    @Query("SELECT l FROM Livrable l WHERE l.stagiaire.encadrant.id = :encadrantId AND l.statut = 'SOUMIS'")
    Page<Livrable> findPendingByEncadrant(@Param("encadrantId") Long encadrantId, Pageable pageable);
}
