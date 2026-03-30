# Definition of Ready (DoR) & Definition of Done (DoD)
## InternTrackAI — DXC Technology Morocco × ISTIC, PFE 2026

---

## Definition of Ready (DoR)

A User Story is **ready to be picked up in a sprint** when ALL of the following are true:

### Story Quality
- [ ] Follows the format: *"En tant que [rôle], je veux [action] afin de [bénéfice]"*
- [ ] Story points estimated by the team (using Planning Poker or consensus)
- [ ] Acceptance criteria written in **Given / When / Then** format
- [ ] MoSCoW priority assigned (Must / Should / Could / Won't)

### Clarity & Scope
- [ ] The team understands what needs to be built (no open questions)
- [ ] Dependencies on other US are identified and either done or planned in the same sprint
- [ ] UI/UX: wireframe or mockup available (Chadi's maquettes consulted for frontend US)
- [ ] API contract defined (endpoint, method, request/response schema) for backend US

### Technical
- [ ] No blocking technical unknowns (spikes completed if needed)
- [ ] Database migration impact assessed (new table/column vs. existing schema)
- [ ] Security impact assessed (RBAC rules confirmed against roles matrix)

---

## Definition of Done (DoD)

A User Story is **done** when ALL of the following are true:

### Code
- [ ] Feature branch merged into `develop` via Pull Request (no direct commits to `develop`)
- [ ] PR reviewed and approved by at least **one** other team member
- [ ] No TODO/FIXME left in code related to this story
- [ ] No compiler warnings introduced

### Tests
- [ ] Unit tests written for all new service methods (happy path + at least one error case)
- [ ] `mvn test` passes locally before pushing
- [ ] Test coverage does not decrease compared to previous sprint baseline
- [ ] API manually tested via Swagger UI (`http://localhost:8080/api/swagger-ui.html`)

### Security
- [ ] RBAC rules enforced with `@PreAuthorize` (no endpoint is publicly accessible unless explicitly required)
- [ ] No hardcoded secrets, passwords, or API keys in code
- [ ] Input validation present (`@Valid` + Bean Validation annotations)
- [ ] Sensitive actions logged via `AuditService` (RG04)

### Integration
- [ ] Feature works end-to-end with `DataSeeder` test data (all 7 seed users)
- [ ] No regression in existing API endpoints (run full test suite)
- [ ] Docker image builds successfully: `docker build -f docker/Dockerfile.backend .`

### Documentation
- [ ] Swagger `@Operation` annotation updated for new/modified endpoints
- [ ] Backlog row status updated to **Terminé** in `BACKLOG_COMPLET.xlsx`
- [ ] Sprint board card moved to **Done**
- [ ] If a new business rule was implemented, it is noted in `architecture/architecture-globale.md`

---

## Sprint-Level DoD (applied to the sprint as a whole)

In addition to per-story DoD, a sprint is closed when:

- [ ] Sprint Review held — demo to supervisor Naoufal LEBHAR (or internal demo if supervisor unavailable)
- [ ] Sprint Retrospective held (10 min — what worked, what didn't, one improvement)
- [ ] `BACKLOG_COMPLET.xlsx` sprint tab fully updated (status, actual SP, owner)
- [ ] Jenkins CI pipeline green on `develop` branch
- [ ] No CRITICAL vulnerabilities in Trivy image scan
- [ ] Release note added if it's a Release sprint (R1, R2, R3)

---

## Business Rules Checklist (apply to all relevant stories)

| Rule | Requirement | Checked in DoD by |
|------|-------------|-------------------|
| RG01 | Score = 40% eval + 25% attendance + 25% tasks + 10% risk | ClassementService |
| RG02 | Score recalculates automatically each week | Scheduled job |
| RG03 | Weighted average: mid-term 40% + final 60% | EvaluationService |
| RG04 | All sensitive actions in immutable audit log | AuditService |
| RG05 | Deliverable max 20 MB | Validation + multipart config |
| RG06 | Deliverable validated only by assigned encadrant | LivrableService |
| RG07 | Attendance < 80% triggers automatic alert | AbsenceService |
| RG08 | Overdue task → auto status EN_RETARD | Scheduled job |
| RG09 | Notification sent when task assigned | NotificationService |
| RG10 | Report generated within 5 min of request | ReportingService |
| RG11 | All exports in PDF or Excel | ExcelExportService, ReportingService |
| RG12 | Only RH/Admin can create/delete users | AuthController @PreAuthorize |
