package com.internship.platform.repository;

import com.internship.platform.entity.ReportJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportJobRepository extends JpaRepository<ReportJob, Long> {
    Page<ReportJob> findByDemandeurIdOrderByCreatedAtDesc(Long demandeurId, Pageable pageable);
}
