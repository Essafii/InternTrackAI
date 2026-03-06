\# Stratégie DevSecOps - InternTrackAI



\## 1. Introduction



La stratégie DevSecOps vise à intégrer la sécurité à chaque étape du cycle de développement logiciel.



Dans le projet InternTrackAI, la sécurité sera intégrée dès la phase de développement jusqu’au déploiement.



---



\## 2. Sécurité du code



L’analyse statique du code sera réalisée avec \*\*Semgrep\*\*.



Objectifs :

\- détecter les vulnérabilités dans le code

\- identifier les mauvaises pratiques de programmation

\- améliorer la qualité du code



---



\## 3. Analyse des dépendances



Les dépendances du projet seront analysées avec \*\*OWASP Dependency-Check\*\*.



Objectifs :

\- détecter les bibliothèques vulnérables

\- prévenir l’utilisation de dépendances compromises



---



\## 4. Sécurité des conteneurs



Les images Docker seront analysées avec \*\*Trivy\*\*.



Objectifs :

\- détecter les vulnérabilités dans les images

\- renforcer la sécurité des conteneurs



---



\## 5. Gestion des accès



La gestion des accès sera réalisée avec \*\*RBAC (Role Based Access Control)\*\*.



Les rôles principaux sont :



\- Administrateur

\- Encadrant

\- Stagiaire



Chaque rôle possède des permissions spécifiques dans le système.



---



\## 6. Sécurité du pipeline



Le pipeline CI/CD intégrera les étapes suivantes :



\- analyse statique du code

\- scan des dépendances

\- scan des images Docker



Le pipeline échouera automatiquement en cas de vulnérabilité critique.



---



\## 7. Objectif final



L’objectif est de garantir :



\- la sécurité du code

\- la sécurité des dépendances

\- la sécurité des conteneurs

\- la sécurité de l’infrastructure

