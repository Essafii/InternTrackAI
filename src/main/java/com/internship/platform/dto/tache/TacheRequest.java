package com.internship.platform.dto.tache;

import com.internship.platform.entity.enums.EtatTache;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TacheRequest {
    @NotBlank
    private String titre;
    private String description;
    private LocalDate dateDebut;
    private LocalDate deadline;
    private Integer priorite;
    private EtatTache etat;
    private String commentaire;
}
