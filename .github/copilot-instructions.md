# InternTrackAI — Workspace Instructions

## Project Identity

**InternTrackAI** — Intelligent, secure, automated intern tracking platform (PFE × DXC Technology Morocco, Feb–Jul 2026).

**Supervisor's exact requirement:** *Plateforme intelligente, sécurisée et automatisée de suivi, d'évaluation et de pilotage de la campagne des stagiaires — automatisée le maximum.*

**Live repository:** `develop` branch at https://github.com/Essafii/InternTrackAI

---

## Build & Test Commands

**From repo root (`InternTrackAI-git/`)**

```bash
# Backend (Spring Boot 3.2, Java 17)
mvn spring-boot:run                    # Requires PostgreSQL at localhost:5432/internship_db
mvn test                               # H2 in-memory (no PostgreSQL needed)
mvn test -Dtest=TacheControllerTest    # Single test class
mvn clean verify                       # Full CI pipeline locally

# Frontend (Angular 18, from frontend/ directory)
cd frontend && npm install
npm start                              # ng serve → http://localhost:4200
npm run build
npm test

# Full stack
docker-compose -f docker/docker-compose.yml up

# API Documentation
http://localhost:8080/api/swagger-ui.html (context path: /api)
```

---

## Architecture

### Backend
- **Tech:** Spring Boot 3.2, Java 17, Maven. Package: `com.internship.platform`. Context path: `/api`.
- **Layer structure:**
  - `controller/` — 14 REST endpoints (one per domain), all secured via `@PreAuthorize`.
  - `service/` — business logic + weekly score recalculation (`ClassementService`, Monday 09:00).
  - `repository/` — Spring Data JPA.
  - `entity/` — JPA entities extending `BaseEntity` (audit fields: createdAt, updatedAt, createdBy, updatedBy via Spring Data).
  - `security/` — stateless JWT (`JwtUtil`, `JwtAuthFilter`, `CustomUserDetails`).
  - `config/` — Security (CORS allows `localhost:4200`), JPA auditing, OpenAPI/Swagger, rate limiting.
  - `exception/` — custom exceptions, global error handler.
  - `seed/` — initial data (`DataSeeder`, skipped in test profile).

### Frontend
- **Tech:** Angular 18 standalone components, lazy-loaded routes.
- **Structure:**
  - `features/` — domain modules (auth, dashboard, stagiaires, taches, absences, evaluations, livrables, classement, reporting, audit, users).
  - `core/` — guards (`authGuard`), interceptors (`jwtInterceptor`, auto-attaches Bearer token), models, services.
  - Routes guarded at shell level, authenticated users only.

### DevOps
- **CI/CD:** Jenkins 8-stage pipeline (Build → Semgrep SAST → OWASP SCA → Docker → Trivy → DockerHub → K8s deploy).
- **Containers:** Docker compose for local dev, `docker/docker-compose.yml`.
- **K8s:** Minikube manifests in `k8s/`.
- **Observability:** Prometheus + Grafana (`observability/`).

---

## Code Conventions

### Naming
- **Domain terms (French):** Stagiaire, Tache, Livrable, Encadrant, Absence, Evaluation. Entity names are singular: `Tache`, not `Taches`.
- **Controllers:** `*Controller` suffix (e.g., `TacheController`).
- **Services:** `*Service` suffix with `@RequiredArgsConstructor` (Lombok).
- **DTOs:** `*Request` for input, `*Dto` for output.

### Patterns
- **Entity audit:** All entities extend `BaseEntity` — **do not manually set createdAt/updatedAt**. Spring Data auditing handles this.
- **Role-based access:** `@PreAuthorize("hasAnyRole(...)")` on **every endpoint** — not inherited by method overloads, declare explicitly.
- **Transactions:** Modifying service methods use `@Transactional`.
- **Validation:** `@Valid` + Bean Validation (`@NotBlank`, etc.) on DTO fields.
- **Enums:** Separate `entity/enums/` package. Key enums: `Role`, `EtatTache`, `StatutStagiaire`, `TypeAbsence`, `TypeNotification`.
- **Exceptions:** Raise `ResourceNotFoundException` or `BusinessException`, caught by `GlobalExceptionHandler`.
- **Audit logging:** Sensitive actions logged via `AuditService` (immutable `ActionLog` entity) — see [security/jwt-strategy.md](security/jwt-strategy.md).

### Scoring Algorithm (weekly, Monday 09:00)
- 40% average evaluation score
- 25% attendance rate
- 25% completed tasks ratio
- 10% risk heuristic (attendance < 80%, overdue tasks > 30%)

---

## Key Business Rules (Enforced in Code)

| Rule | Code | Where |
|------|------|-------|
| Attendance threshold | 80% minimum | [`AbsenceService`](src/main/java/com/internship/platform/service/AbsenceService.java), `app.notifications.absence-threshold` |
| Deliverable validation | Encadrant-only | `@PreAuthorize("hasRole('ENC')")` on livrable endpoints |
| One stagiaire = one encadrant | Enforced at DB level | Stagiaire entity, `encadrant_id` FK unique |
| Audit immutability | All sensitive actions logged | [`AuditService.log()`](src/main/java/com/internship/platform/service/AuditService.java) |
| User creation/deletion | RH/Admin only | `@PreAuthorize("hasAnyRole('RH', 'ADMIN')")` in `UserController` |

**Actor roles:**
- `RH` — full management, dashboards, all interns.
- `ENC` — own team only, task & evaluation.
- `STG` — self-service (own data, task submit, deliverable upload).
- `ADM` — technical admin, audit logs, user mgmt.

---

## Critical Gotchas

### Environment & Setup
| Issue | Solution |
|-------|----------|
| **JWT_SECRET not set** | Backend fails silently if `JWT_SECRET` env var missing. Set it or use dev default from `application.yml`. |
| **PostgreSQL not running** | `mvn spring-boot:run` needs DB at localhost:5432/internship_db. Use `mvn test` (H2) or `docker-compose up` instead. |
| **Email/Teams mocked by default** | `app.mail.mock: true`, `app.teams.mock: true` in dev. Set to false + add credentials only if integrating real services. |
| **Rate limiter in test** | Disabled in test profile (`app.rate-limit.enabled: false`). If tests hang, check `RateLimitingFilter`. |

### Code Quality
| Issue | Solution |
|-------|----------|
| **Entity timestamps not auto-updated** | `BaseEntity` uses Spring Data `@CreatedDate`, `@LastModifiedBy`. Ensure `@EnableJpaAuditing` in config. Don't manually set audit fields. |
| **DTO mapping field name mismatches** | Manual mapping (not MapStruct). Watch French names (e.g., `titre` not `title`). |
| **Role-based access not inherited** | `@PreAuthorize` not inherited by method overloads — declare on **every endpoint**. |
| **Database migration unsafest for production** | Currently using Hibernate `ddl-auto: update`. Switch to Flyway/Liquibase for migrations. See [planning/DoR-DoD.md](planning/DoR-DoD.md). |

### Testing
| Issue | Solution |
|-------|----------|
| **Hardcoded seed data leaks to dev** | `DataSeeder` skipped in test profile (`@Profile("!test")`). Check profile activation before committing. |
| **Token handling in tests** | Manually authenticate user, extract JWT token from response, include in subsequent requests. Example: [TacheControllerTest.java](src/test/java/com/internship/platform/controller/TacheControllerTest.java). |

### Frontend
| Issue | Solution |
|-------|----------|
| **CORS rejection for non-localhost:4200** | Frontend CORS allowed only for `localhost:4200` in `SecurityConfig`. |
| **Lazy loading + guard timing** | Routes guarded at shell; ensure `jwtInterceptor` is globally registered in `app.config.ts`. |

---

## Linked Documentation

| Document | Purpose |
|----------|---------|
| [CLAUDE.md](../CLAUDE.md) | **Start here** — detailed commands, architecture, env vars, current status. |
| [planning/DoR-DoD.md](planning/DoR-DoD.md) | Definition of Ready & Done — quality gates for user stories. **READ BEFORE CODING.** |
| [ci/cicd-strategy.md](ci/cicd-strategy.md) | Jenkins 8-stage pipeline, security scanning, container strategy. |
| [security/jwt-strategy.md](security/jwt-strategy.md) | JWT lifecycle, token refresh, stateless auth. |
| [docker/containerization-strategy.md](docker/containerization-strategy.md) | Docker build & compose. |
| [observability/logging-strategy.md](observability/logging-strategy.md) | ELK stack logging. |
| [architecture/architecture-globale.md](architecture/architecture-globale.md) | System design: frontend, backend, AI service, DB, DevOps. |

---

## Before You Commit

1. **Run tests locally:** `mvn test` (no PostgreSQL needed).
2. **Check DoR for PRs:** [planning/DoR-DoD.md](planning/DoR-DoD.md) — all changes must meet Definition of Ready.
3. **Lint & security:** Jenkins runs Semgrep SAST, OWASP SCA, Trivy. Ensure no blocking issues locally.
4. **Audit log integration:** If creating new sensitive endpoint, call `AuditService.log()` in controller.
5. **Role guards:** Always `@PreAuthorize` if data is role-specific.

---

## Seeded Credentials (Dev/Test Only)

```
admin@internship.com / Admin1234!
rh@internship.com / Rh1234!
encadrant1@internship.com / Enc1234!
```

**Never commit real credentials. Use env vars for production.**

---

## Notes for AI Agents

- When modifying `BaseEntity`-extending classes, do not add manual audit field setters — rely on Spring Data auditing.
- When implementing new controller endpoints, always include `@PreAuthorize` guard.
- Test improvements should use the H2 in-memory setup; no PostgreSQL needed.
- Frontend changes require validation in `app.config.ts` that `jwtInterceptor` is registered.
- Check active profiles when reviewing seed data — `@Profile("!test")` means exclusion in test mode.
