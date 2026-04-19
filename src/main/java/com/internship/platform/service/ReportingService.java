package com.internship.platform.service;

import com.internship.platform.dto.reporting.ReportJobDto;
import com.internship.platform.entity.Evaluation;
import com.internship.platform.entity.ReportJob;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.User;
import com.internship.platform.entity.enums.StatutRapport;
import com.internship.platform.entity.enums.StatutStagiaire;
import com.internship.platform.entity.enums.TypeRapport;
import com.internship.platform.exception.BusinessException;
import com.internship.platform.exception.ResourceNotFoundException;
import com.internship.platform.repository.EvaluationRepository;
import com.internship.platform.repository.ReportJobRepository;
import com.internship.platform.repository.StagiaireRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private final ReportJobRepository reportJobRepository;
    private final StagiaireRepository stagiaireRepository;
    private final EvaluationRepository evaluationRepository;
    private final ScoringDomainService scoringDomainService;

    @Value("${app.storage.upload-dir:./uploads}")
    private String uploadDir;

    @Transactional
    public ReportJobDto createJob(User demandeur, TypeRapport type, Long stagiaireCibleId) {
        if (type == TypeRapport.RAPPORT_STAGIAIRE && stagiaireCibleId == null) {
            throw new BusinessException("stagiaireCibleId requis pour RAPPORT_STAGIAIRE");
        }
        ReportJob job = ReportJob.builder()
                .demandeur(demandeur)
                .type(type)
                .statut(StatutRapport.EN_ATTENTE)
                .stagiaireCibleId(stagiaireCibleId)
                .build();
        return toDto(reportJobRepository.save(job));
    }

    /**
     * Runs in a background thread. Transaction boundary is per-save (Spring Data default).
     * Controller must call this AFTER createJob() transaction has committed.
     */
    @Async("reportExecutor")
    @Transactional
    public void generateAsync(Long jobId) {
        ReportJob job = reportJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("ReportJob {} not found — skipping async generation", jobId);
            return;
        }
        job.setStatut(StatutRapport.EN_COURS);
        reportJobRepository.save(job);

        try {
            byte[] pdf = generatePdf(job);
            Path dir = Paths.get(uploadDir, "reports").toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + ".pdf";
            Files.write(dir.resolve(filename), pdf);
            job.setFilePath("reports/" + filename);
            job.setStatut(StatutRapport.TERMINE);
            job.setDateGeneration(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Erreur generation rapport {}: {}", jobId, e.getMessage());
            job.setStatut(StatutRapport.ERREUR);
            job.setErreur(e.getMessage());
        }
        reportJobRepository.save(job);
    }

    public ReportJob getJobById(Long id) {
        return reportJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReportJob", id));
    }

    public Page<ReportJobDto> getJobsByDemandeur(Long userId, Pageable pageable) {
        return reportJobRepository.findByDemandeurIdOrderByCreatedAtDesc(userId, pageable).map(this::toDto);
    }

    public ReportJobDto toDto(ReportJob job) {
        ReportJobDto dto = new ReportJobDto();
        dto.setId(job.getId());
        dto.setType(job.getType());
        dto.setStatut(job.getStatut());
        dto.setFilePath(job.getFilePath());
        dto.setDateGeneration(job.getDateGeneration());
        return dto;
    }

    // ─── PDF generation ──────────────────────────────────────────────────────

    private byte[] generatePdf(ReportJob job) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font titleFont   = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font bodyFont    = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font labelFont   = new Font(Font.HELVETICA, 10, Font.BOLD);

        try {
            doc.add(new Paragraph("InternTrackAI — Rapport", titleFont));
            doc.add(new Paragraph("Type : " + job.getType(), sectionFont));
            doc.add(new Paragraph("Demandeur : "
                    + job.getDemandeur().getFirstName() + " " + job.getDemandeur().getLastName(), bodyFont));
            doc.add(new Paragraph("Date : " + LocalDate.now(), bodyFont));
            doc.add(Chunk.NEWLINE);

            if (job.getType() == TypeRapport.RAPPORT_STAGIAIRE && job.getStagiaireCibleId() != null) {
                Stagiaire s = stagiaireRepository.findById(job.getStagiaireCibleId()).orElse(null);
                if (s != null) {
                    addRichStagiaireSection(doc, s, sectionFont, bodyFont, labelFont);
                }
            } else if (job.getType() == TypeRapport.RAPPORT_CAMPAGNE) {
                addCampagneSection(doc, sectionFont, bodyFont, labelFont);
            }
        } finally {
            doc.close();
        }
        return baos.toByteArray();
    }

    private void addRichStagiaireSection(Document doc, Stagiaire s,
            Font sectionFont, Font bodyFont, Font labelFont) throws DocumentException {

        doc.add(new Paragraph("Profil du stagiaire", sectionFont));
        Table profile = new Table(2);
        profile.setWidth(100);
        addRow(profile, "Nom complet", s.getUser().getFullName(),  labelFont, bodyFont);
        addRow(profile, "Email",       s.getUser().getEmail(),     labelFont, bodyFont);
        addRow(profile, "Équipe",      nvl(s.getEquipe()),          labelFont, bodyFont);
        addRow(profile, "Sujet",       s.getSujet(),                labelFont, bodyFont);
        addRow(profile, "Période",     s.getDateDebut() + " → " + s.getDateFin(), labelFont, bodyFont);
        doc.add(profile);
        doc.add(Chunk.NEWLINE);

        ScoringResult score = scoringDomainService.computeScore(s.getId());
        doc.add(new Paragraph("Évaluation globale", sectionFont));
        Table scoreTable = new Table(2);
        scoreTable.setWidth(100);
        addRow(scoreTable, "Score calculé",            String.format("%.1f / 20", score.scoreCalcule()),       labelFont, bodyFont);
        addRow(scoreTable, "Niveau",                   score.niveau(),                                          labelFont, bodyFont);
        addRow(scoreTable, "Note moyenne évaluations", String.format("%.1f / 20", score.noteMoyenne()),        labelFont, bodyFont);
        addRow(scoreTable, "Taux d'assiduité",         String.format("%.0f %%",   score.tauxAssiduite()),      labelFont, bodyFont);
        addRow(scoreTable, "Tâches terminées",         String.format("%.0f %%",   score.tauxTachesTerminees()), labelFont, bodyFont);
        addRow(scoreTable, "Indicateur risque",        String.format("%.0f / 10", score.risque()),              labelFont, bodyFont);
        doc.add(scoreTable);
        doc.add(Chunk.NEWLINE);

        List<Evaluation> evals = evaluationRepository.findByStagiaireId(s.getId());
        if (!evals.isEmpty()) {
            doc.add(new Paragraph("Évaluations (" + evals.size() + ")", sectionFont));
            Table evalTable = new Table(4);
            evalTable.setWidth(100);
            for (String h : new String[]{"Date", "Type", "Note moy.", "Commentaire"}) {
                Cell hCell = new Cell(new Phrase(h, labelFont));
                hCell.setHeader(true);
                evalTable.addCell(hCell);
            }
            evalTable.endHeaders();
            for (Evaluation e : evals) {
                evalTable.addCell(new Cell(new Phrase(e.getDateEvaluation().toString(), bodyFont)));
                evalTable.addCell(new Cell(new Phrase(e.getType().name(), bodyFont)));
                evalTable.addCell(new Cell(new Phrase(
                        e.getNoteMoyenne() != null ? String.format("%.1f", e.getNoteMoyenne()) : "—", bodyFont)));
                evalTable.addCell(new Cell(new Phrase(nvl(e.getCommentaire()), bodyFont)));
            }
            doc.add(evalTable);
        }
    }

    private void addCampagneSection(Document doc, Font sectionFont, Font bodyFont, Font labelFont)
            throws DocumentException {
        List<Stagiaire> actifs = stagiaireRepository.findByStatut(StatutStagiaire.ACTIF);
        doc.add(new Paragraph("Stagiaires actifs : " + actifs.size(), sectionFont));
        if (!actifs.isEmpty()) {
            Table t = new Table(3);
            t.setWidth(100);
            for (String h : new String[]{"Nom", "Équipe", "Score / 20"}) {
                Cell c = new Cell(new Phrase(h, labelFont));
                c.setHeader(true);
                t.addCell(c);
            }
            t.endHeaders();
            for (Stagiaire s : actifs) {
                ScoringResult sr = scoringDomainService.computeScore(s.getId());
                t.addCell(new Cell(new Phrase(s.getUser().getFullName(), bodyFont)));
                t.addCell(new Cell(new Phrase(nvl(s.getEquipe()), bodyFont)));
                t.addCell(new Cell(new Phrase(String.format("%.1f", sr.scoreCalcule()), bodyFont)));
            }
            doc.add(t);
        }
    }

    private void addRow(Table t, String label, String value, Font labelFont, Font bodyFont)
            throws BadElementException {
        t.addCell(new Cell(new Phrase(label, labelFont)));
        t.addCell(new Cell(new Phrase(value, bodyFont)));
    }

    private String nvl(String s) {
        return s != null ? s : "—";
    }
}
