# Runbook — InternTrackAI DevOps/DevSecOps

**Projet :** InternTrackAI — DXC Technology Morocco × ISTIC PFE 2026
**Responsable DevOps :** Zakaria EL Omari
**Dernière mise à jour :** 2026-04-21

---

## 1. Architecture de déploiement

```
GitHub (develop/main)
    │
    ▼ webhook
Jenkins (ci/Jenkinsfile)
    │
    ├─ Build (Maven) ──────────────────────────────────────────────────────────┐
    ├─ Test (JUnit + JaCoCo)                                                   │
    ├─ SonarQube Scan + Quality Gate                                           │ Artefacts
    ├─ OWASP Dependency Check ──── target/dependency-check-report.*            │
    ├─ Semgrep Scan ─────────────── semgrep-report.json                        │
    ├─ Docker Build (tag versionné + :latest)                                  │
    ├─ Trivy Image Scan ─────────── trivy-image-report.txt                     │
    ├─ SBOM CycloneDX ───────────── sbom-cyclonedx.json                        │
    ├─ Docker Push (DockerHub)                                                 │
    ├─ Deploy Staging (Minikube)                                               │
    │    └─ namespace: staging                                                 │
    │       ├─ LimitRange                                                      │
    │       ├─ NetworkPolicy (deny-all + allow explicit)                       │
    │       ├─ ConfigMap / ServiceAccount / RBAC                               │
    │       ├─ PostgreSQL + PVC 2Gi                                            │
    │       ├─ Backend (HPA 1→3, rolling update)                               │
    │       ├─ Service (NodePort) + Ingress NGINX                              │
    │       ├─ Prometheus (scrape /actuator/prometheus)                        │
    │       └─ Fluent Bit (logs containers)                                    │
    ├─ Smoke Tests (curl health + openapi)                                     │
    ├─ Load Test k6 (10 VU / 30s)                                              │
    └─ DAST ZAP Baseline ─────────── zap-reports/                            ◄─┘
```

---

## 2. Commandes quotidiennes

### Vérifier l'état du cluster staging

```powershell
kubectl get all -n staging
kubectl get pods -n staging
kubectl get hpa -n staging
kubectl top pods -n staging
```

### Vérifier les logs backend

```powershell
kubectl logs -l app=interntrackai-backend -n staging --tail=100 -f
```

### Vérifier les métriques Prometheus

```powershell
# Port-forward Prometheus local
kubectl port-forward svc/prometheus-service -n staging 9090:9090

# Ouvrir http://localhost:9090
# Query utile : up{job="interntrackai-backend"}
```

### Accéder à Grafana (monitoring local Docker)

```powershell
docker compose -f docker/docker-compose.monitoring.yml up -d
# http://localhost:3000  (admin/admin par défaut)
```

---

## 3. Procédures d'urgence

### 3.1 Backend en crash loop (CrashLoopBackOff)

```powershell
# 1. Voir les logs du pod qui crashe
kubectl logs <pod-name> -n staging --previous

# 2. Décrire le pod pour voir les events
kubectl describe pod <pod-name> -n staging

# 3. Rollback vers la version précédente
kubectl rollout undo deployment/interntrackai-backend -n staging

# 4. Vérifier l'historique des déploiements
kubectl rollout history deployment/interntrackai-backend -n staging
```

### 3.2 Base de données inaccessible

```powershell
# Vérifier que PostgreSQL tourne
kubectl get pods -l app=postgres -n staging

# Tester la connectivité depuis un pod temporaire
kubectl run pg-test --image=postgres:15-alpine --rm -it -n staging \
  -- psql -h postgres -U postgres -d interntrackai -c "\l"

# Restaurer depuis le dernier backup
kubectl get jobs -n staging | grep postgres-backup
kubectl exec -it <backup-pod> -n staging -- ls /backups/
```

### 3.3 Quality Gate SonarQube qui échoue

```
Cause principale : couverture de code insuffisante sur le "New Code"

Actions DevOps (dans ce repo) :
  1. Vérifier sonar-project.properties — les exclusions couvrent-elles
     le code non testable (config, entity, dto, exception, seed) ?
  2. Ajouter les packages manquants dans sonar.coverage.exclusions

Actions Backend (à signaler à l'équipe) :
  1. Ajouter des tests unitaires sur les services et contrôleurs modifiés
  2. Objectif : coverage > 80% sur le New Code (condition du Quality Gate)
  3. Classes prioritaires : AbsenceService, EvaluationService, AuthController

Note : le Quality Gate "Sonar Way" exige 80% de couverture sur New Code.
Pour ajuster ce seuil : SonarQube UI → Quality Gates → Sonar Way → Edition.
```

### 3.4 Pipeline Jenkins bloquée

```
Causes fréquentes :
  1. NVD_API_KEY expirée → renouveler le credential Jenkins 'nvd-api-key'
     https://nvd.nist.gov/developers/request-an-api-key
  2. Docker socket non accessible → vérifier que Jenkins tourne avec accès /var/run/docker.sock
  3. kubeconfig-staging expiré → regénérer : minikube update-context
  4. Credentials DockerHub expirés → renouveler 'dockerhub-creds' dans Jenkins
  5. SonarQube server down → vérifier http://localhost:9000
```

### 3.5 Rollback complet vers une version précédente

```powershell
# Option A — kubectl rollout undo
kubectl rollout undo deployment/interntrackai-backend -n staging

# Option B — forcer une image spécifique (tag versionné)
kubectl set image deployment/interntrackai-backend \
  backend=zakariael3/interntrackai-backend:<git-sha>-<build-number> \
  -n staging

# Vérifier le rollout
kubectl rollout status deployment/interntrackai-backend -n staging
```

---

## 4. Démarrage complet de l'environnement local

```powershell
# 1. Stack Docker Compose complète (développement)
docker compose -f docker/docker-compose.yml up --build -d

# 2. Monitoring Prometheus + Grafana
docker compose -f docker/docker-compose.monitoring.yml up -d

# 3. Keycloak local (optionnel)
docker compose -f docker/docker-compose.keycloak.yml up -d

# Vérifications
docker ps
# Backend    : http://localhost:8080/api/actuator/health
# Swagger    : http://localhost:8080/api/swagger-ui.html
# Prometheus : http://localhost:9090
# Grafana    : http://localhost:3000
# Keycloak   : http://localhost:8180
```

---

## 5. Déploiement Helm (staging / prod)

```powershell
# Staging (valeurs par défaut)
helm upgrade --install interntrackai ./helm/interntrackai \
  --set image.tag=<git-sha>-<build-number> \
  -n staging --create-namespace

# Production
helm upgrade --install interntrackai ./helm/interntrackai \
  -f helm/interntrackai/values-prod.yaml \
  --set image.tag=<git-sha>-<build-number> \
  -n production --create-namespace

# Vérifier le déploiement
helm status interntrackai -n staging
kubectl get pods -n staging
```

---

## 6. Credentials Jenkins requis

| ID credential | Type | Usage |
|---|---|---|
| `sonar-token` | Secret text | Token SonarQube |
| `nvd-api-key` | Secret text | API NVD pour OWASP |
| `dockerhub-creds` | Username/Password | Push DockerHub |
| `kubeconfig-staging` | Secret file | Deploy K8s Minikube |
| `teams-webhook-url` | Secret text | Notification Teams sur échec |

---

## 7. Checklist avant release

- [ ] Pipeline Jenkins verte (ou UNSTABLE acceptable sur OWASP/Trivy/ZAP)
- [ ] Quality Gate SonarQube PASSED
- [ ] Image Docker taguée et pushée sur DockerHub
- [ ] SBOM archivé (`sbom-cyclonedx.json`)
- [ ] Smoke Tests passés (health + openapi)
- [ ] k6 smoke : error_rate < 5%
- [ ] `git tag Release-SX` posé et poussé
- [ ] PR mergée dans `develop` (puis `main` pour release finale)

---

## 8. Contacts

| Rôle | Nom |
|---|---|
| DevOps / DevSecOps | Zakaria EL Omari |
| Superviseur | Naoufal LEBHAR |
| Repo | https://github.com/Essafii/InternTrackAI |
