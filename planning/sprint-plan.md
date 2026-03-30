# Sprint Plan — InternTrackAI
## DXC Technology Morocco × ISTIC, PFE 2026

**Methodology:** Agile Scrum — 2-week sprints
**Team:** Youssef, Mehdi Essafi, Abdelhafid Meskour, Zakaria EL aoumari, Chadi
**Supervisor:** Naoufal LEBHAR
**Total:** 8 sprints (S0–S7) · 90 US · 551 SP · 3 Releases

---

## Sprint Status Overview

| Sprint | Period | SP | Status | Release |
|--------|--------|----|--------|---------|
| S0 | 01/03 → 14/03/2026 | 34 | ⚠️ En cours (dépassé) | — |
| S1 | 15/03 → 28/03/2026 | 32 | ⚠️ En cours (dépassé) | R1 |
| S2 | 29/03 → 11/04/2026 | 26 | 🔜 À venir | — |
| S3 | 12/04 → 25/04/2026 | 72 | 🔜 À venir | R2 |
| S4 | 26/04 → 09/05/2026 | 68 | 🔜 À venir | — |
| S5 | 10/05 → 23/05/2026 | 92 | 🔜 À venir | R3 |
| S6 | 24/05 → 06/06/2026 | 144 | 🔜 À venir | — |
| S7 | 07/06 → 20/06/2026 | 83 | 🔜 À venir | — |

---

## S0 — DevSecOps & Infrastructure Setup
**01/03/2026 → 14/03/2026 · 34 SP · EPIC 10**
**Status:** ⚠️ Clôture en retard — backend partiellement implémenté, pipeline incomplet

### Objectifs
- Mise en place du repo GitHub + stratégie de branches (GitFlow)
- Docker + Docker Compose opérationnel
- Pipeline Jenkins fonctionnel
- Kubernetes Minikube configuré
- Monitoring Prometheus + Grafana

### US du Sprint
| US | Description | SP | Statut |
|----|-------------|-----|--------|
| US-71 | Mise en place repo GitHub + branches | 3 | ✅ Fait |
| US-72 | Dockerfiles backend + frontend | 5 | ⚠️ Partiel (Dockerfiles présents mais non fonctionnels) |
| US-73 | Docker Compose avec PostgreSQL | 5 | ⚠️ Partiel (PostgreSQL manquait) |
| US-74 | Pipeline Jenkins CI/CD | 8 | ⚠️ Partiel (Jenkinsfile présent, stages vides) |
| US-75 | Déploiement Kubernetes Minikube | 8 | ⚠️ Partiel (manifests présents, secrets manquants) |
| US-76 | Monitoring Prometheus + Grafana | 5 | ❌ Non démarré |

### Livrables attendus
- [x] Repository GitHub avec branches (main, develop, feature/*)
- [x] docker-compose.yml avec tous les services
- [ ] Pipeline Jenkins end-to-end (build → test → scan → push → deploy)
- [ ] Dashboard Grafana opérationnel

---

## S1 — Onboarding & Authentification
**15/03/2026 → 28/03/2026 · 32 SP · EPIC 1**
**Status:** ⚠️ Clôture en retard — backend ~70% fait, frontend = 0%

### Objectifs
- Authentification JWT avec refresh tokens
- Gestion des rôles RBAC (STAGIAIRE, ENCADRANT, RH, ADMIN)
- Onboarding des stagiaires par RH
- Gestion des utilisateurs

### US du Sprint
| US | Description | SP | Statut |
|----|-------------|-----|--------|
| US-01 | Inscription/création de compte | 5 | ✅ Fait (AuthController + register) |
| US-02 | Connexion + JWT | 5 | ✅ Fait (AuthController + login/refresh) |
| US-03 | Gestion des rôles RBAC | 5 | ✅ Fait (SecurityConfig + @PreAuthorize) |
| US-04 | Onboarding stagiaire (RH) | 8 | ✅ Fait (StagiaireController + onboarding) |
| US-05 | Profil utilisateur | 5 | ✅ Fait (UserController) |
| US-06 | Email de bienvenue automatique | 4 | ✅ Fait (EmailService + templates) |

### Livrables attendus
- [x] API auth : POST /auth/login, /auth/register, /auth/refresh
- [x] API stagiaires : POST /stagiaires/onboarding
- [x] RBAC enforced sur tous les endpoints
- [ ] Interface Angular login/register (non démarrée)

---

## S2 — Gestion des Stagiaires & Absences
**29/03/2026 → 11/04/2026 · 26 SP · EPIC 2**
**Status:** 🔜 Démarre demain (29/03)

### Objectifs
- CRUD complet stagiaires
- Suivi des absences + calcul taux d'assiduité
- Alertes automatiques seuil 80% (RG07)
- Upload justificatifs

### US du Sprint
| US | Description | SP | Propriétaire |
|----|-------------|-----|--------------|
| US-07 | Liste/filtrage des stagiaires | 3 | |
| US-08 | Fiche détaillée stagiaire | 3 | |
| US-09 | Mise à jour profil stagiaire | 3 | |
| US-10 | Enregistrement absence | 5 | |
| US-11 | Calcul taux assiduité | 5 | |
| US-12 | Alerte assiduité < 80% | 4 | |
| US-13 | Upload justificatif absence | 3 | |

### Livrables attendus
- [ ] API absences : POST /absences, GET /absences/stagiaire/{id}
- [ ] Calcul assiduité automatique opérationnel
- [ ] Alertes notifications envoyées en temps réel (SSE)

---

## S3 — Tâches, Livrables & Évaluations
**12/04/2026 → 25/04/2026 · 72 SP · EPICs 3 & 4**
**Status:** 🔜 À venir · Release R2

### Objectifs
- Gestion complète des tâches (CRUD + statuts)
- Upload et validation des livrables (RG05, RG06)
- Évaluations mi-parcours et finales (RG03)
- Notifications automatiques

### Livrables attendus
- [ ] API tâches : /taches/**
- [ ] API livrables : /livrables/** avec upload fichier
- [ ] API évaluations : /evaluations/**
- [ ] Release R2 : backend API complète + tests

---

## S4 — Dashboards & Reporting
**26/04/2026 → 09/05/2026 · 68 SP · EPIC 5**
**Status:** 🔜 À venir

### Objectifs
- Dashboards RH, Encadrant, Stagiaire
- Génération rapports PDF et Excel (RG10, RG11)
- Export données

### Livrables attendus
- [ ] API dashboard : /dashboard/rh, /dashboard/encadrant/{id}, /dashboard/stagiaire/{id}
- [ ] Génération PDF/Excel opérationnelle
- [ ] API classement : /classement

---

## S5 — Scoring IA & Classement
**10/05/2026 → 23/05/2026 · 92 SP · EPIC 6**
**Status:** 🔜 À venir · Release R3

### Objectifs
- Algorithme de scoring pondéré (RG01, RG02)
  - 40% évaluations + 25% assiduité + 25% tâches + 10% risque
- Recalcul automatique hebdomadaire
- Prédiction de risques (heuristique)

### Livrables attendus
- [ ] ClassementService opérationnel + endpoint /classement
- [ ] Job @Scheduled de recalcul hebdomadaire
- [ ] Release R3 : scoring IA + frontend Angular MVP

---

## S6 — Frontend Angular
**24/05/2026 → 06/06/2026 · 144 SP · EPICs 7 & 8**
**Status:** 🔜 À venir — LE sprint critique (frontend = 0%)

### Objectifs
- Application Angular complète
- Pages : Login, Dashboard RH/Encadrant/Stagiaire, Missions, Évaluations
- Intégration avec l'API backend
- Respect de la charte DXC (couleurs : #0E1020, #004AAC, #4995FF, #F6F3F0)

### Livrables attendus
- [ ] Application Angular buildable et déployable
- [ ] Toutes les vues fonctionnelles
- [ ] Authentification JWT côté frontend

---

## S7 — Finalisation, Tests & Soutenance
**07/06/2026 → 20/06/2026 · 83 SP · EPICs 9 & 10**
**Status:** 🔜 À venir

### Objectifs
- Tests end-to-end
- Corrections bugs
- Documentation finale
- Déploiement production
- Préparation soutenance PFE

### Livrables attendus
- [ ] Version finale déployée
- [ ] Documentation technique complète
- [ ] Présentation soutenance
- [ ] Rapport PFE

---

## Notes importantes

> **Constat au 28/03/2026** : S0 et S1 sont clôturés sur le calendrier mais non formellement
> acceptés (statut "À faire" dans BACKLOG_COMPLET.xlsx). Le backend API existe et est
> fonctionnel (~70% EPIC 1-6 implémentés), mais aucune interface Angular n'existe.
> La priorité immédiate est de valider le backend existant comme base de S1 et de
> démarrer S2 demain avec les absences et la finalisation des tests.
