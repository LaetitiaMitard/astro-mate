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

Le dépôt fournit le socle local de l’application. Les vues et règles métier
décrites dans les spécifications restent à implémenter.

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

## Prérequis

- Java 21
- Node.js 20 ou ultérieur et npm

Les dépendances sont téléchargées lors du premier build. À l’exécution,
l’application ne requiert aucun service distant : Spring Boot utilise une base
SQLite locale et écoute uniquement sur `127.0.0.1`.

## Développement

Lancer le frontend et le backend dans deux terminaux, depuis la racine du
dépôt.

Terminal 1 — frontend Vite, avec proxy `/api` vers le backend :

```sh
cd frontend
npm ci
npm run dev
```

Ouvrir <http://127.0.0.1:5173>.

Terminal 2 — backend Spring Boot avec SQLite locale :

```sh
cd backend
./mvnw -Dexec.skip=true compile spring-boot:run
```

La base est créée dans `backend/astromate.db`. Le backend est disponible sur
<http://127.0.0.1:8080>.

En développement, `-Dexec.skip=true` évite de réinstaller et reconstruire le
frontend pendant que Vite fonctionne. Vite choisit un autre port si `5173` est
occupé : ouvrir l’adresse indiquée dans son terminal.

## Build, tests et exécution empaquetée

Depuis le répertoire `backend`, cette commande installe les dépendances
frontend, construit les assets, exécute les tests frontend et backend, puis
produit le JAR exécutable :

```sh
./mvnw verify
```

Le test d’intégration démarre Spring Boot avec une base SQLite en mémoire et
vérifie que le serveur écoute sur loopback et sert l’index compilé. Les tests
frontend vérifient l’écran de démarrage minimal.

Pour lancer l’application empaquetée avec le frontend servi par Spring Boot :

```sh
java -jar target/astromate-0.1.0-SNAPSHOT.jar
```

Lancer cette commande depuis `backend`; l’interface sera disponible sur
<http://127.0.0.1:8080>.
