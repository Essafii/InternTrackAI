package com.internship.platform.service;

import com.internship.platform.dto.reporting.ReportJobDto;
import com.internship.platform.entity.ReportJob;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.User;
import com.internship.platform.entity.enums.StatutRapport;
import com.internship.platform.entity.enums.StatutStagiaire;
import com.internship.platform.entity.enums.TypeRapport;
import com.internship.platform.exception.BusinessException;
import com.internship.platform.exception.ResourceNotFoundException;
import com.internship.platform.repository.ReportJobRepository;
import com.internship.platform.repository.StagiaireRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private final ReportJobRepository reportJobRepository;
    private final StagiaireRepository stagiaireRepository;

    @Value("${app.storage.upload-dir:./uploads}")
    private String uploadDir;

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
        job = reportJobRepository.save(job);

        try {
            job.setStatut(StatutRapport.EN_COURS);
            byte[] pdf = generatePdf(job);

            Path dir = Paths.get(uploadDir, "reports").toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + ".pdf";
            Files.write(dir.resolve(filename), pdf);

            job.setFilePath("reports/" + filename);
            job.setStatut(StatutRapport.TERMINE);
            job.setDateGeneration(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Erreur generation rapport: {}", e.getMessage());
            job.setStatut(StatutRapport.ERREUR);
            job.setErreur(e.getMessage());
        }

        return toDto(reportJobRepository.save(job));
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

    private byte[] generatePdf(ReportJob job) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

        try {
            doc.add(new Paragraph("InternTrackAI - Rapport", titleFont));
            doc.add(new Paragraph("Type: " + job.getType(), sectionFont));
            doc.add(new Paragraph("Demandeur: " + job.getDemandeur().getFirstName()
                    + " " + job.getDemandeur().getLastName(), bodyFont));
            doc.add(new Paragraph("Date: " + LocalDateTime.now(), bodyFont));
            doc.add(Chunk.NEWLINE);

            if (job.getType() == TypeRapport.RAPPORT_STAGIAIRE && job.getStagiaireCibleId() != null) {
                stagiaireRepository.findById(job.getStagiaireCibleId()).ifPresent(s -> {
                    try {
                        addStagiaireSection(doc, s, sectionFont, bodyFont);
                    } catch (Exception e) {
                        log.warn("Erreur section stagiaire: {}", e.getMessage());
                    }
                });
            } else if (job.getType() == TypeRapport.RAPPORT_CAMPAGNE) {
                List<Stagiaire> actifs = stagiaireRepository.findByStatut(StatutStagiaire.ACTIF);
                doc.add(new Paragraph("Stagiaires actifs: " + actifs.size(), sectionFont));
                for (Stagiaire s : actifs) {
                    doc.add(new Paragraph("- " + s.getUser().getFirstName() + " "
                            + s.getUser().getLastName() + " | " + s.getEquipe(), bodyFont));
                }
            }
        } finally {
            doc.close();
        }
        return baos.toByteArray();
    }

    private void addStagiaireSection(Document doc, Stagiaire s, Font sectionFont, Font bodyFont)
            throws DocumentException {
        doc.add(new Paragraph("Stagiaire: " + s.getUser().getFirstName()
                + " " + s.getUser().getLastName(), sectionFont));
        doc.add(new Paragraph("Equipe: " + s.getEquipe(), bodyFont));
        doc.add(new Paragraph("Sujet: " + s.getSujet(), bodyFont));
        doc.add(new Paragraph("Periode: " + s.getDateDebut() + " -> " + s.getDateFin(), bodyFont));
    }
}
