package com.internship.platform.repository;

import com.internship.platform.entity.Tache;
import com.internship.platform.entity.enums.EtatTache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TacheRepository extends JpaRepository<Tache, Long> {
    Page<Tache> findByStagiaireId(Long stagiaireId, Pageable pageable);

    List<Tache> findByStagiaireId(Long stagiaireId);

    List<Tache> findByStagiaireIdAndEtat(Long stagiaireId, EtatTache etat);

    @Query("SELECT t FROM Tache t WHERE t.etat NOT IN ('TERMINE') AND t.deadline < :today AND t.alerteEnvoyee = false")
    List<Tache> findOverdueTachesForAlert(@Param("today") LocalDate today);

    @Query("SELECT t FROM Tache t WHERE t.etat NOT IN ('TERMINE') AND t.deadline BETWEEN :today AND :alertDate AND t.alerteEnvoyee = false")
    List<Tache> findUpcomingDeadlines(@Param("today") LocalDate today, @Param("alertDate") LocalDate alertDate);

    @Query("SELECT COUNT(t) FROM Tache t WHERE t.stagiaire.id = :stagiaireId AND t.etat = :etat")
    long countByEtat(@Param("stagiaireId") Long stagiaireId, @Param("etat") EtatTache etat);

    @Query("SELECT MONTH(t.dateDebut), COUNT(t) FROM Tache t " +
            "WHERE t.dateDebut >= :since GROUP BY MONTH(t.dateDebut) ORDER BY MONTH(t.dateDebut)")
    List<Object[]> countByMonth(@Param("since") LocalDate since);
}
