\# Stratégie DevOps - InternTrackAI



\## 1. Objectif



L’objectif de la stratégie DevOps est d’automatiser le développement, les tests et le déploiement de l’application afin d’améliorer la qualité du logiciel et réduire le temps de mise en production.



---



\## 2. Gestion du code source



Le code source du projet est géré avec \*\*Git\*\*.



Stratégie de branches :



\- main : version stable du projet

\- develop : branche de développement

\- feature : développement des nouvelles fonctionnalités



---



\## 3. Containerisation



Les différents services du projet seront containerisés avec \*\*Docker\*\* :



\- frontend Angular

\- backend Spring Boot

\- service IA Python



Chaque service disposera de son propre \*\*Dockerfile\*\*.



---



\## 4. Intégration continue



L’intégration continue sera réalisée avec \*\*Jenkins\*\*.



Le pipeline Jenkins exécutera automatiquement :



\- compilation du code

\- exécution des tests

\- construction des images Docker



---



\## 5. Déploiement continu



Le déploiement continu sera réalisé avec \*\*Kubernetes (Minikube)\*\*.



Les conteneurs Docker seront déployés sous forme de :



\- Pods

\- Services

\- Deployments



---



\## 6. GitOps



Le déploiement sera automatisé avec \*\*Argo CD\*\*.



Argo CD surveillera le repository Git et appliquera automatiquement les changements dans le cluster Kubernetes.



---



\## 7. Monitoring



La supervision de l’application sera assurée par :



\- Prometheus (collecte des métriques)

\- Grafana (visualisation des métriques)



Cela permettra de surveiller :



\- la performance du système

\- l’utilisation des ressources

\- l’état des services

