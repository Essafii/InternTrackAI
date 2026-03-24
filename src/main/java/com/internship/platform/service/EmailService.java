package com.internship.platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.mock:true}")
    private boolean mockMode;

    @Value("${app.mail.from:noreply@internship.com}")
    private String fromEmail;

    @Async
    public void sendNotificationEmail(String to, String subject, String message) {
        if (mockMode) {
            log.info("[MOCK EMAIL] To: {} | Subject: {} | Message: {}", to, subject, message);
            return;
        }
        try {
            Context context = new Context();
            context.setVariable("subject", subject);
            context.setVariable("message", message);
            String html = templateEngine.process("email/notification", context);
            sendHtmlEmail(to, subject, html);
        } catch (Exception e) {
            log.error("Erreur envoi email à {}: {}", to, e.getMessage());
        }
    }

    @Value("${app.mail.platform-url:http://localhost:5173}")
    private String platformUrl;

    /**
     * Sends the welcome email to a newly created stagiaire.
     * Returns null on success, or an error message string on failure.
     */
    public String sendWelcomeEmail(String to, String fullName, String password,
            String sujet, String dateDebut, String dateFin,
            String encadrant) {
        if (mockMode) {
            log.info("[MOCK EMAIL] Welcome → {} | Nom: {} | Sujet: {} | Du: {} Au: {} | URL: {}",
                    to, fullName, sujet, dateDebut, dateFin, platformUrl);
            return null; // success
        }
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("email", to);
            context.setVariable("password", password);
            context.setVariable("sujet", sujet);
            context.setVariable("dateDebut", dateDebut);
            context.setVariable("dateFin", dateFin);
            context.setVariable("encadrant", encadrant);
            context.setVariable("platformUrl", platformUrl);
            String html = templateEngine.process("email/welcome", context);
            sendHtmlEmail(to, "🎓 Bienvenue sur InternShip Platform — " + fullName, html);
            log.info("Email de bienvenue envoyé à {}", to);
            return null; // success
        } catch (Exception e) {
            String errMsg = "Erreur envoi email de bienvenue à " + to + ": " + e.getMessage();
            log.error(errMsg);
            return errMsg;
        }
    }

    @Async
    public void sendLateTaskEmail(String to, String fullName, String taskTitle, String deadline) {
        if (mockMode) {
            log.info("[MOCK EMAIL] LateTask to: {} | Task: {} | Deadline: {}", to, taskTitle, deadline);
            return;
        }
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("taskTitle", taskTitle);
            context.setVariable("deadline", deadline);
            String html = templateEngine.process("email/tache-retard", context);
            sendHtmlEmail(to, "⚠️ Tâche en retard : " + taskTitle, html);
        } catch (Exception e) {
            log.error("Erreur email tache-retard à {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendEvaluationValideeEmail(String to, String fullName, String typeEvaluation, double noteMoyenne) {
        if (mockMode) {
            log.info("[MOCK EMAIL] EvalValidee to: {} | Type: {} | Note: {}", to, typeEvaluation, noteMoyenne);
            return;
        }
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("typeEvaluation", typeEvaluation);
            context.setVariable("noteMoyenne", String.format("%.1f", noteMoyenne));
            String html = templateEngine.process("email/evaluation-validee", context);
            sendHtmlEmail(to, "⭐ Votre évaluation " + typeEvaluation + " a été validée", html);
        } catch (Exception e) {
            log.error("Erreur email evaluation-validee à {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendLivrableStatusEmail(String to, String fullName, String titre, String statut, String commentaire) {
        if (mockMode) {
            log.info("[MOCK EMAIL] LivrableStatus to: {} | Titre: {} | Statut: {}", to, titre, statut);
            return;
        }
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("titre", titre);
            context.setVariable("statut", statut);
            context.setVariable("commentaire", commentaire);
            String html = templateEngine.process("email/livrable-status", context);
            sendHtmlEmail(to, "📄 Livrable " + statut.toLowerCase() + " : " + titre, html);
        } catch (Exception e) {
            log.error("Erreur email livrable-status à {}: {}", to, e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String html) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }
}
