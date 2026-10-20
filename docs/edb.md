# Expression de besoin — AstroMate

## 1. Objet du document

Ce document constitue le point de départ fonctionnel pour construire les spécifications du projet **AstroMate** avec GitHub Copilot.

Il décrit le contexte, les utilisateurs, les objectifs et le périmètre initial de l’application. Les règles détaillées et les cas limites devront être clarifiés avant l’implémentation, notamment à l’aide de `/grill-me`.

---

## 2. Vision du produit

> Nous avons besoin d’une application web, AstroMate, pour aider l’équipage et l’équipe au sol à surveiller l’état de la station spatiale et diagnostiquer rapidement les anomalies.
>
> L’application doit être simple à utiliser, fiable et fournir une vue actualisée des systèmes critiques et des alertes en cours.

---

## 3. Contexte

Une station spatiale scientifique regroupe plusieurs systèmes critiques nécessaires à la sécurité de l’équipage et à la continuité des opérations :

- surveillance des conditions de vie à bord ;
- contrôle des équipements techniques ;
- détection et traitement des alertes ;
- diagnostic des anomalies ;
- suivi des événements techniques.

Dans le cadre de l’atelier, les informations sont simulées. L’application doit permettre aux participants de comprendre rapidement l’état de la station, d’identifier une anomalie et d’en rechercher la cause.

---

## 4. Objectifs principaux

AstroMate doit permettre de :

1. surveiller l’état général de la station ;
2. consulter les mesures principales des systèmes critiques ;
3. signaler les valeurs anormales ;
4. consulter les alertes récentes ;
5. accéder au détail d’un système ;
6. faciliter le diagnostic d’une anomalie ;
7. conserver un historique simple des événements techniques.

---

## 5. Utilisateurs concernés

### 5.1 Équipage

Consulte l’état général de la station et les alertes nécessitant une attention immédiate.

### 5.2 Équipe au sol

Surveille les indicateurs, analyse les anomalies et assiste l’équipage.

### 5.3 Ingénieurs

Consultent les mesures détaillées, recherchent la cause des anomalies et suivent l’état des composants.

### 5.4 Direction de mission

Dispose d’une vue synthétique de l’état de la station et des incidents en cours.

---

## 6. Périmètre fonctionnel initial

### 6.1 Tableau de bord simplifié

Le tableau de bord doit présenter :

- l’état global de la station ;
- les principaux indicateurs ;
- les alertes récentes ;
- un accès à la liste des systèmes ;
- un accès au journal des événements.

Indicateurs envisagés :

- taux d’oxygène ;
- taux de CO₂ ;
- température ;
- niveau d’énergie disponible.

Chaque indicateur doit afficher :

- son nom ;
- sa valeur courante ;
- son unité ;
- son état : nominal, avertissement ou critique ;
- la plage attendue ou le seuil associé.

### 6.2 Liste des systèmes

L’utilisateur doit pouvoir consulter une liste simple des systèmes de la station avec :

- le nom du système ;
- son état courant ;
- sa dernière mise à jour ;
- un accès à son écran de détail.

Exemples de systèmes :

- contrôle thermique ;
- support de vie ;
- alimentation électrique ;
- communications.

### 6.3 Détail d’un système

L’utilisateur doit pouvoir sélectionner un système et consulter :

- son nom et sa description ;
- son état courant ;
- ses valeurs principales ;
- les seuils associés ;
- l’évolution récente d’une mesure principale ;
- l’état de ses composants ;
- les événements récents associés.

Le contrôle thermique constitue le système de référence pour la première version.

### 6.4 Alertes

L’application doit afficher les alertes récentes avec :

- leur niveau : information, avertissement ou critique ;
- le système concerné ;
- un message court ;
- la date et l’heure de détection.

### 6.5 Journal des événements

L’application doit proposer un historique simple des événements techniques :

- démarrage d’un système ;
- détection d’un dépassement de seuil ;
- retour à la normale ;
- erreur de lecture ou incohérence de donnée ;
- opération de maintenance simulée.

---

## 7. Contraintes générales

### 7.1 Simplicité d’implémentation

La première version doit rester volontairement limitée :

- peu d’écrans ;
- données simulées ;
- règles métier simples ;
- absence de connexion à des équipements réels ;
- absence de workflow complexe ;
- interface utilisable sur un poste de travail standard.

### 7.2 Simplicité d’utilisation

Les informations critiques doivent être compréhensibles rapidement. Les alertes et états dégradés doivent être visuellement identifiables.

### 7.3 Fiabilité

Les données affichées doivent être cohérentes avec les données reçues. Les unités de mesure et les règles de conversion doivent être explicites et testées.

### 7.4 Traçabilité

Les changements d’état importants doivent être historisés afin de faciliter le diagnostic.

### 7.5 Exécution locale

L’application doit pouvoir fonctionner localement avec des données simulées et sans dépendre d’un système opérationnel réel.

### 7.6 Sécurité des données

Le jeu de données de démonstration ne doit contenir aucune donnée opérationnelle, personnelle ou confidentielle réelle.

---

## 8. Hypothèses de départ

Les éléments suivants sont des hypothèses à confirmer pendant la construction des spécifications :

- les données de télémétrie sont simulées ;
- l’application est une application web ;
- l’application concerne une seule station ;
- les données sont actualisées à intervalles réguliers ;
- aucune authentification avancée n’est requise pour la première version ;
- les mesures récentes sont conservées pour alimenter un graphique simple ;
- les seuils d’alerte sont définis dans la configuration de l’application ;
- une anomalie peut provenir d’une mesure, d’un composant ou d’une incohérence de données.

---

## 9. Hors périmètre initial

Les éléments suivants sont hors périmètre de la première version :

- gestion des missions ;
- gestion des expériences scientifiques ;
- gestion détaillée de l’équipage ;
- planification des opérations de maintenance ;
- connexion à une station ou à des capteurs réels ;
- pilotage physique d’un équipement ;
- gestion médicale de l’équipage ;
- authentification ou habilitation de niveau opérationnel ;
- notifications externes ;
- prise de décision automatique ayant un impact réel sur la sécurité ;
- échanges avec des systèmes externes réels.

---

## 10. Critères de réussite du socle

Le socle fonctionnel est considéré comme utilisable lorsque :

- l’utilisateur visualise l’état global de la station ;
- les quatre indicateurs principaux sont affichés avec leur unité ;
- les valeurs anormales sont signalées ;
- les alertes récentes sont visibles ;
- l’utilisateur accède à la liste des systèmes ;
- l’utilisateur consulte le détail du contrôle thermique ;
- l’écran de détail présente une mesure récente, les composants et les événements associés ;
- les parcours principaux sont couverts par des tests automatisés ;
- les règles importantes sont documentées.

---

## 11. Points à clarifier avec GitHub Copilot

Avant de produire les spécifications détaillées, utiliser `/grill-me` pour préciser au minimum :

1. La fréquence de mise à jour des télémétries.
2. Les unités reçues, stockées et affichées.
3. Les seuils des quatre indicateurs principaux.
4. Les niveaux de criticité des alertes.
5. La règle de calcul de l’état global de la station.
6. Le comportement attendu en cas de donnée absente, périmée ou incohérente.
7. La durée d’historique nécessaire au graphique.
8. Les événements à conserver dans le journal.
9. Le comportement attendu lorsque plusieurs alertes concernent le même système.
10. Les exigences minimales d’accessibilité et d’affichage.

---

## 12. Livrables de spécification attendus

À partir de cette expression de besoin, GitHub Copilot devra aider à produire :

- un glossaire du domaine ;
- une description de l’architecture existante ;
- les parcours utilisateurs ;
- les règles métier ;
- les user stories ;
- les critères d’acceptation associés ;
- les états possibles d’un système et d’une alerte ;
- le modèle de données minimal ;
- les contrats d’API ;
- les cas d’erreur et cas limites ;
- la stratégie de tests ;
- les décisions d’architecture significatives.

---

## 13. Instruction suggérée pour GitHub Copilot

```text
Analyse le dépôt et le fichier edb.md sans commencer l’implémentation.

1. Identifie les concepts métier, les acteurs, les parcours et les règles déjà explicites.
2. Distingue clairement les faits, les hypothèses et les informations manquantes.
3. Recherche les incohérences entre l’expression de besoin et le code existant.
4. Construis une liste priorisée de questions fonctionnelles et techniques.
5. Propose ensuite une structure de spécifications composée de :
   - glossaire ;
   - user stories ;
   - règles métier ;
   - critères d’acceptation en Given/When/Then ;
   - modèle de données minimal ;
   - contrats d’API ;
   - cas limites ;
   - stratégie de tests.
6. Ne prends aucune décision métier implicite : signale les arbitrages nécessaires.
7. Ne modifie aucun fichier tant que les questions bloquantes ne sont pas résolues.
8. Respecte le périmètre de la première version : tableau de bord, systèmes, alertes et journal technique uniquement.
```

---

## 14. Évolutions futures

Les fonctionnalités suivantes pourront faire l’objet de spécifications séparées après validation du socle :

- gestion des expériences scientifiques ;
- gestion des missions ;
- planification des maintenances ;
- gestion détaillée de l’équipage ;
- mode d’évacuation d’urgence ;
- notifications vers des systèmes externes.
