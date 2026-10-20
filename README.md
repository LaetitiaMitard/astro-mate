# AstroMate

AstroMate est un projet d’atelier IA autour d’une application de formation à la
surveillance d’une station spatiale fictive. Il doit permettre d’observer des
indicateurs simulés, de repérer des anomalies et de consulter leur historique.

> **Simulation uniquement** — toutes les télémétries et tous les seuils sont
> fictifs et non opérationnels. AstroMate ne se connecte ni à une station réelle,
> ni à des capteurs ou à des services externes.

## Périmètre V1

- Tableau de bord avec l’état de la station, quatre indicateurs principaux et
  les alertes ouvertes.
- Suivi du support de vie, du contrôle thermique, de l’alimentation électrique
  et des communications.
- Détail du système thermique, de ses composants et de ses mesures historiques.
- Consultation des alertes et recherche filtrée dans le journal d’événements.
- Simulation locale périodique et persistance de l’historique.

L’application est en phase de spécification : le dépôt ne contient pas encore
de code applicatif ni de procédure d’installation, de lancement ou de test.

## Architecture prévue

- **Interface** : React, TypeScript et Vite.
- **API locale** : Java 21 et Spring Boot 3.
- **Persistance** : SQLite locale.
- **Contrat API** : OpenAPI, selon une approche contract-first.

En usage normal, l’application Spring Boot servira les fichiers compilés du
frontend afin de démarrer AstroMate avec un seul processus. Les détails et
contraintes sont décrits dans l’[ADR d’architecture](docs/adr/0001-architecture-locale.md).

## Documentation

- [Spécifications fonctionnelles et techniques](docs/specifications.md)
- [Expression de besoin](docs/edb.md)
- [Modèle de données](docs/modele-donnees-astromate.md)
- [Contrat OpenAPI](docs/api/openapi.yaml)
- [Glossaire métier](GLOSSARY.md)
- [Maquette du tableau de bord](docs/Maquette%20tableau%20de%20bord.jpeg)
- [Maquette du détail système](docs/maquette%20d%C3%A9tail%20systeme.png)
- [Maquette du journal](docs/maquette%20journal.jpeg)

## Démarrage et vérification

Les commandes de démarrage et de validation seront documentées une fois le
code applicatif et ses outils ajoutés au dépôt.
