package com.internship.platform.dto.livrable;

import com.internship.platform.entity.enums.StatutLivrable;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LivrableValidationRequest {
    @NotNull
    private StatutLivrable statut;
    private String commentaire;
}
