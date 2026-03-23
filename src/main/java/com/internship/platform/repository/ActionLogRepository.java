package com.internship.platform.repository;

import com.internship.platform.entity.ActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionLogRepository extends JpaRepository<ActionLog, Long> {
    Page<ActionLog> findByEntiteTypeAndEntiteIdOrderByTimestampDesc(String entiteType, Long entiteId,
            Pageable pageable);

    Page<ActionLog> findByActeurOrderByTimestampDesc(String acteur, Pageable pageable);
}
