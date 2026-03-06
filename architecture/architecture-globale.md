\# Architecture globale du système - InternTrackAI



\## 1. Vue générale



InternTrackAI est une plateforme web basée sur une architecture moderne orientée microservices et DevOps.  

L'application est composée de plusieurs couches permettant de garantir la scalabilité, la sécurité et la facilité de déploiement.



L'architecture repose sur les composants suivants :



\- Frontend web

\- Backend applicatif

\- Service d'intelligence artificielle

\- Base de données

\- Infrastructure DevOps



---



\## 2. Architecture logique



Le système est composé de trois parties principales :



\### Frontend



Le frontend est développé avec \*\*Angular\*\*.



Responsabilités :

\- interface utilisateur

\- affichage des tableaux de bord

\- interaction avec l’API backend



---



\### Backend



Le backend est développé avec \*\*Spring Boot\*\*.



Responsabilités :

\- gestion des utilisateurs

\- gestion des missions

\- gestion des stagiaires

\- gestion des tableaux de bord

\- communication avec la base de données



---



\### Service IA



Un microservice d’intelligence artificielle sera développé avec \*\*Python et FastAPI\*\*.



Responsabilités :

\- analyse des données

\- prédiction des performances

\- recommandations pour les encadrants



---



\## 3. Base de données



Le système utilise \*\*PostgreSQL\*\* pour stocker :



\- les utilisateurs

\- les stagiaires

\- les missions

\- les rapports

\- les statistiques



---



\## 4. Infrastructure DevOps



Le projet adopte une architecture DevOps moderne.



\### Containerisation



Les services seront containerisés avec \*\*Docker\*\*.



\### CI/CD



L’intégration continue sera assurée par \*\*Jenkins\*\*.



\### Orchestration



Les conteneurs seront orchestrés avec \*\*Kubernetes (Minikube)\*\*.



\### GitOps



Le déploiement continu sera géré avec \*\*Argo CD\*\*.



---



\## 5. Observabilité



Le monitoring du système sera assuré avec :



\- \*\*Prometheus\*\*

\- \*\*Grafana\*\*



Ces outils permettront de surveiller les performances du système et les métriques applicatives.



---



\## 6. Sécurité



Le projet adopte une approche DevSecOps.



Les outils utilisés sont :



\- \*\*RBAC\*\* pour la gestion des accès

\- \*\*Semgrep\*\* pour l’analyse du code

\- \*\*OWASP Dependency Check\*\* pour les dépendances

\- \*\*Trivy\*\* pour l’analyse des images Docker



---



\## 7. Schéma d’architecture



Architecture simplifiée :



Utilisateur  

↓  

Frontend Angular  

↓  

Backend Spring Boot  

↓  

Base de données PostgreSQL  



Le backend communique également avec :



\- le microservice IA

\- les services DevOps

\- l’infrastructure Kubernetes

