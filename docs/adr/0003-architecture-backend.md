# Architecture hexagonale légère du backend

## Statut

Acceptée.

## Contexte

AstroMate reste une application d'atelier locale : un backend Java 21 / Spring
Boot 3, une base SQLite et une interface React servie par le même processus.
Il n'est pas nécessaire de distribuer les composants ni de changer la pile
retenue dans l'[ADR de l'architecture locale](0001-architecture-locale.md).

Le backend était organisé dans un package unique. Ses services combinaient
les règles métier, les requêtes SQL, les réponses HTTP et les déclencheurs
périodiques. Le détail système dépendait notamment du service de tableau de bord
pour obtenir les états agrégés.

L'objectif est de clarifier les responsabilités, de faciliter les évolutions
et de tester le métier sans démarrer Spring ou SQLite.

## Décision

Adopter une architecture hexagonale légère, dans un seul module Maven, avec
trois packages principaux et sans dossiers `in` ou `out` :

```text
fr.astromate/
  AstroMateApplication.java
  domain/
    model/
    service/
  application/
    model/
    service/
    port/
  infrastructure/
    web/
    persistence/
    scheduling/
    notification/
    configuration/
```

Le **domaine** contient les mesures et leurs états, le catalogue des systèmes
et métriques, les règles de validité physique, les seuils, la fraîcheur,
l'agrégation des états et le scénario thermique. Il ne dépend que du JDK.

L'**application** orchestre les lectures, l'enregistrement des mesures,
les épisodes de qualité et d'alerte, la disponibilité, la simulation et
la rétention. Elle définit ses modèles de résultats et les interfaces
nécessaires dans `port`, sans connaître HTTP, JDBC ou Spring.

L'**infrastructure** contient les contrôleurs et DTO HTTP, les accès JDBC,
les notifications SSE, les événements Spring, les déclencheurs périodiques et
l'assemblage des composants. Les contrôleurs sont répartis entre tableau de bord,
systèmes et historique, alertes, journal et télémétrie.

Les dépendances vont de l'infrastructure vers l'application et le domaine,
et de l'application vers le domaine. Le cœur ne référence jamais une
implémentation technique.

## Frontières et fonctionnement

- `StationOverviewService` partage les indicateurs et les états calculés entre
  les consultations du tableau de bord et des systèmes. Le détail système
  ne passe plus par une réponse HTTP de tableau de bord.
- Les ports de consultation renvoient des modèles de lecture applicatifs.
  Les jointures, tris, recherches et paginations restent dans les adaptateurs
  de persistance ; ils ne nécessitent pas un modèle DDD complexe.
- Les DTO HTTP restent dans `infrastructure/web`. `WebResponses` réalise
  leur conversion explicite depuis les résultats applicatifs, sans mapper
  générique ni dépendance supplémentaire.
- Les erreurs applicatives expriment une requête invalide ou une ressource
  inconnue ; l'adaptateur web les traduit vers les codes HTTP existants.
  Les erreurs d'accès à SQLite restent signalées comme une indisponibilité.
- Le port `Transactions` encadre les écritures. Son implémentation Spring
  conserve la participation à une transaction englobante : un cycle thermique
  et ses mesures sont atomiques. L'avancement en mémoire de la simulation
  n'est modifié qu'après commit.
- Le port `MeasurementChanges` publie via Spring. Le transport SSE consomme
  toujours les notifications après commit, jamais après rollback.
- L'horloge de simulation conserve son point d'injection spécifique. Les cas
  d'usage dépendant du temps reçoivent une horloge UTC, sans coupler leur temps
  de lecture à une horloge de simulation remplacée dans un test.

## Alternatives

Une architecture en couches aurait isolé le SQL avec moins de déplacements,
mais n'aurait pas imposé l'indépendance du métier vis-à-vis du framework.
Une Clean Architecture stricte ou une décomposition en microservices aurait
ajouté des abstractions et une complexité disproportionnées pour cet atelier.

Il n'y a pas d'interface par service, de repository générique, de bus
supplémentaire, de CQRS, de JPA ou de nouvelle dépendance.

## Conséquences et validation

Le nombre de fichiers augmente pour rendre les frontières explicites,
et les modèles de lecture et DTO HTTP nécessitent une conversion.
En contrepartie, les règles et les cas d'usage peuvent être testés isolément
et les accès techniques sont remplaçables via leurs ports.

Cette refonte ne change ni les routes, ni le contrat OpenAPI, ni le schéma SQLite,
ni les migrations, ni les comportements de l'atelier. Les données existantes,
les projections de compatibilité et la reprise de simulation sont conservées.

Les tests unitaires couvrent le domaine et l'orchestration sans contexte Spring.
Les tests d'intégration couvrent SQLite, HTTP, les transactions et SSE.
`ArchitectureTest` contrôle les références entre couches et l'emplacement du SQL,
sans ajouter de bibliothèque. La vérification complète reste `./mvnw verify`
depuis `backend/`.
