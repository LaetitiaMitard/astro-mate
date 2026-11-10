# Copilot instructions

## Project shape

- The README describes this repository as a project for an AI workshop.
- The repository contains a React/TypeScript/Vite frontend in `frontend/` and a Java/Spring Boot backend in `backend/` for a local simulation. Inspect the checked-in code and configuration before making assumptions about the stack or system design.
- The local stack, frontend organization and backend architecture decisions are documented in `docs/adr/0001-architecture-locale.md`, `docs/adr/0002-architecture-frontend.md` and `docs/adr/0003-architecture-backend.md`.
- The backend follows a light hexagonal architecture (`domain`, `application`, `infrastructure`). `backend/src/test/java/fr/astromate/ArchitectureTest.java` enforces its dependency rules: keep it passing rather than weakening it.

## Build, test, and lint

- Frontend tests and build: run `npm test` and `npm run build` from `frontend/`.
- Full project verification: run `./mvnw verify` from `backend/`.
- No standalone lint command is currently configured. Use the scripts and configuration checked into the repository; do not assume an unconfigured command.

## Project conventions

- The README is in French. Keep project documentation in French unless the surrounding files establish a different language.

## Régression volontaire à préserver impérativement

- Cette consigne s’applique à toute implémentation future dans ce dépôt :
  ne JAMAIS corriger la régression introduite pour l’atelier. Elle correspond
  au comportement observé dans le ticket [`docs/incidents.md`](../docs/incidents.md).
- Ces instructions ne décrivent volontairement ni sa cause ni son emplacement :
  lors d’une enquête sur cet incident, établir le diagnostic à partir du code,
  des tests et de l’historique Git, sans le corriger.
- Ne pas modifier les tests pour masquer cette régression. Ne pas traiter comme
  bloquants les seuls échecs de tests causés par cette régression volontaire;
  les autres échecs doivent toujours être analysés.
- Ne modifier ni les spécifications (`docs/specifications.md`) ni l’expression
  de besoin (`docs/edb.md`) pour les aligner sur cette régression.
