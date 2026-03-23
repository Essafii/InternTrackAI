package com.internship.platform.repository;

import com.internship.platform.entity.Absence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AbsenceRepository extends JpaRepository<Absence, Long> {
    Page<Absence> findByStagiaireId(Long stagiaireId, Pageable pageable);

    List<Absence> findByStagiaireId(Long stagiaireId);

    @Query("SELECT COUNT(a) FROM Absence a WHERE a.stagiaire.id = :stagiaireId AND a.dateAbsence BETWEEN :debut AND :fin")
    long countAbsencesByPeriod(@Param("stagiaireId") Long stagiaireId,
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin);

    @Query("SELECT COUNT(a) FROM Absence a WHERE a.stagiaire.id = :stagiaireId AND a.justifiee = false")
    long countNonJustifiees(@Param("stagiaireId") Long stagiaireId);

    boolean existsByStagiaireIdAndDateAbsence(Long stagiaireId, LocalDate dateAbsence);

    @Query("SELECT MONTH(a.dateAbsence), COUNT(a) FROM Absence a " +
            "WHERE a.dateAbsence >= :since GROUP BY MONTH(a.dateAbsence) ORDER BY MONTH(a.dateAbsence)")
    List<Object[]> countByMonth(@Param("since") LocalDate since);
}
