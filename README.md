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
- Journal chronologique des événements techniques des sept derniers jours,
  consultable par pages de 20 événements.
- Simulation locale périodique et persistance de l’historique.

Le socle local comprend le tableau de bord, la liste des systèmes et le calcul
des états des métriques selon les seuils configurés, ainsi que le détail
thermique et le journal chronologique. Les autres vues et capacités décrites
dans les spécifications restent à implémenter.

## Scénario thermique

La simulation démarre automatiquement avec le backend et produit une télémétrie
toutes les 10 secondes. Les deux radiateurs suivent quatre phases de 60 secondes :
22,1/22,4 °C, 27 °C chacun, 31 °C chacun, puis 22,1/22,4 °C, avant de répéter
le cycle. Les autres métriques restent nominales et sont rafraîchies à la même
cadence. La température affichée est leur moyenne, mais l’état thermique suit
toujours le radiateur le plus sévèrement dégradé.

Une mesure valide devient périmée après plus de 30 secondes sans nouveau relevé :
son état passe à inconnu et un événement de catégorie `DATA` est ajouté au journal,
sans ouvrir d’alerte de seuil. La dernière valeur valide et son horodatage restent
consultables. Cette surveillance continue même lorsque la simulation automatique
est désactivée.

La disponibilité simulée est indépendante de la fraîcheur des mesures : un
système explicitement `OFFLINE` suspend sa télémétrie et ne produit aucun événement
de péremption par métrique. Son passage hors ligne est journalisé en catégorie
`SYSTEM`, niveau `WARNING`, et son retour en ligne en `INFO`, sans créer d’alerte
de seuil. Les dernières valeurs restent horodatées et leur état devient `UNKNOWN`
après plus de 30 secondes, tandis que le système reste hors ligne. Une mesure absente ou
périmée ne suffit jamais à déclarer un système hors ligne.

Une panne de l’API ou de SQLite est signalée par « Simulation indisponible » :
les vues concernées masquent leurs données conservées, sans valeurs de secours.
Un contrôle périodique toutes les 10 secondes complète les notifications de
télémétrie, et une lecture sans réponse échoue après 5 secondes. Les déclenchements
rapprochés, y compris les reconnexions du flux de notifications, sont regroupés
pour laisser expirer les lectures bloquées. L’affichage reprend uniquement après
une réponse valide de l’API.

Le cycle sauvegarde son avancement avec chaque production de télémétrie.
Après un redémarrage, la première production reprend la phase et l’avancement
sauvegardés, puis le cycle continue : le temps d’arrêt n’est pas compté.
Pour désactiver la production automatique, utiliser la propriété Spring
`astromate.simulation.enabled=false`.

Sur une base vide, l’initialisation crée les quatre systèmes canoniques et les
mesures nominales, avec six heures d’historique synthétique à pas de 10 secondes.
Les composants thermiques et leurs associations aux métriques sont définis par
le catalogue applicatif. Les horodatages sont persistés en UTC. Les démarrages
suivants conservent les mesures, l’historique, les alertes et les événements
existants, sans réensemencer un historique devenu vide.
Aucun reset complet automatique n’est effectué.

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

Les tests d’intégration couvrent le calcul des seuils, l’agrégation des états,
les domaines physiques et l’agrégation des températures des radiateurs. Le
frontend vérifie aussi la présentation des états d’alerte et des systèmes hors
ligne.

Pour lancer l’application empaquetée avec le frontend servi par Spring Boot :

```sh
java -jar target/astromate-0.1.0-SNAPSHOT.jar
```

Lancer cette commande depuis `backend`; l’interface sera disponible sur
<http://127.0.0.1:8080>.
