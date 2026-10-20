# Modèle de données — AstroMate V1

Ce modèle décrit les données fictives d’une station unique. Les unités sont canoniques et les horodatages sont stockés en UTC. Les états décrits comme dérivés sont calculés à partir des données et ne constituent pas une seconde source de vérité.

## Entités

### System

Représente un système fonctionnel de la station.

| Champ | Description |
|---|---|
| `id` | Identifiant stable du système |
| `name` | Nom canonique : support de vie, contrôle thermique, alimentation électrique ou communications |
| `description` | Description courte |
| `availability` | Disponibilité simulée explicite : `ONLINE` ou `OFFLINE` |
| `status` | État calculé : `NOMINAL`, `WARNING`, `CRITICAL`, `OFFLINE` ou `UNKNOWN` |
| `updatedAt` | Horodatage de la dernière mesure valide reçue pour l’une de ses métriques |

Les métriques O₂ et CO₂ appartiennent au support de vie; la température, le débit, la pression et la consommation thermique au contrôle thermique; l’énergie disponible à l’alimentation électrique. Les communications n’ont pas de KPI principal en V1 et sont nominales lorsqu’elles sont explicitement en ligne.

### Component

Représente un équipement d’un système, par exemple un radiateur, une pompe ou un échangeur thermique.

| Champ | Description |
|---|---|
| `id` | Identifiant stable du composant |
| `systemId` | Système propriétaire |
| `name` | Nom du composant |
| `status` | État calculé depuis ses métriques, ou état simulé si aucune métrique n’y est associée |
| `updatedAt` | Horodatage de la dernière mesure valide associée ou de l’initialisation de l’état simulé |

Une valeur courante et son unité ne sont pas dupliquées sur le composant : elles proviennent de sa métrique. Les composants sans métrique n’affichent pas de valeur courante. Les composants n’ont pas d’état `OFFLINE`; cette disponibilité est portée par le système.

### Metric

Définit une grandeur mesurée pour un système et, facultativement, pour un composant.

| Champ | Description |
|---|---|
| `id` | Identifiant stable de la métrique |
| `systemId` | Système propriétaire |
| `componentId` | Composant associé, facultatif |
| `name` | Nom de la grandeur |
| `unit` | Unité canonique des valeurs reçues, stockées et affichées |
| `warningMin` | Borne basse de la plage nominale, facultative et inclusive par défaut |
| `warningMax` | Borne haute de la plage nominale, facultative |
| `warningMaxInclusive` | Indique si la borne haute appartient à la plage nominale; vrai par défaut |
| `criticalMin` | Seuil critique bas; une valeur strictement inférieure est critique, facultatif |
| `criticalMax` | Seuil critique haut; une valeur strictement supérieure est critique, facultatif |
| `status` | État dérivé de la dernière mesure valide et de sa fraîcheur |

`warningMin` et `warningMax` décrivent la plage nominale affichée. Une valeur hors de cette plage est `WARNING`, sauf si elle franchit un seuil critique. Les indicateurs d’inclusion permettent de représenter la borne exclusive de consommation thermique (`valeur < 2,0 kW`). Les limites sont facultatives pour les métriques à seuil unilatéral. Une métrique sans mesure valide récente est `UNKNOWN`.

La température système affichée est une projection de la moyenne des métriques de température de Radiateur A et Radiateur B, arrondie au dixième le plus proche (demi vers le haut). Les seuils de température s’appliquent à chaque radiateur. L’état et les alertes du système suivent la métrique source la plus sévère; aucune mesure agrégée indépendante n’est stockée.

### Measurement

Représente un échantillon numérique valide d’une métrique.

| Champ | Description |
|---|---|
| `id` | Identifiant de la mesure |
| `metricId` | Métrique mesurée |
| `value` | Valeur numérique dans l’unité canonique de la métrique |
| `measuredAt` | Instant de mesure en UTC |

Les mesures invalides ne sont pas enregistrées comme mesures valides. Les mesures sont conservées sur une fenêtre glissante de six heures.

Le backend initialise les dernières mesures canoniques dans `metric_measurement`. Cette initialisation idempotente reprend O₂, CO₂ et l’énergie depuis la table historique `dashboard_indicator`, et initialise séparément les mesures des deux radiateurs, le débit, la pression et la consommation. `dashboard_indicator` reste une projection de compatibilité; la température courante est calculée depuis les deux radiateurs et n’est pas lue comme une mesure indépendante.

### Alert

Représente un épisode d’anomalie de métrique suivi de son ouverture à sa résolution.

| Champ | Description |
|---|---|
| `id` | Identifiant de l’alerte |
| `systemId` | Système concerné |
| `metricId` | Métrique à l’origine de l’alerte, facultative |
| `level` | `INFO`, `WARNING` ou `CRITICAL` |
| `message` | Description courte |
| `status` | `OPEN` ou `RESOLVED` |
| `detectedAt` | Date de détection en UTC |
| `resolvedAt` | Date de résolution en UTC, absente tant que l’alerte est ouverte |

Le niveau est immuable pour un épisode. Une escalade ou une désescalade résout l’alerte précédente et en crée une nouvelle. Les alertes de seuil sont résolues automatiquement au retour nominal. INFO est un niveau permis, mais aucune règle de génération automatique d’alerte INFO n’est définie en V1.

Les alertes résolues sont conservées sept jours; les alertes ouvertes sont conservées jusqu’à leur résolution.

### StationEvent

Représente un fait technique horodaté consigné dans le journal.

| Champ | Description |
|---|---|
| `id` | Identifiant de l’événement |
| `systemId` | Système concerné, facultatif |
| `level` | `INFO`, `WARNING` ou `CRITICAL` |
| `category` | `SYSTEM`, `ALERT`, `MAINTENANCE`, `OPERATION` ou `DATA` |
| `source` | Source textuelle de l’événement, facultative |
| `message` | Description de l’événement |
| `occurredAt` | Date de l’événement en UTC |

Un événement peut référencer un système et nommer sa source textuellement. Il n’existe pas de lien direct vers un composant dans le modèle V1. Les événements sont conservés sept jours, sauf ceux liés à une alerte encore ouverte, gardés jusqu’à sa résolution.

## Relations

- Un système possède zéro ou plusieurs composants, métriques et alertes.
- Un composant appartient à un seul système et peut être associé à zéro ou plusieurs métriques.
- Une métrique appartient à un système, peut référencer un composant et possède zéro ou plusieurs mesures.
- Un indicateur agrégé, tel que la température du contrôle thermique, est une projection déterministe de ses métriques sources et ne possède pas de mesures indépendantes.
- Une alerte concerne un système et peut référencer la métrique à l’origine de l’anomalie.
- Un événement peut concerner un système; sa source demeure une valeur textuelle.
- La station est unique en V1. Son état est une agrégation des états des systèmes; aucune entité `Station` multi-instance n’est nécessaire.

## Unités et seuils V1

Les bornes nominales sont inclusives par défaut; les seuils critiques sont franchis strictement. La consommation thermique est l’exception à la borne nominale inclusive : son seuil haut est exclusif.

| Métrique | Unité | `warningMin` | `warningMax` | `warningMaxInclusive` | `criticalMin` | `criticalMax` |
|---|---|---:|---:|:---:|---:|---:|
| O₂ | `% vol.` | 19 | 23 | true | 17 | 25 |
| CO₂ | `ppm` | 0 | 1 000 | true | — | 2 000 |
| Température système | `°C` | 18 | 26 | true | 15 | 30 |
| Énergie disponible | `%` | 20 | 100 | true | 10 | — |
| Débit de fluide | `L/min` | 80 | 200 | true | — | — |
| Pression | `bar` | 1,5 | 3,0 | true | — | — |
| Consommation thermique | `kW` | — | 2,0 | false | — | — |

Pour la consommation, seule la borne haute existe et le nominal est strictement inférieur à 2,0 kW; à 2,0 kW, l’état est WARNING. Pour l’énergie, les valeurs supérieures à 100 % sont invalides, et non critiques. Les seuils critiques sont stricts et fictifs/pédagogiques.

## États dérivés

- **Métrique** : `CRITICAL` si une limite critique est strictement franchie; sinon `WARNING` hors de la plage nominale, en tenant compte de l’inclusion de chaque borne; `NOMINAL` dans la plage; `UNKNOWN` si aucune mesure valide n’est disponible ou si la dernière mesure a plus de 30 secondes.
- **Composant** : état le plus sévère de ses métriques associées; état simulé si aucune métrique n’est associée. États possibles : `NOMINAL`, `WARNING`, `CRITICAL`, `UNKNOWN`.
- **Système** : calculé avec la priorité `CRITICAL > WARNING > OFFLINE > UNKNOWN > NOMINAL`. OFFLINE provient uniquement de `availability`; une absence de télémétrie seule produit UNKNOWN. Toutes les métriques déclarées d’un système sont requises; un système sans métrique est NOMINAL s’il est explicitement en ligne.
- **Station** : même agrégation et même priorité que pour un système. En cas d’état OFFLINE, l’interface nomme le ou les systèmes concernés.

Les états connus CRITICAL ou WARNING ne sont pas masqués par une autre métrique UNKNOWN. Un système explicitement OFFLINE conserve cet état même lorsque ses mesures deviennent anciennes.

## Qualité des données et événements

- Domaine physique : O₂ et énergie entre 0 et 100 %; CO₂, débit, pression et consommation positifs ou nuls; température supérieure ou égale à −273,15 °C.
- Toute valeur hors domaine est rejetée comme mesure valide; elle ne déclenche pas d’alerte de seuil et ne remplace pas la dernière mesure valide.
- Le premier échantillon invalide d’un épisode produit un événement DATA WARNING; les répétitions ne génèrent pas de nouveaux événements. Le retour à une valeur valide produit un événement DATA INFO.
- Une mesure en ligne qui dépasse 30 secondes sans nouvelle valeur valide devient UNKNOWN et génère un événement DATA WARNING; son rétablissement est INFO.
- Pendant OFFLINE, les mesures sont suspendues et aucun événement de péremption par métrique n’est émis. La transition du système en OFFLINE est un événement SYSTEM WARNING; le retour en ligne est un événement SYSTEM INFO.
- Les transitions d’alerte et d’état sont journalisées. Les mesures nominales périodiques ne génèrent pas d’événements.
- Les dates sont conservées en UTC; l’interface les formate dans le fuseau local du navigateur.
