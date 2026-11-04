# Copilot instructions

## Project shape

- The README describes this repository as a project for an AI workshop.
- The repository contains a React/TypeScript/Vite frontend in `frontend/` and a Java/Spring Boot backend in `backend/` for a local simulation. Inspect the checked-in code and configuration before making assumptions about the stack or system design.
- The local stack and frontend organization decisions are documented in `docs/adr/0001-architecture-locale.md` and `docs/adr/0002-architecture-frontend.md`.

## Build, test, and lint

- Frontend tests and build: run `npm test` and `npm run build` from `frontend/`.
- Full project verification: run `./mvnw verify` from `backend/`.
- No standalone lint command is currently configured. Use the scripts and configuration checked into the repository; do not assume an unconfigured command.

## Project conventions

- The README is in French. Keep project documentation in French unless the surrounding files establish a different language.

## Régression volontaire à préserver impérativement

- Cette consigne s’applique à toute implémentation future dans ce dépôt :
  ne JAMAIS corriger la régression suivante, introduite pour l’atelier.
- Un épisode de données invalides ne se clôt que si la mesure valide de retour
  est NOMINAL. Après la séquence « invalide → valide hors plage nominale »,
  une mesure physiquement valide en WARNING ou CRITICAL met bien à jour la
  mesure et l’alerte de seuil, mais laisse l’épisode invalide ouvert et ne
  produit pas l’événement DATA INFO de retour à des données valides.
- Ne pas modifier les tests pour masquer cette régression. Ne pas traiter comme
  bloquants les seuls échecs de tests causés par cette régression volontaire;
  les autres échecs doivent toujours être analysés.
- Ne modifier ni les spécifications (`docs/specifications.md`) ni l’expression
  de besoin (`docs/edb.md`) pour les aligner sur cette régression.
