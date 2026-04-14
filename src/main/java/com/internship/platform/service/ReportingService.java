package com.internship.platform.service;

import com.internship.platform.entity.ReportJob;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.User;
import com.internship.platform.entity.enums.EtatTache;
import com.internship.platform.entity.enums.StatutRapport;
import com.internship.platform.entity.enums.TypeRapport;
import com.internship.platform.exception.ResourceNotFoundException;
import com.internship.platform.repository.EvaluationRepository;
import com.internship.platform.repository.ReportJobRepository;
import com.internship.platform.repository.StagiaireRepository;
import com.internship.platform.repository.TacheRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private final ReportJobRepository reportJobRepository;
    private final StagiaireRepository stagiaireRepository;
    private final AbsenceService absenceService;
    private final EvaluationRepository evaluationRepository;
    private final TacheRepository tacheRepository;

    @Value("${app.storage.upload-dir:./uploads}")
    private String uploadDir;

    @Transactional
    public ReportJob createReportJob(User demandeur, TypeRapport type, Long stagiaireCibleId) {
        ReportJob job = ReportJob.builder()
                .demandeur(demandeur)
                .type(type)
                .statut(StatutRapport.EN_ATTENTE)
                .stagiaireCibleId(stagiaireCibleId)
                .build();
        reportJobRepository.save(job);
        generateReportAsync(job.getId());
        return job;
    }

    @Async
    public void generateReportAsync(Long jobId) {
        ReportJob job = reportJobRepository.findById(jobId).orElseThrow();
        job.setStatut(StatutRapport.EN_COURS);
        reportJobRepository.save(job);
        try {
            String filePath = switch (job.getType()) {
                case RAPPORT_STAGIAIRE -> generateRapportStagiaire(job.getStagiaireCibleId());
                case RAPPORT_CAMPAGNE -> generateRapportCampagne();
                case RAPPORT_ABSENCES -> generateRapportAbsences(job.getStagiaireCibleId());
                case DECISION_RH -> generateDecisionRh(job.getStagiaireCibleId());
            };
            job.setFilePath(filePath);
            job.setStatut(StatutRapport.TERMINE);
            job.setDateGeneration(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Erreur génération rapport: {}", e.getMessage());
            job.setStatut(StatutRapport.ERREUR);
            job.setErreur(e.getMessage());
        }
        reportJobRepository.save(job);
    }

    private String generateRapportStagiaire(Long stagiaireId) throws Exception {
        Stagiaire s = stagiaireRepository.findById(stagiaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire", stagiaireId));
        String filename = "rapport_stagiaire_" + stagiaireId + "_" + UUID.randomUUID() + ".pdf";
        Path dir = Paths.get(uploadDir, "rapports").toAbsolutePath();
        Files.createDirectories(dir);
        String filePath = "rapports/" + filename;

        Document doc = new Document();
        PdfWriter.getInstance(doc, new FileOutputStream(dir.resolve(filename).toFile()));
        doc.open();
        doc.add(new Paragraph("RAPPORT DE STAGE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Stagiaire: " + s.getUser().getFullName()));
        doc.add(new Paragraph("Email: " + s.getUser().getEmail()));
        doc.add(new Paragraph("Sujet: " + s.getSujet()));
        doc.add(new Paragraph("Équipe: " + (s.getEquipe() != null ? s.getEquipe() : "N/A")));
        doc.add(new Paragraph("Période: " + s.getDateDebut() + " → " + s.getDateFin()));
        doc.add(new Paragraph("Statut: " + s.getStatut().name()));
        if (s.getEncadrant() != null) {
            doc.add(new Paragraph("Encadrant: " + s.getEncadrant().getFullName()));
        }
        doc.add(new Paragraph(" "));
        double assiduite = absenceService.calculerTauxAssiduite(stagiaireId);
        doc.add(new Paragraph("Taux d'assiduité: " + String.format("%.1f", assiduite) + "%"));
        doc.add(new Paragraph(
                "Généré le: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        doc.close();
        return filePath;
    }

    /**
     * Synchronous byte[] PDF export — rich report for a single stagiaire,
     * including evaluations, task statistics, and absence summary.
     */
    public byte[] generateRapportStagiaireBytes(Long stagiaireId) throws Exception {
        Stagiaire s = stagiaireRepository.findById(stagiaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire", stagiaireId));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // ── Fonts ────────────────────────────────────────────────
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(96, 52, 148));
        Font h2Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(96, 52, 148));
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);

        // ── Title ─────────────────────────────────────────────────
        Paragraph title = new Paragraph("RAPPORT DE STAGE INDIVIDUEL", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        doc.add(title);

        Paragraph generated = new Paragraph(
                "Généré le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")),
                footerFont);
        generated.setAlignment(Element.ALIGN_CENTER);
        generated.setSpacingAfter(18);
        doc.add(generated);

        doc.add(new Chunk(new LineSeparator()));
        doc.add(Chunk.NEWLINE);

        // ── Stagiaire Info ────────────────────────────────────────
        addSectionTitle(doc, h2Font, "1. Informations Générales");
        addKeyValue(doc, labelFont, valueFont, "Nom", s.getUser().getFullName());
        addKeyValue(doc, labelFont, valueFont, "Email", s.getUser().getEmail());
        addKeyValue(doc, labelFont, valueFont, "Sujet", s.getSujet());
        addKeyValue(doc, labelFont, valueFont, "Équipe", s.getEquipe() != null ? s.getEquipe() : "—");
        addKeyValue(doc, labelFont, valueFont, "Établissement",
                s.getEtablissement() != null ? s.getEtablissement() : "—");
        addKeyValue(doc, labelFont, valueFont, "Spécialité", s.getSpecialite() != null ? s.getSpecialite() : "—");
        addKeyValue(doc, labelFont, valueFont, "Période", s.getDateDebut() + "  →  " + s.getDateFin());
        addKeyValue(doc, labelFont, valueFont, "Statut", s.getStatut().name());
        if (s.getEncadrant() != null) {
            addKeyValue(doc, labelFont, valueFont, "Encadrant", s.getEncadrant().getFullName());
        }
        doc.add(Chunk.NEWLINE);

        // ── Assiduité ─────────────────────────────────────────────
        addSectionTitle(doc, h2Font, "2. Assiduité");
        double assiduite = absenceService.calculerTauxAssiduite(stagiaireId);
        long totalAbsences = s.getAbsences().size();
        long absJustifiees = s.getAbsences().stream().filter(a -> a.isJustifiee()).count();
        addKeyValue(doc, labelFont, valueFont, "Taux d'assiduité", String.format("%.1f%%", assiduite));
        addKeyValue(doc, labelFont, valueFont, "Total absences", String.valueOf(totalAbsences));
        addKeyValue(doc, labelFont, valueFont, "Absences justifiées", absJustifiees + " / " + totalAbsences);
        doc.add(Chunk.NEWLINE);

        // ── Tâches ────────────────────────────────────────────────
        addSectionTitle(doc, h2Font, "3. Statistiques des Tâches");
        long tTotal = tacheRepository.countByEtat(stagiaireId, EtatTache.A_FAIRE)
                + tacheRepository.countByEtat(stagiaireId, EtatTache.EN_COURS)
                + tacheRepository.countByEtat(stagiaireId, EtatTache.TERMINE)
                + tacheRepository.countByEtat(stagiaireId, EtatTache.EN_RETARD);
        addKeyValue(doc, labelFont, valueFont, "Total tâches", String.valueOf(tTotal));
        addKeyValue(doc, labelFont, valueFont, "À faire",
                String.valueOf(tacheRepository.countByEtat(stagiaireId, EtatTache.A_FAIRE)));
        addKeyValue(doc, labelFont, valueFont, "En cours",
                String.valueOf(tacheRepository.countByEtat(stagiaireId, EtatTache.EN_COURS)));
        addKeyValue(doc, labelFont, valueFont, "Terminées",
                String.valueOf(tacheRepository.countByEtat(stagiaireId, EtatTache.TERMINE)));
        addKeyValue(doc, labelFont, valueFont, "En retard",
                String.valueOf(tacheRepository.countByEtat(stagiaireId, EtatTache.EN_RETARD)));
        doc.add(Chunk.NEWLINE);

        // ── Évaluations ───────────────────────────────────────────
        var evaluations = evaluationRepository.findByStagiaireId(stagiaireId);
        addSectionTitle(doc, h2Font, "4. Évaluations (" + evaluations.size() + ")");
        if (evaluations.isEmpty()) {
            doc.add(new Paragraph("Aucune évaluation enregistrée.", valueFont));
        } else {
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(8);
            String[] evalHeaders = { "Type", "Mois", "Note Moy.", "Validée" };
            for (String h : evalHeaders) {
                PdfPCell cell = new PdfPCell(new Phrase(h, labelFont));
                cell.setBackgroundColor(new Color(235, 228, 248));
                cell.setPadding(6);
                table.addCell(cell);
            }
            for (var ev : evaluations) {
                table.addCell(new PdfPCell(new Phrase(ev.getType().name(), valueFont)));
                table.addCell(new PdfPCell(new Phrase(ev.getMois() != null ? "M" + ev.getMois() : "—", valueFont)));
                table.addCell(new PdfPCell(new Phrase(String.format("%.1f / 20", ev.getNoteMoyenne()), valueFont)));
                table.addCell(new PdfPCell(new Phrase(ev.isValidee() ? "Oui" : "Non", valueFont)));
            }
            doc.add(table);
        }
        doc.add(Chunk.NEWLINE);

        // ── Footer ─────────────────────────────────────────────────
        doc.add(new Chunk(new LineSeparator()));
        Paragraph footer = new Paragraph("InternShip Platform – Document confidentiel", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(8);
        doc.add(footer);

        doc.close();
        return out.toByteArray();
    }

    // ── Helpers ─────────────────────────────────────────────────
    private void addSectionTitle(Document doc, Font font, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(4);
        p.setSpacingAfter(8);
        doc.add(p);
    }

    private void addKeyValue(Document doc, Font labelFont, Font valueFont, String key, String value)
            throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Chunk(key + ": ", labelFont));
        p.add(new Chunk(value != null ? value : "—", valueFont));
        p.setSpacingAfter(3);
        doc.add(p);
    }

    private String generateRapportCampagne() throws Exception {
        List<Stagiaire> stagiaires = stagiaireRepository.findAll();
        String filename = "rapport_campagne_" + UUID.randomUUID() + ".pdf";
        Path dir = Paths.get(uploadDir, "rapports").toAbsolutePath();
        Files.createDirectories(dir);
        String filePath = "rapports/" + filename;

        Document doc = new Document();
        PdfWriter.getInstance(doc, new FileOutputStream(dir.resolve(filename).toFile()));
        doc.open();
        doc.add(new Paragraph("RAPPORT DE CAMPAGNE DE STAGES", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Total stagiaires: " + stagiaires.size()));
        doc.add(new Paragraph(" "));
        for (Stagiaire s : stagiaires) {
            doc.add(new Paragraph(
                    "- " + s.getUser().getFullName() + " | " + s.getSujet() + " | " + s.getStatut().name()));
        }
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(
                "Généré le: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        doc.close();
        return filePath;
    }

    private String generateRapportAbsences(Long stagiaireId) throws Exception {
        Stagiaire s = stagiaireRepository.findById(stagiaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire", stagiaireId));
        String filename = "rapport_absences_" + stagiaireId + "_" + UUID.randomUUID() + ".pdf";
        Path dir = Paths.get(uploadDir, "rapports").toAbsolutePath();
        Files.createDirectories(dir);
        String filePath = "rapports/" + filename;

        Document doc = new Document();
        PdfWriter.getInstance(doc, new FileOutputStream(dir.resolve(filename).toFile()));
        doc.open();
        doc.add(new Paragraph("RAPPORT D'ABSENCES", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
        doc.add(new Paragraph("Stagiaire: " + s.getUser().getFullName()));
        doc.add(new Paragraph(
                "Taux d'assiduité: " + String.format("%.1f", absenceService.calculerTauxAssiduite(stagiaireId)) + "%"));
        doc.add(new Paragraph(" "));
        s.getAbsences().forEach(a -> {
            try {
                doc.add(new Paragraph(
                        "- " + a.getDateAbsence() + " | " + a.getType().name() + " | Justifiée: " + a.isJustifiee()));
            } catch (DocumentException e) {
                log.error("PDF error", e);
            }
        });
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(
                "Généré le: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        doc.close();
        return filePath;
    }

    private String generateDecisionRh(Long stagiaireId) throws Exception {
        Stagiaire s = stagiaireRepository.findById(stagiaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire", stagiaireId));
        String filename = "decision_rh_" + stagiaireId + "_" + UUID.randomUUID() + ".pdf";
        Path dir = Paths.get(uploadDir, "rapports").toAbsolutePath();
        Files.createDirectories(dir);
        String filePath = "rapports/" + filename;

        Document doc = new Document();
        PdfWriter.getInstance(doc, new FileOutputStream(dir.resolve(filename).toFile()));
        doc.open();
        doc.add(new Paragraph("DÉCISION RH", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Stagiaire: " + s.getUser().getFullName()));
        doc.add(new Paragraph("Statut final: " + s.getStatut().name()));
        doc.add(new Paragraph("Période: " + s.getDateDebut() + " → " + s.getDateFin()));
        doc.add(new Paragraph(
                "Taux d'assiduité: " + String.format("%.1f", absenceService.calculerTauxAssiduite(stagiaireId)) + "%"));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Décision: " + (s.getStatut().name().equals("TERMINE") ? "Stage validé" : "À évaluer")));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(
                "Généré le: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        doc.close();
        return filePath;
    }

    public ReportJob getJobById(Long id) {
        return reportJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReportJob", id));
    }

    public Page<ReportJob> getJobsByUser(Long userId, Pageable pageable) {
        return reportJobRepository.findByDemandeurIdOrderByCreatedAtDesc(userId, pageable);
    }
}
