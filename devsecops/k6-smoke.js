// ─── k6 Smoke Load Test — InternTrackAI ───────────────────────────────────────
// Usage local :
//   k6 run devsecops/k6-smoke.js -e BASE_URL=http://localhost:8080
// Usage Jenkins (dans stage 'Load Test Smoke') :
//   docker run --rm -v $(pwd):/src grafana/k6 run /src/devsecops/k6-smoke.js \
//     -e BASE_URL=http://<backend-cluster-ip>:8080
//
// Objectif : vérifier que l'API supporte 10 VUs pendant 30s sans erreur
// Ce n'est pas un test de charge (stress) — c'est une validation de base.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('error_rate');
const healthDuration = new Trend('health_check_duration', true);

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    // Moins de 5% d'erreurs
    error_rate: ['rate<0.05'],
    // P95 de /actuator/health < 2s
    health_check_duration: ['p(95)<2000'],
    // Taux de succès HTTP > 95%
    http_req_failed: ['rate<0.05'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // Test 1 : Health check
  const healthRes = http.get(`${BASE_URL}/api/actuator/health`, {
    tags: { name: 'health' },
  });
  healthDuration.add(healthRes.timings.duration);
  const healthOk = check(healthRes, {
    'health status is 200': (r) => r.status === 200,
    'health body contains UP': (r) => r.body && r.body.includes('"UP"'),
  });
  errorRate.add(!healthOk);

  sleep(0.5);

  // Test 2 : API docs disponible
  const docsRes = http.get(`${BASE_URL}/api/v3/api-docs`, {
    tags: { name: 'api-docs' },
  });
  check(docsRes, {
    'api-docs status is 200': (r) => r.status === 200,
    'api-docs contains openapi': (r) => r.body && r.body.includes('openapi'),
  });

  sleep(0.5);

  // Test 3 : Prometheus metrics endpoint
  const metricsRes = http.get(`${BASE_URL}/api/actuator/prometheus`, {
    tags: { name: 'prometheus' },
  });
  check(metricsRes, {
    'metrics status is 200': (r) => r.status === 200,
    'metrics non vide': (r) => r.body && r.body.length > 0,
  });

  sleep(1);
}

export function handleSummary(data) {
  // Résumé JSON archivé par Jenkins comme artefact
  return {
    'k6-smoke-report.json': JSON.stringify(data, null, 2),
  };
}
