package com.internship.platform.repository;

import com.internship.platform.entity.ScoreSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScoreSnapshotRepository extends JpaRepository<ScoreSnapshot, Long> {

    List<ScoreSnapshot> findByStagiaireIdOrderByDateCalculAsc(Long stagiaireId);
}
