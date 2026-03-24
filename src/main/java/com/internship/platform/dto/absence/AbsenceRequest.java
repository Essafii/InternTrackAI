package com.internship.platform.dto.absence;

import com.internship.platform.entity.enums.TypeAbsence;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AbsenceRequest {
    @NotNull
    private LocalDate dateAbsence;

    @NotNull
    private TypeAbsence type;

    private String motif;
    private boolean justifiee;
}
