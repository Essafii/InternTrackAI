package com.internship.platform.dto.reporting;

import com.internship.platform.entity.enums.TypeRapport;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportGenerateRequest {
    @NotNull
    private TypeRapport type;
    private Long stagiaireCibleId;
}
