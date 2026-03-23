package com.internship.platform.seed;

import com.internship.platform.entity.Stagiaire;
import com.internship.platform.entity.Tache;
import com.internship.platform.entity.User;
import com.internship.platform.entity.enums.EtatTache;
import com.internship.platform.entity.enums.Role;
import com.internship.platform.entity.enums.StatutStagiaire;
import com.internship.platform.repository.StagiaireRepository;
import com.internship.platform.repository.TacheRepository;
import com.internship.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StagiaireRepository stagiaireRepository;
    private final TacheRepository tacheRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Base de données déjà initialisée, seed ignoré.");
            return;
        }
        log.info("Initialisation des données de test...");

        // --- ADMIN ---
        User admin = User.builder()
                .email("admin@internship.com")
                .password(passwordEncoder.encode("Admin1234!"))
                .firstName("Super").lastName("Admin")
                .role(Role.ADMIN).enabled(true).build();
        userRepository.save(admin);

        // --- RH ---
        User rh = User.builder()
                .email("rh@internship.com")
                .password(passwordEncoder.encode("Rh1234!"))
                .firstName("Marie").lastName("Dupont")
                .role(Role.RH).department("Ressources Humaines").enabled(true).build();
        userRepository.save(rh);

        // --- ENCADRANT 1 ---
        User enc1 = User.builder()
                .email("encadrant1@internship.com")
                .password(passwordEncoder.encode("Enc1234!"))
                .firstName("Ahmed").lastName("Benali")
                .role(Role.ENCADRANT).department("Développement Logiciel").enabled(true).build();
        userRepository.save(enc1);

        // --- ENCADRANT 2 ---
        User enc2 = User.builder()
                .email("encadrant2@internship.com")
                .password(passwordEncoder.encode("Enc1234!"))
                .firstName("Sarah").lastName("Morin")
                .role(Role.ENCADRANT).department("Data Science").enabled(true).build();
        userRepository.save(enc2);

        // --- STAGIAIRE 1 ---
        User u1 = User.builder()
                .email("stagiaire1@internship.com")
                .password(passwordEncoder.encode("Stage1234!"))
                .firstName("Karim").lastName("Amrani")
                .role(Role.STAGIAIRE).enabled(true).build();
        userRepository.save(u1);
        Stagiaire s1 = Stagiaire.builder()
                .user(u1).encadrant(enc1)
                .sujet("Développement d'une API REST Spring Boot")
                .equipe("Équipe Alpha")
                .dateDebut(LocalDate.now().minusMonths(2))
                .dateFin(LocalDate.now().plusMonths(4))
                .etablissement("ENSIAS").niveauEtude("Bac+5").specialite("Génie Logiciel")
                .statut(StatutStagiaire.ACTIF).build();
        stagiaireRepository.save(s1);

        // --- STAGIAIRE 2 ---
        User u2 = User.builder()
                .email("stagiaire2@internship.com")
                .password(passwordEncoder.encode("Stage1234!"))
                .firstName("Fatima").lastName("Zahra")
                .role(Role.STAGIAIRE).enabled(true).build();
        userRepository.save(u2);
        Stagiaire s2 = Stagiaire.builder()
                .user(u2).encadrant(enc2)
                .sujet("Analyse de données avec Python et ML")
                .equipe("Équipe Beta")
                .dateDebut(LocalDate.now().minusMonths(3))
                .dateFin(LocalDate.now().plusMonths(3))
                .etablissement("FSR").niveauEtude("Bac+5").specialite("Data Science")
                .statut(StatutStagiaire.ACTIF).build();
        stagiaireRepository.save(s2);

        // --- STAGIAIRE 3 (en retard) ---
        User u3 = User.builder()
                .email("stagiaire3@internship.com")
                .password(passwordEncoder.encode("Stage1234!"))
                .firstName("Youssef").lastName("El Amrani")
                .role(Role.STAGIAIRE).enabled(true).build();
        userRepository.save(u3);
        Stagiaire s3 = Stagiaire.builder()
                .user(u3).encadrant(enc1)
                .sujet("Mise en place d'un pipeline CI/CD")
                .equipe("Équipe Alpha")
                .dateDebut(LocalDate.now().minusMonths(4))
                .dateFin(LocalDate.now().plusMonths(2))
                .etablissement("UM5").niveauEtude("Bac+5").specialite("DevOps")
                .statut(StatutStagiaire.EN_RETARD).build();
        stagiaireRepository.save(s3);

        // --- TÂCHES pour stagiaire 1 ---
        tacheRepository.save(Tache.builder()
                .stagiaire(s1).titre("Analyse des besoins")
                .description("Analyser les besoins du projet avec le client")
                .dateDebut(LocalDate.now().minusMonths(2))
                .deadline(LocalDate.now().minusMonths(1))
                .priorite(1).etat(EtatTache.TERMINE)
                .dateCompletion(LocalDate.now().minusMonths(1).plusDays(2)).build());

        tacheRepository.save(Tache.builder()
                .stagiaire(s1).titre("Conception API")
                .description("Concevoir les endpoints REST")
                .dateDebut(LocalDate.now().minusWeeks(3))
                .deadline(LocalDate.now().plusWeeks(1))
                .priorite(2).etat(EtatTache.EN_COURS).build());

        tacheRepository.save(Tache.builder()
                .stagiaire(s1).titre("Tests unitaires")
                .description("Écrire les tests unitaires")
                .dateDebut(LocalDate.now())
                .deadline(LocalDate.now().plusMonths(1))
                .priorite(3).etat(EtatTache.A_FAIRE).build());

        log.info("✅ Données de seed chargées avec succès!");
        log.info("Comptes créés:");
        log.info("  admin@internship.com / Admin1234! (ADMIN)");
        log.info("  rh@internship.com / Rh1234! (RH)");
        log.info("  encadrant1@internship.com / Enc1234! (ENCADRANT)");
        log.info("  encadrant2@internship.com / Enc1234! (ENCADRANT)");
        log.info("  stagiaire1@internship.com / Stage1234! (STAGIAIRE)");
        log.info("  stagiaire2@internship.com / Stage1234! (STAGIAIRE)");
        log.info("  stagiaire3@internship.com / Stage1234! (STAGIAIRE)");
    }
}
