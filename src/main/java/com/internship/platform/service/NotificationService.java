package com.internship.platform.service;

import com.internship.platform.entity.Notification;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.User;
import com.internship.platform.entity.enums.TypeNotification;
import com.internship.platform.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Transactional
    public void createNotification(User destinataire, String titre, String message,
            TypeNotification type, Long entiteId, String entiteType) {
        if (destinataire == null)
            return;
        Notification notification = Notification.builder()
                .destinataire(destinataire)
                .titre(titre)
                .message(message)
                .type(type)
                .lue(false)
                .entiteId(entiteId)
                .entiteType(entiteType)
                .build();
        notificationRepository.save(notification);
        // Push real-time via SSE
        com.internship.platform.controller.SseController.pushNotification(
                destinataire.getId(),
                java.util.Map.of("titre", titre, "message", message, "type", type.name()));

        // Also send email
        emailService.sendNotificationEmail(destinataire.getEmail(), titre, message);
    }

    public void sendOnboardingNotification(Stagiaire stagiaire) {
        createNotification(
                stagiaire.getUser(),
                "Bienvenue sur la plateforme!",
                "Votre compte stagiaire a été créé. Sujet: " + stagiaire.getSujet(),
                TypeNotification.ONBOARDING,
                stagiaire.getId(), "Stagiaire");
        if (stagiaire.getEncadrant() != null) {
            createNotification(
                    stagiaire.getEncadrant(),
                    "Nouveau stagiaire affecté",
                    "Le stagiaire " + stagiaire.getUser().getFullName() + " vous a été affecté. Sujet: "
                            + stagiaire.getSujet(),
                    TypeNotification.ONBOARDING,
                    stagiaire.getId(), "Stagiaire");
        }
    }

    public Page<Notification> getNotificationsByUser(Long userId, Pageable pageable) {
        return notificationRepository.findByDestinataireIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long countUnread(Long userId) {
        return notificationRepository.countUnread(userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setLue(true);
            notificationRepository.save(n);
        });
    }
}
