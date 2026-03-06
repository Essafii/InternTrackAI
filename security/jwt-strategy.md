# Stratégie de sécurité JWT - InternTrackAI

## Objectif
Mettre en place une authentification sécurisée basée sur JWT avec access token et refresh token.

## Principe
- l'utilisateur s'authentifie avec ses identifiants
- le système génère un access token
- le système génère un refresh token
- l'access token est utilisé pour accéder aux API
- le refresh token permet de générer un nouvel access token

## Sécurité
- durée courte pour l'access token
- durée plus longue pour le refresh token
- stockage sécurisé des tokens
- vérification de la signature du token

## Intégration future
Cette stratégie sera implémentée dans le backend Spring Boot.
