# Stratégie GitFlow - InternTrackAI

## Branches principales
- main : branche stable de production
- develop : branche principale de développement

## Branches secondaires
- feature/* : développement des nouvelles fonctionnalités
- release/* : préparation d'une version
- hotfix/* : correction urgente sur la version stable

## Workflow
1. créer une branche feature depuis develop
2. développer et commiter les changements
3. fusionner la branche feature dans develop
4. préparer une branche release si nécessaire
5. fusionner dans main pour la version stable
