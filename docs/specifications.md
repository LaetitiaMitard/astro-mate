# Spécifications fonctionnelles et techniques — AstroMate

**Statut :** décisions de cadrage confirmées le 16 octobre 2026.
**Nature :** application de démonstration locale; toutes les télémétries et tous les seuils sont fictifs et non opérationnels.

## 1. Objectif et sources

AstroMate permet à des participants d’observer une station simulée, de repérer des anomalies de télémétrie et d’en consulter les causes et l’historique.

Les documents de référence sont l’expression de besoin (`edb.md`), le modèle de données (`modele-donnees-astromate.md`) et les deux maquettes corrigées. La maquette « tableau de bord » représente bien le tableau de bord; aucune maquette distincte du journal n’est fournie. Les exemples visuels qui contredisent les règles ci-dessous ne constituent pas des données de référence.

## 2. Périmètre

### Inclus en V1

- Une station simulée, fonctionnant localement sans connexion à des équipements ni à des services externes.
- Un tableau de bord avec l’état global, quatre indicateurs, les alertes ouvertes et l’accès aux systèmes et au journal.
- Une liste des quatre systèmes canoniques : support de vie, contrôle thermique, alimentation électrique et communications.
- Une fiche minimale pour chaque système; le contrôle thermique dispose du détail de référence avec métriques, composants, historique et événements.
- Une vue complète des alertes et un journal d’événements filtrable et paginé.
- Des données fictives actualisées toutes les 10 secondes et un scénario thermique déterministe.
- Une persistance locale des mesures, alertes et événements selon les durées définies dans ce document.

### Hors périmètre

- Connexion ou pilotage d’une station, d’un capteur ou d’un système opérationnel réel.
- Gestion des missions, des expériences, de l’équipage ou des opérations de maintenance.
- Acquittement manuel d’alertes, planification d’intervention, notifications externes, authentification ou droits différenciés.
- Plusieurs stations, données personnelles ou données opérationnelles.
- Export du journal et actions de modification depuis l’interface.

Les entrées Équipage, Expériences et Maintenance peuvent rester visibles dans la navigation ou les onglets, mais doivent être désactivées et indiquer qu’elles ne sont pas disponibles en V1. L’entrée Settings est masquée.

## 3. Utilisateurs et parcours

L’équipage, l’équipe au sol, les ingénieurs et la direction de mission sont des personas, pas des rôles applicatifs. Tous disposent de la même interface en lecture seule, sans compte ni authentification.

1. **Surveiller la station** : consulter l’état global et les quatre indicateurs; repérer une alerte ouverte ou un système dégradé.
2. **Examiner un système** : ouvrir la liste, consulter l’état et la dernière mesure, puis ouvrir le détail du contrôle thermique.
3. **Diagnostiquer une anomalie** : examiner les métriques et leurs seuils, la courbe des six dernières heures, les composants et les événements associés.
4. **Rechercher un événement** : ouvrir le journal, filtrer par période, niveau et catégorie, rechercher un message, une source ou un système, puis parcourir les résultats.
5. **Suivre une alerte** : consulter les alertes ouvertes en priorité; l’alerte se résout automatiquement lorsque la mesure revient à la normale.

## 4. Écrans et comportement attendu

### 4.1 Tableau de bord

Afficher :

- l’état global de la station, calculé à partir des systèmes;
- O₂, CO₂, température et énergie disponible, chacun avec valeur, unité, état et plage nominale;
- jusqu’à quatre alertes ouvertes, triées par niveau (critique avant avertissement) puis par date décroissante;
- un lien vers la liste complète des alertes;
- un schéma de station dont les zones naviguent vers les quatre systèmes canoniques;
- l’état du simulateur et l’heure de la dernière mesure valide.

À l’installation initiale, les indicateurs sont nominaux et le panneau des alertes affiche « Aucune alerte ouverte ». Les messages de succès ou de rétablissement ne sont pas placés dans ce panneau : ils appartiennent au journal. Une carte « température en hausse » ne constitue pas une alerte sans franchissement de seuil.

L’indicateur de connexion doit décrire le simulateur (« Simulation active » ou une indisponibilité), jamais prétendre qu’une station réelle est connectée. Une panne de l’API ne doit pas remplacer les données par des valeurs nominales fictives de secours.

### 4.2 Liste des systèmes

Pour chacun des quatre systèmes, afficher son nom, son état dérivé et la date de sa dernière mesure valide reçue. Un système peut être ouvert depuis la liste ou depuis le schéma du tableau de bord.

### 4.3 Détail du contrôle thermique

Les onglets Vue d’ensemble, Composants et Historique sont fonctionnels. Maintenance est désactivé.

- **Vue d’ensemble** : valeurs courantes, unités, plages nominales et seuils critiques; courbe de température des six dernières heures; événements récents.
- **Composants** : Radiateur A, Radiateur B, Pompe principale et Échangeur thermique, avec état, valeur, unité et dernière mise à jour.
- **Historique** : mesures horodatées, sur la fenêtre de six heures.
- **Maintenance** : aucune planification; l’onglet et le bouton « Planifier une intervention » sont désactivés.

La température système affichée est la moyenne des températures des radiateurs, arrondie à 0,1 °C. L’état et les alertes sont déterminés selon la mesure de radiateur la plus sévère, afin qu’une moyenne ne masque pas une anomalie.
Cette température système est une projection calculée à partir des métriques des deux radiateurs; elle n’est pas enregistrée comme une mesure indépendante.

### 4.4 Alertes

La vue complète présente les alertes ouvertes et résolues, les alertes ouvertes en premier, et permet de filtrer par niveau et système. Elle ne permet ni acquittement ni résolution manuelle. Le niveau est immuable pour une alerte donnée : lors d’une escalade ou d’une désescalade, l’alerte précédente est résolue et une nouvelle alerte est créée.

### 4.5 Journal des événements

Le journal n’a pas de maquette dédiée. La V1 fournit néanmoins :

- filtres par période, niveau et catégorie;
- recherche sur le message, la source et le nom du système;
- tri chronologique ascendant ou descendant, descendant par défaut;
- pagination de 20 lignes, avec les événements les plus récents en premier;
- période par défaut des sept derniers jours;
- aucune fonction d’export ni action par ligne.

Les catégories sont SYSTEM, ALERT, MAINTENANCE, OPERATION et DATA. Les niveaux sont INFO, WARNING et CRITICAL; SUCCESS n’est pas un niveau. Les opérations réussies sont des événements INFO. Les mesures nominales ne génèrent pas chacune un événement.

## 5. Règles métier

### 5.1 Unités et seuils

Les valeurs sont générées, stockées et affichées dans la même unité canonique. La conversion utilisée pour la donnée CO₂ de la maquette est explicite : 0,04 % = 400 ppm.

| Métrique | Plage nominale inclusive / avertissement | Seuil critique fictif |
|---|---:|---:|
| O₂ | 19–23 % vol. | <17 % ou >25 % |
| CO₂ | 0–1 000 ppm | >2 000 ppm |
| Température | 18–26 °C | <15 °C ou >30 °C |
| Énergie disponible | 20–100 % | <10 % |
| Débit de fluide | 80–200 L/min | Aucun seuil critique en V1 |
| Pression | 1,5–3,0 bar | Aucun seuil critique en V1 |
| Consommation thermique | <2,0 kW | Aucun seuil critique en V1 |

Une valeur égale à une borne nominale reste nominale, sauf si la borne est explicitement exclusive. WARNING commence hors de la plage nominale; CRITICAL commence strictement au-delà du seuil critique. Ainsi, 2 000 ppm de CO₂ et 30 °C restent WARNING, tandis qu’une consommation de 2,0 kW est WARNING puisque sa borne haute est exclusive.

Les seuils critiques sont des paramètres pédagogiques, non des recommandations de sécurité. Les cartes du tableau de bord affichent les plages nominales; les détails système exposent aussi les seuils critiques.

### 5.2 Validité d’une mesure

Les limites de validité physique sont distinctes des seuils de fonctionnement :

- O₂ et énergie : 0–100 %;
- CO₂, débit, pression et consommation : valeurs positives ou nulles;
- température : au moins −273,15 °C.

Une valeur invalide n’est pas enregistrée comme mesure valide et ne déclenche pas une alerte de seuil. La dernière mesure valide est conservée jusqu’à ce qu’elle devienne périmée. Un événement DATA WARNING est créé au début d’un épisode de valeurs invalides; un événement DATA INFO est créé au rétablissement. Les échantillons invalides répétés pendant le même épisode ne créent pas d’événements supplémentaires.

### 5.3 Fraîcheur et disponibilité

- Une nouvelle télémétrie est produite toutes les 10 secondes.
- Une métrique requise devient UNKNOWN si aucune mesure valide n’a été reçue depuis plus de 30 secondes.
- Toutes les métriques déclarées pour un système sont requises pour son état.
- Une indisponibilité OFFLINE est un signal explicite de disponibilité simulée, distinct de données absentes ou périmées. Pendant OFFLINE, la télémétrie est suspendue et les événements de péremption ne sont pas émis; les transitions hors ligne et de rétablissement sont journalisées en WARNING puis INFO.
- Si l’API locale est indisponible, l’interface signale l’erreur, conserve les valeurs avec leurs horodatages réels et ne les présente pas comme fraîches. L’état devient UNKNOWN après le délai de fraîcheur.

### 5.4 États dérivés

Les métriques ont les états NOMINAL, WARNING, CRITICAL ou UNKNOWN. Les composants ont ces mêmes états; si des métriques leur sont rattachées, leur état est dérivé de la mesure la plus sévère. Un composant sans métrique peut recevoir un état simulé. OFFLINE est réservé au système et à la station.

L’état d’un système est calculé depuis ses métriques requises, ses alertes ouvertes et sa disponibilité explicite. L’état global de la station est calculé depuis les systèmes. Dans les deux cas, la priorité est :

**CRITICAL > WARNING > OFFLINE > UNKNOWN > NOMINAL**

CRITICAL ou WARNING connus ne sont pas masqués par une autre métrique inconnue. Une métrique manquante n’est pas une alerte de seuil. Si l’état global est OFFLINE, l’interface précise le ou les systèmes concernés. Un système sans métrique est NOMINAL lorsqu’il est explicitement en ligne, et OFFLINE lorsqu’il est explicitement hors ligne.

### 5.5 Cycle de vie des alertes et des événements

- Un franchissement de seuil WARNING ou CRITICAL ouvre une alerte liée à la métrique et au système.
- Le retour dans la plage nominale résout automatiquement l’alerte.
- Une transition WARNING → CRITICAL résout l’alerte WARNING et crée une alerte CRITICAL; CRITICAL → WARNING fait l’inverse. Chaque épisode garde son niveau et ses dates.
- Chaque transition d’état importante et chaque création/résolution d’alerte produit un événement technique. Les transitions de seuil ont le niveau correspondant; une résolution est INFO.
- Aucune hystérésis ni temporisation n’est appliquée : chaque franchissement de seuil est enregistré.
- INFO reste un niveau autorisé pour une alerte explicitement définie, mais aucune règle ne génère automatiquement une alerte INFO en V1.
- Les données périmées, invalides et les indisponibilités produisent des événements DATA ou SYSTEM, pas des alertes de seuil.

### 5.6 Jeu simulé, historique et persistance

Valeurs nominales initiales :

- O₂ : 21 % vol.; CO₂ : 400 ppm; température système : 22,3 °C; énergie : 78 %;
- température Radiateur A : 22,1 °C; Radiateur B : 22,4 °C; débit : 120 L/min; pression : 2,1 bar; consommation : 1,2 kW.

Sur une base vide, l’initialisation idempotente crée les systèmes, métriques, composants et six heures de mesures nominales synthétiques. Elle ne charge pas d’alerte ouverte. Les dates de démonstration sont calculées relativement à l’initialisation, pas copiées depuis les dates d’une maquette.

Le simulateur fait ensuite évoluer les deux radiateurs selon un cycle répétable : 60 s à 22,1/22,4 °C; 60 s à 27 °C chacun; 60 s à 31 °C chacun; 60 s à 22,1/22,4 °C, puis répétition. Les autres métriques restent nominales. Le cycle est persisté, suspendu lorsque l’application s’arrête et repris à la phase sauvegardée au redémarrage.

La base SQLite est locale. L’initialisation à chaque démarrage est idempotente et ne supprime ni les mesures ni l’historique existants. Un reset complet est une opération explicite séparée.

- Mesures : rétention glissante de six heures.
- Événements : rétention de sept jours; les événements liés à une alerte encore ouverte sont gardés jusqu’à sa résolution.
- Alertes résolues : rétention de sept jours; alertes ouvertes : conservées jusqu’à leur résolution.

Les horodatages sont stockés en UTC et présentés dans le fuseau local du navigateur.

## 6. Modèle de données

Le modèle détaillé et ses relations sont décrits dans [le modèle de données AstroMate](./modele-donnees-astromate.md). La V1 dérive les états au lieu de faire coexister des valeurs saisies et des états calculés contradictoires. Une métrique peut être rattachée facultativement à un composant. Les indicateurs agrégés sont des projections et ne dupliquent pas les mesures sources. Les événements restent associés facultativement au système et conservent une source textuelle; ils ne nécessitent pas de lien composant direct.

La station est unique : son état global est une agrégation, pas une entité multi-station à sélectionner.

## 7. Contrat API

L’API HTTP locale est **contract-first**; [le contrat OpenAPI](./api/openapi.yaml) est la source de vérité. Le frontend reçoit les notifications de changement par Server-Sent Events, puis relit par HTTP les ressources concernées. L’API n’expose que des opérations de lecture à l’interface; le simulateur met à jour les données en interne.

Les ressources couvrent le tableau de bord, les systèmes, les séries de mesures, les alertes et le journal. Les dates sont des dates-heures ISO 8601 en UTC. Les erreurs utilisent `application/problem+json`; les paramètres invalides renvoient 400, un identifiant inconnu 404 et une indisponibilité de simulation/base locale 503.

## 8. Exigences transverses

- Application en français, utilisable sur un poste de travail standard.
- Cible d’accessibilité WCAG 2.1 AA : navigation clavier, libellés accessibles, contraste suffisant, états non communiqués par la couleur seule; les graphiques ont une alternative textuelle/tabulaire.
- Mention permanente : « Simulation — données fictives, non opérationnelles ».
- L’API écoute uniquement sur l’interface locale; aucune télémétrie, donnée personnelle ou requête vers un service externe.
- Stack retenue : React + TypeScript + Vite; Java 21 + Spring Boot 3; SQLite locale. Les assets React compilés sont servis par l’application Spring Boot en usage normal; Vite avec proxy API est réservé au développement. Aucun design system supplémentaire n’est imposé.

## 9. Tests et critères d’acceptation

Les tests automatisés couvrent les règles de seuil, les égalités aux bornes, les unités, l’agrégation moyenne/pire mesure, les états système et station, la fraîcheur, les valeurs invalides, les transitions d’alerte, la rétention, l’idempotence du seed, la reprise du scénario, les filtres/pagination API et les états UI accessibles.

### Parcours d’acceptation

**Tableau de bord nominal**

- Given une base initialisée et le simulateur actif,
- When l’utilisateur ouvre le tableau de bord,
- Then il voit les quatre indicateurs en unités canoniques, leurs plages nominales, l’état global NOMINAL, le statut de simulation et « Aucune alerte ouverte ».

**Franchissement de seuil**

- Given une température système nominale,
- When les deux mesures de radiateur passent à 27 °C,
- Then la métrique et le système sont WARNING et une alerte WARNING ouverte apparaît; l’état global reflète WARNING si aucun état plus prioritaire n’existe.

**Seuil critique puis retour**

- Given une alerte WARNING ouverte,
- When les radiateurs passent à 31 °C puis reviennent dans la plage nominale,
- Then l’alerte WARNING est résolue à l’escalade, une alerte CRITICAL est créée, puis résolue au retour nominal; chaque transition est historisée.

**Mesure périmée**

- Given un système en ligne avec métriques requises,
- When une métrique n’a reçu aucune valeur valide depuis plus de 30 secondes,
- Then cette métrique est UNKNOWN, un événement DATA est présent et l’état système/global suit la priorité convenue sans créer d’alerte de seuil.

**Valeur invalide**

- Given une dernière mesure valide encore fraîche,
- When une valeur hors du domaine physique est reçue,
- Then la valeur invalide n’est pas traitée comme mesure valide, la dernière valeur reste datée, un événement DATA WARNING est écrit et aucun seuil n’est déclenché par cette valeur.

**Disponibilité locale**

- Given l’API ou SQLite indisponible,
- When le frontend ne peut pas actualiser les données,
- Then il signale « Simulation indisponible », n’invente pas de valeurs de secours et ne présente pas les données en cache comme fraîches.

**Recherche dans le journal**

- Given des événements des sept derniers jours,
- When l’utilisateur applique filtres, recherche, tri ou pagination,
- Then seuls les événements correspondants sont affichés, 20 par page, du plus récent au plus ancien par défaut, sans fonction d’export.

## 10. Décisions hors spécification détaillée

Les versions mineures de Spring Boot 3, des bibliothèques et des outils de test seront fixées lors de l’initialisation du code, en cohérence avec Java 21 et les dépendances effectivement disponibles. Aucun déploiement hors du poste local n’est prévu dans cette V1.
