package com.internship.platform.service;

import com.internship.platform.entity.ActionLog;
import com.internship.platform.repository.ActionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final ActionLogRepository actionLogRepository;

    public void log(String action, String entiteType, Long entiteId, String details) {
        String acteur = getCurrentUser();
        ActionLog log = ActionLog.builder()
                .timestamp(LocalDateTime.now())
                .acteur(acteur)
                .action(action)
                .entiteType(entiteType)
                .entiteId(entiteId)
                .details(details)
                .build();
        actionLogRepository.save(log);
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "system";
    }
}
