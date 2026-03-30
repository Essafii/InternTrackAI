# Product Backlog — InternTrackAI
## DXC Technology Morocco × ISTIC, PFE 2026

**Total:** 90 US · 551 SP · 8 sprints (S0–S7) · 3 Releases
**Source of truth:** `BACKLOG_COMPLET.xlsx` (90 US with full acceptance criteria)
**This file:** Quick reference — implementation status per US

> Legend: ✅ Backend done · ⚠️ Partial · ❌ Not started · 🔜 Upcoming sprint

---

## EPIC 1 — Onboarding & Authentification (S1)

| US | Story | SP | Backend | Frontend |
|----|-------|----|---------|----------|
| US-01 | En tant que RH, je veux créer un compte utilisateur | 5 | ✅ `/auth/register` | ❌ |
| US-02 | En tant qu'utilisateur, je veux me connecter (JWT) | 5 | ✅ `/auth/login` + `/auth/refresh` | ❌ |
| US-03 | En tant qu'admin, je veux gérer les rôles RBAC | 5 | ✅ `SecurityConfig` + `@PreAuthorize` | ❌ |
| US-04 | En tant que RH, je veux onboarder un stagiaire | 8 | ✅ `/stagiaires/onboarding` | ❌ |
| US-05 | En tant qu'utilisateur, je veux gérer mon profil | 5 | ✅ `/users/{id}` | ❌ |
| US-06 | En tant que système, je veux envoyer un email de bienvenue | 4 | ✅ `EmailService` + templates HTML | ❌ |

---

## EPIC 2 — Gestion des Stagiaires & Absences (S2)

| US | Story | SP | Backend | Frontend |
|----|-------|----|---------|----------|
| US-07 | En tant que RH, je veux lister/filtrer les stagiaires | 3 | ✅ `GET /stagiaires` (paginé) | ❌ |
| US-08 | En tant qu'encadrant, je veux voir la fiche d'un stagiaire | 3 | ✅ `GET /stagiaires/{id}` | ❌ |
| US-09 | En tant que RH, je veux mettre à jour le profil stagiaire | 3 | ✅ `PUT /stagiaires/{id}` | ❌ |
| US-10 | En tant qu'encadrant, je veux enregistrer une absence | 5 | ✅ `POST /absences` | ❌ |
| US-11 | En tant que système, je veux calculer le taux d'assiduité | 5 | ✅ `AbsenceService.calculerTauxAssiduite()` | ❌ |
| US-12 | En tant que système, je veux alerter si assiduité < 80% | 4 | ✅ `AbsenceService` → `NotificationService` | ❌ |
| US-13 | En tant que stagiaire, je veux uploader un justificatif | 3 | ✅ `POST /absences/{id}/justificatif` | ❌ |

---

## EPIC 3 — Gestion des Tâches (S3)

| US | Story | SP | Backend | Frontend |
|----|-------|----|---------|----------|
| US-14 | En tant qu'encadrant, je veux créer une tâche | 5 | ✅ `POST /taches/stagiaire/{id}` | ❌ |
| US-15 | En tant que stagiaire, je veux voir mes tâches | 3 | ✅ `GET /taches/stagiaire/{id}` | ❌ |
| US-16 | En tant que stagiaire, je veux mettre à jour l'état d'une tâche | 5 | ✅ `PUT /taches/{id}` | ❌ |
| US-17 | En tant qu'encadrant, je veux replanifier une tâche | 3 | ✅ `PUT /taches/{id}` | ❌ |
| US-18 | En tant que système, je veux marquer les tâches en retard automatiquement | 5 | ✅ `TacheService.checkOverdueTaches()` @Scheduled cron 8h/jour | ❌ |
| US-19 | En tant que système, je veux notifier lors d'une assignation | 3 | ✅ `NotificationService` | ❌ |

---

## EPIC 4 — Livrables & Évaluations (S3)

| US | Story | SP | Backend | Frontend |
|----|-------|----|---------|----------|
| US-20 | En tant que stagiaire, je veux soumettre un livrable | 5 | ✅ `POST /livrables` | ❌ |
| US-21 | En tant qu'encadrant, je veux valider un livrable (RG06) | 5 | ✅ `PATCH /livrables/{id}/valider` | ❌ |
| US-22 | En tant qu'encadrant, je veux créer une évaluation | 5 | ✅ `POST /evaluations/stagiaire/{id}` | ❌ |
| US-23 | En tant qu'encadrant, je veux valider une évaluation | 3 | ✅ `PATCH /evaluations/{id}/valider` | ❌ |
| US-24 | En tant que stagiaire, je veux voir mes évaluations | 3 | ✅ `GET /evaluations/stagiaire/{id}` | ❌ |
| US-25 | En tant que RH, je veux voir toutes les évaluations | 3 | ✅ `GET /evaluations/stagiaire/{id}` | ❌ |

---

## EPIC 5 — Dashboards & Reporting (S4)

| US | Story | SP | Backend | Frontend |
|----|-------|----|---------|----------|
| US-26 | En tant que RH, je veux un dashboard global | 8 | ✅ `GET /dashboard/rh` | ❌ |
| US-27 | En tant qu'encadrant, je veux un dashboard équipe | 5 | ✅ `GET /dashboard/encadrant/{id}` | ❌ |
| US-28 | En tant que stagiaire, je veux voir ma progression | 5 | ✅ `GET /dashboard/stagiaire/{id}` | ❌ |
| US-29 | En tant que RH, je veux les tendances mensuelles | 5 | ✅ `GET /dashboard/rh/trends` | ❌ |
| US-30 | En tant que RH, je veux générer un rapport PDF | 8 | ✅ `ReportingService` + OpenPDF | ❌ |
| US-31 | En tant que RH, je veux exporter en Excel | 5 | ✅ `ExcelExportService` + Apache POI | ❌ |
| US-32 | En tant que RH, je veux voir l'historique des rapports | 3 | ✅ `ReportJob` entity + `GET /reporting` | ❌ |

---

## EPIC 6 — Scoring IA & Classement (S5)

| US | Story | SP | Backend | Frontend |
|----|-------|----|---------|----------|
| US-33 | En tant que système, je veux calculer un score pondéré (RG01) | 8 | ✅ `ClassementService.calculerScore()` | ❌ |
| US-34 | En tant que système, je veux recalculer le score hebdomadairement (RG02) | 5 | ✅ `ClassementService.recalculerClassementHebdomadaire()` @Scheduled lundi 9h | ❌ |
| US-35 | En tant que RH, je veux voir le classement global | 5 | ✅ `GET /classement` | ❌ |
| US-36 | En tant qu'encadrant, je veux voir le classement de mon équipe | 3 | ✅ `GET /classement?encadrantId={id}` | ❌ |
| US-37 | En tant que système, je veux détecter les stagiaires à risque | 5 | ✅ `risqueIA` dans `ClassementService` | ❌ |

---

## EPIC 7 — Notifications & SSE (S6)

| US | Story | SP | Backend | Frontend |
|----|-------|----|---------|----------|
| US-38 | En tant qu'utilisateur, je veux recevoir des notifications temps réel | 8 | ✅ `SseController` + `NotificationService` | ❌ |
| US-39 | En tant qu'utilisateur, je veux voir mes notifications | 3 | ✅ `GET /notifications` | ❌ |
| US-40 | En tant qu'utilisateur, je veux marquer une notification comme lue | 3 | ✅ `PATCH /notifications/{id}/read` | ❌ |

---

## EPIC 8 — Frontend Angular (S6 — CRITIQUE)

| US | Story | SP | Backend | Frontend |
|----|-------|----|---------|----------|
| US-41 | Page de login Angular | 8 | ✅ API prête | ❌ |
| US-42 | Dashboard RH Angular | 13 | ✅ API prête | ❌ |
| US-43 | Dashboard Encadrant Angular | 13 | ✅ API prête | ❌ |
| US-44 | Dashboard Stagiaire Angular | 8 | ✅ API prête | ❌ |
| US-45 | Gestion stagiaires Angular | 8 | ✅ API prête | ❌ |
| US-46 | Gestion tâches Angular | 8 | ✅ API prête | ❌ |
| US-47 | Gestion livrables Angular | 8 | ✅ API prête | ❌ |
| US-48 | Gestion évaluations Angular | 8 | ✅ API prête | ❌ |
| US-49 | Gestion absences Angular | 5 | ✅ API prête | ❌ |
| US-50 | Classement & scoring Angular | 5 | ✅ API prête | ❌ |
| US-51 | Notifications temps réel Angular (SSE) | 8 | ✅ API prête | ❌ |

---

## EPIC 9 — Audit & Sécurité (S7)

| US | Story | SP | Backend | Frontend |
|----|-------|----|---------|----------|
| US-52 | Journal d'audit immutable (RG04) | 5 | ✅ `AuditService` + `ActionLog` | ❌ |
| US-53 | Consultation logs d'audit (RH/Admin) | 3 | ✅ `GET /audit` | ❌ |
| US-54 | Rate limiting par IP | 3 | ✅ `RateLimitingFilter` | N/A |

---

## EPIC 10 — DevSecOps (S0 & S7)

| US | Story | SP | Statut |
|----|-------|----|--------|
| US-55 | GitHub repo + stratégie de branches | 3 | ✅ Fait |
| US-56 | Docker + Docker Compose complet | 5 | ✅ Fait (corrigé) |
| US-57 | Jenkinsfile CI/CD | 8 | ✅ Fait (corrigé) |
| US-58 | Kubernetes Minikube | 8 | ⚠️ Manifests présents, à valider |
| US-59 | Prometheus + Grafana | 5 | ❌ Non démarré |
| US-60 | Semgrep SAST | 3 | ⚠️ Dans Jenkinsfile, config à créer |
| US-61 | OWASP Dependency Check | 3 | ⚠️ Dans Jenkinsfile, plugin requis |
| US-62 | Trivy image scan | 3 | ⚠️ Dans Jenkinsfile, à installer |

---

## Backlog Summary — 28/03/2026

| Dimension | Valeur |
|-----------|--------|
| US backend implémentées | ~35/90 (~39%) |
| US frontend implémentées | 0/90 (0%) |
| US DoD validées formellement | 0/90 |
| Sprints clôturés formellement | 0/8 |
| Sprints en retard | S0, S1 |
| Prochain sprint | S2 (démarre 29/03) |

> **Note:** Le backend de Mehdi couvre largement S1–S5 au niveau API.
> La vraie priorité est (1) valider S0/S1, (2) démarrer S2 (absences),
> (3) lancer le frontend Angular dès que possible (S6 = 144 SP = plus gros sprint).
