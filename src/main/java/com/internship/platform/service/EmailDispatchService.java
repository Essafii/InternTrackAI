package com.internship.platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dedicated bean for email dispatch so @Async and @Transactional(REQUIRES_NEW)
 * are properly applied via AOP proxy (self-calls bypass Spring AOP).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDispatchService {

    private final EmailService emailService;

    // ── ASYNC: used during onboarding (fire-and-forget after main tx commits) ──

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendWelcomeEmailAsync(Long stagiaireId, String email, String fullName,
            String rawPwd, String sujet, String dateDebut,
            String dateFin, String encadrantNom) {
        String err = emailService.sendWelcomeEmail(
                email, fullName, rawPwd, sujet, dateDebut, dateFin, encadrantNom);
        logResult(stagiaireId, err);
    }

    // ── SYNC: used for manual resend (caller needs the success/failure result) ──

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String syncSendWelcomeEmail(Long stagiaireId, String email, String fullName,
            String pwd, String sujet, String dateDebut,
            String dateFin, String encadrantNom) {
        String err = emailService.sendWelcomeEmail(
                email, fullName, pwd, sujet, dateDebut, dateFin, encadrantNom);
        logResult(stagiaireId, err);
        return err;
    }

    private void logResult(Long stagiaireId, String err) {
        if (err != null)
            log.warn("Email stagiaire {} non envoyé: {}", stagiaireId, err);
        else
            log.info("Email stagiaire {} envoyé avec succès", stagiaireId);
    }
}
