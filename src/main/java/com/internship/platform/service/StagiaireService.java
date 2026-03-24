package com.internship.platform.service;

import com.internship.platform.dto.stagiaire.OnboardingRequest;
import com.internship.platform.dto.stagiaire.StagiaireDto;
import com.internship.platform.dto.stagiaire.StagiaireUpdateRequest;
import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.User;
import com.internship.platform.entity.enums.Role;
import com.internship.platform.entity.enums.StatutStagiaire;
import com.internship.platform.exception.BusinessException;
import com.internship.platform.exception.ResourceNotFoundException;
import com.internship.platform.repository.StagiaireRepository;
import com.internship.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StagiaireService {

    private final StagiaireRepository stagiaireRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final EmailDispatchService emailDispatchService;

    @Transactional
    public StagiaireDto onboardStagiaire(OnboardingRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email déjà utilisé: " + request.getEmail());
        }

        // Create user account
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.STAGIAIRE)
                .phone(request.getPhone())
                .enabled(true)
                .build();
        userRepository.save(user);

        // Find encadrant
        User encadrant = null;
        if (request.getEncadrantId() != null) {
            encadrant = userRepository.findById(request.getEncadrantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Encadrant", request.getEncadrantId()));
            if (encadrant.getRole() != Role.ENCADRANT) {
                throw new BusinessException("L'utilisateur désigné n'est pas un encadrant");
            }
        }

        Stagiaire stagiaire = Stagiaire.builder()
                .user(user)
                .encadrant(encadrant)
                .sujet(request.getSujet())
                .equipe(request.getEquipe())
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .etablissement(request.getEtablissement())
                .niveauEtude(request.getNiveauEtude())
                .specialite(request.getSpecialite())
                .description(request.getDescription())
                .statut(StatutStagiaire.ACTIF)
                .build();

        stagiaireRepository.save(stagiaire);
        auditService.log("ONBOARDING", "Stagiaire", stagiaire.getId(), "Stagiaire onboardé: " + user.getEmail());
        notificationService.sendOnboardingNotification(stagiaire);

        // ── Email envoyé via EmailDispatchService (@Async + REQUIRES_NEW dans un bean
        // séparé) ──
        final Long stagId = stagiaire.getId();
        final String rawPwd = request.getPassword();
        final String enc = stagiaire.getEncadrant() != null ? stagiaire.getEncadrant().getFullName() : null;
        emailDispatchService.sendWelcomeEmailAsync(stagId, user.getEmail(), user.getFullName(), rawPwd,
                stagiaire.getSujet(), stagiaire.getDateDebut().toString(),
                stagiaire.getDateFin().toString(), enc);

        return toDto(stagiaire);
    }

    @Transactional(readOnly = true)
    public Page<StagiaireDto> getAllStagiaires(Pageable pageable) {
        return stagiaireRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<StagiaireDto> getStagiairesByEncadrant(Long encadrantId, Pageable pageable) {
        return stagiaireRepository.findByEncadrantId(encadrantId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<StagiaireDto> getStagiairesByStatut(StatutStagiaire statut, Pageable pageable) {
        return stagiaireRepository.findByStatut(statut, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public StagiaireDto getStagiaireById(Long id) {
        return toDto(findById(id));
    }

    @Transactional(readOnly = true)
    public StagiaireDto getStagiaireByUserId(Long userId) {
        return toDto(stagiaireRepository.findByUserId(userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Profil stagiaire non trouvé pour userId: " + userId)));
    }

    @Transactional
    public StagiaireDto updateStagiaire(Long id, StagiaireUpdateRequest request) {
        Stagiaire stagiaire = findById(id);
        if (request.getSujet() != null)
            stagiaire.setSujet(request.getSujet());
        if (request.getEquipe() != null)
            stagiaire.setEquipe(request.getEquipe());
        if (request.getDateFin() != null)
            stagiaire.setDateFin(request.getDateFin());
        if (request.getDescription() != null)
            stagiaire.setDescription(request.getDescription());
        if (request.getStatut() != null) {
            stagiaire.setStatut(request.getStatut());
            auditService.log("CHANGEMENT_STATUT", "Stagiaire", id, "Nouveau statut: " + request.getStatut());
        }
        if (request.getEncadrantId() != null) {
            User encadrant = userRepository.findById(request.getEncadrantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Encadrant", request.getEncadrantId()));
            stagiaire.setEncadrant(encadrant);
        }
        return toDto(stagiaireRepository.save(stagiaire));
    }

    @Transactional
    public void deleteStagiaire(Long id) {
        Stagiaire stagiaire = findById(id);
        stagiaire.setStatut(StatutStagiaire.ARCHIVE);
        stagiaireRepository.save(stagiaire);
        auditService.log("ARCHIVE", "Stagiaire", id, "Stagiaire archivé");
    }

    @Transactional(readOnly = true)
    public Stagiaire findById(Long id) {
        return stagiaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire", id));
    }

    /**
     * Vérifie que le stagiaire {@code stagiaireId} est bien supervisé par
     * l'encadrant {@code encadrantUserId}. Lance
     * {@link org.springframework.security.access.AccessDeniedException}
     * (→ HTTP 403) dans le cas contraire.
     */
    @Transactional(readOnly = true)
    public void assertEncadrantOwns(Long stagiaireId, Long encadrantUserId) {
        Stagiaire s = findById(stagiaireId);
        if (s.getEncadrant() == null || !s.getEncadrant().getId().equals(encadrantUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Accès refusé : ce stagiaire n'est pas dans votre équipe.");
        }
    }

    /**
     * Vérifie qu'un stagiaire ne consulte que sa propre fiche.
     */
    @Transactional(readOnly = true)
    public void assertStagiaireOwns(Long stagiaireId, Long callerUserId) {
        Stagiaire s = findById(stagiaireId);
        if (!s.getUser().getId().equals(callerUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Accès refusé : vous ne pouvez consulter que votre propre fiche.");
        }
    }

    /**
     * Renvoi manuel de l'email de bienvenue pour un stagiaire.
     * 
     * @param id          identifiant du stagiaire
     * @param rawPassword nouveau mot de passe temporaire à envoyer (null = utiliser
     *                    "[mot de passe confidentiel]")
     */
    @Transactional
    public void resendWelcomeEmail(Long id, String rawPassword) {
        Stagiaire stagiaire = findById(id);
        String passwordToSend = rawPassword != null ? rawPassword
                : "[Utilisez votre mot de passe actuel ou demandez une réinitialisation]";
        String emailError = emailDispatchService.syncSendWelcomeEmail(
                stagiaire.getId(),
                stagiaire.getUser().getEmail(),
                stagiaire.getUser().getFullName(),
                passwordToSend,
                stagiaire.getSujet(),
                stagiaire.getDateDebut().toString(),
                stagiaire.getDateFin().toString(),
                stagiaire.getEncadrant() != null ? stagiaire.getEncadrant().getFullName() : null);
        if (emailError != null) {
            throw new BusinessException("Echec renvoi email: " + emailError);
        }
    }

    public StagiaireDto toDto(Stagiaire s) {
        StagiaireDto dto = new StagiaireDto();
        dto.setId(s.getId());
        dto.setUserId(s.getUser().getId());
        dto.setEmail(s.getUser().getEmail());
        dto.setFullName(s.getUser().getFullName());
        dto.setSujet(s.getSujet());
        dto.setEquipe(s.getEquipe());
        dto.setDateDebut(s.getDateDebut());
        dto.setDateFin(s.getDateFin());
        dto.setStatut(s.getStatut().name());
        dto.setEtablissement(s.getEtablissement());
        dto.setNiveauEtude(s.getNiveauEtude());
        dto.setSpecialite(s.getSpecialite());
        if (s.getEncadrant() != null) {
            dto.setEncadrantId(s.getEncadrant().getId());
            dto.setEncadrantNom(s.getEncadrant().getFullName());
        }
        return dto;
    }
}
