package com.internship.platform.service;

import com.internship.platform.entity.Absence;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.Tache;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Stagiaires ──────────────────────────────────────────────
    public byte[] exportStagiaires(List<Stagiaire> stagiaires) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Stagiaires");
            CellStyle hStyle = createHeaderStyle(wb);

            String[] headers = { "Nom Complet", "Email", "Sujet Stage", "Encadrant",
                    "Etablissement", "Niveau Etude", "Specialité",
                    "Date Début", "Date Fin", "Statut" };
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(hStyle);
                sheet.setColumnWidth(i, 5500);
            }

            int rowIdx = 1;
            for (Stagiaire s : stagiaires) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(s.getUser() != null ? s.getUser().getFullName() : "");
                row.createCell(1).setCellValue(s.getUser() != null ? s.getUser().getEmail() : "");
                row.createCell(2).setCellValue(s.getSujet() != null ? s.getSujet() : "");
                row.createCell(3).setCellValue(s.getEncadrant() != null ? s.getEncadrant().getFullName() : "");
                row.createCell(4).setCellValue(s.getEtablissement() != null ? s.getEtablissement() : "");
                row.createCell(5).setCellValue(s.getNiveauEtude() != null ? s.getNiveauEtude() : "");
                row.createCell(6).setCellValue(s.getSpecialite() != null ? s.getSpecialite() : "");
                row.createCell(7).setCellValue(s.getDateDebut() != null ? s.getDateDebut().format(DATE_FMT) : "");
                row.createCell(8).setCellValue(s.getDateFin() != null ? s.getDateFin().format(DATE_FMT) : "");
                row.createCell(9).setCellValue(s.getStatut() != null ? s.getStatut().name() : "");
            }

            return toBytes(wb);
        }
    }

    // ── Absences ─────────────────────────────────────────────────
    public byte[] exportAbsences(List<Absence> absences) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Absences");
            CellStyle hStyle = createHeaderStyle(wb);

            String[] headers = { "Stagiaire", "Date Absence", "Type", "Justifiée", "Validée", "Motif",
                    "Commentaire Encadrant" };
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(hStyle);
                sheet.setColumnWidth(i, 5500);
            }

            int rowIdx = 1;
            for (Absence a : absences) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(
                        a.getStagiaire() != null && a.getStagiaire().getUser() != null
                                ? a.getStagiaire().getUser().getFullName()
                                : "");
                row.createCell(1).setCellValue(a.getDateAbsence() != null ? a.getDateAbsence().format(DATE_FMT) : "");
                row.createCell(2).setCellValue(a.getType() != null ? a.getType().name() : "");
                row.createCell(3).setCellValue(a.isJustifiee() ? "Oui" : "Non");
                row.createCell(4).setCellValue(a.isValidee() ? "Oui" : "Non");
                row.createCell(5).setCellValue(a.getMotif() != null ? a.getMotif() : "");
                row.createCell(6).setCellValue(a.getCommentaireEncadrant() != null ? a.getCommentaireEncadrant() : "");
            }

            return toBytes(wb);
        }
    }

    // ── Tâches ────────────────────────────────────────────────────
    public byte[] exportTaches(List<Tache> taches) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Tâches");
            CellStyle hStyle = createHeaderStyle(wb);

            String[] headers = { "Stagiaire", "Titre", "État", "Date Début", "Deadline",
                    "Priorité", "Date Complétion", "Commentaire" };
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(hStyle);
                sheet.setColumnWidth(i, 5500);
            }

            int rowIdx = 1;
            for (Tache t : taches) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(
                        t.getStagiaire() != null && t.getStagiaire().getUser() != null
                                ? t.getStagiaire().getUser().getFullName()
                                : "");
                row.createCell(1).setCellValue(t.getTitre() != null ? t.getTitre() : "");
                row.createCell(2).setCellValue(t.getEtat() != null ? t.getEtat().name() : "");
                row.createCell(3).setCellValue(t.getDateDebut() != null ? t.getDateDebut().format(DATE_FMT) : "");
                row.createCell(4).setCellValue(t.getDeadline() != null ? t.getDeadline().format(DATE_FMT) : "");
                row.createCell(5).setCellValue(t.getPriorite() != null ? t.getPriorite().toString() : "");
                row.createCell(6)
                        .setCellValue(t.getDateCompletion() != null ? t.getDateCompletion().format(DATE_FMT) : "");
                row.createCell(7).setCellValue(t.getCommentaire() != null ? t.getCommentaire() : "");
            }

            return toBytes(wb);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────
    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private byte[] toBytes(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }
}
