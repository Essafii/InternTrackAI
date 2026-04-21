package com.internship.platform.dto.reporting;

import com.internship.platform.entity.enums.StatutRapport;
import com.internship.platform.entity.enums.TypeRapport;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportJobDto {
    private Long id;
    private TypeRapport type;
    private StatutRapport statut;
    private String filePath;
    private LocalDateTime dateGeneration;
}
