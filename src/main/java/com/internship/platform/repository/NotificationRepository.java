package com.internship.platform.repository;

import com.internship.platform.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByDestinataireIdOrderByCreatedAtDesc(Long destinataireId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.destinataire.id = :userId AND n.lue = false")
    long countUnread(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.lue = true WHERE n.destinataire.id = :userId AND n.lue = false")
    void markAllAsRead(@Param("userId") Long userId);
}
