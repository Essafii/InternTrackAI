package com.internship.platform.controller;

import com.internship.platform.entity.Notification;
import com.internship.platform.service.NotificationService;
import com.internship.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Mes notifications")
    public ResponseEntity<Page<Notification>> getMesNotifications(Authentication auth, Pageable pageable) {
        Long userId = userService.findByEmail(auth.getName()).getId();
        return ResponseEntity.ok(notificationService.getNotificationsByUser(userId, pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Nombre de notifications non lues")
    public ResponseEntity<Long> getUnreadCount(Authentication auth) {
        Long userId = userService.findByEmail(auth.getName()).getId();
        return ResponseEntity.ok(notificationService.countUnread(userId));
    }

    @PatchMapping("/mark-all-read")
    @Operation(summary = "Marquer toutes les notifications comme lues")
    public ResponseEntity<Void> markAllAsRead(Authentication auth) {
        Long userId = userService.findByEmail(auth.getName()).getId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marquer une notification comme lue")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}
