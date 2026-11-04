# Exercices

Cette fiche propose un parcours en quatre exercices à réaliser dans l’ordre.
Les commandes `/init`, `/create-skill`, `/debug` et `/plan` sont celles de
Copilot Chat : si une commande n’est pas disponible dans l’environnement de
l’atelier, demander à l’assistant de suivre le même objectif sans prétendre
qu’une commande a été exécutée.

Pour les exercices de code, travailler dans un clone personnel ou une branche
de travail, et ne pas modifier l’historique Git partagé. Les tests et commandes
de validation du projet sont décrits dans le README.

## Exercice 1 — Préparer les instructions du projet avec `/init`

### Objectif

Utiliser la commande intégrée `/init` à l’arrivée sur le projet, puis améliorer
le fichier d’instructions généré pour qu’il aide réellement Copilot à travailler
sur AstroMate.

### Étapes

1. Ouvrir le dépôt AstroMate à sa racine et lancer `/init`.
2. Lire le fichier d’instructions créé par Copilot (par exemple `AGENTS.md`).
   Ne pas considérer le contenu généré comme automatiquement exact.
3. Le confronter au README et au dépôt : architecture réellement présente,
   commandes de développement et de validation, langue de la documentation,
   limites de la simulation et éventuelles consignes déjà en place.
4. Réviser le fichier pour supprimer les suppositions et ajouter les consignes
   manquantes ci-dessous.
5. Vérifier que les consignes sont assez précises pour guider une petite
   modification et sa validation.

### Suggestion d'ajout

- **TDD** : pour un bug, commencer par un test qui échoue et reproduit le
  problème, corriger ensuite le code, puis conserver le test de non-régression.
  Pour une évolution, définir le comportement attendu et les tests avant ou
  pendant l’implémentation.
- **Définition of Done (DoD)** : critères vérifiables avant de déclarer un
  changement terminé — critères d’acceptation couverts, tests concernés
  exécutés et réussis, documentation ajustée si nécessaire, aucun changement
  sans rapport avec le besoin.
  
### A vérifier

- **Contexte du projet** : données fictives uniquement, aucune connexion à une
  station ou à des services externes, et pas de nouvelle règle métier sans
  besoin explicite.
- **Validation** : reprendre les commandes réellement documentées dans le
  README, sans inventer des scripts ou une architecture absents du dépôt.


## Exercice 2 — Retrouver et corriger une régression avec un skill

### Objectif

Créer avec `/create-skill` un skill réutilisable pour rechercher l’origine d’une
régression dans l’historique, puis l’utiliser pour identifier le commit fautif
et corriger le bug.

### Ticket du bug

Lire le [ticket incident dans `incidents.md`](incidents.md), qui décrit le
symptôme, les étapes de reproduction, les comportements attendu et observé ainsi
que l’impact.

### Étapes de travail

1. Dans Copilot Chat, lancer `/create-skill` et demander un skill de recherche
   de régression adapté à ce dépôt.
2. Relire le skill créé et le compléter pour qu’il demande au minimum de :
   reproduire le symptôme avec un test, examiner les commits et leurs diffs,
   comparer un état où le comportement fonctionne avec l’état défectueux,
   fournir les éléments qui relient le commit au bug, puis vérifier la
   correction avec le test de non-régression.
3. Faire utiliser ce skill à Copilot sur l’incident de [`incidents.md`](incidents.md). Lui demander de
   citer le commit suspect (SHA et raison) et de distinguer les faits vérifiés
   des hypothèses.
4. Vérifier le diagnostic dans l’historique et dans le code. Ne pas retenir un
   commit uniquement parce que son message semble pertinent.
5. Faire résoudre le bug à Coipilot

### Résultat attendu et critères de fin

- Le skill est enregistré à l’emplacement reconnu par l’environnement de
  l’atelier et contient une procédure réutilisable, pas uniquement la solution
  de cet incident.
- Le commit identifié est justifié par des éléments vérifiables : test,
  changement pertinent et comparaison avec l’état précédent.
- Tous les tests passent et l'application est opératrionnelle.

## Exercice 3 — Analyser la requête avec `/debug`

### Objectif

Utiliser `/debug` pour comprendre ce qui s’est passé pendant la requête Copilot
de l’exercice 2 : quelles informations ont été prises en compte, où le
diagnostic a déraillé ou abouti, et quelles vérifications manquaient.

### Étapes

1. Dans la même conversation Copilot que l’exercice 2, lancer `/debug` et lui
   demander d’analyser la requête ayant servi à rechercher le commit de la
   régression.
2. Constater l'utilisation du skill

## Exercice 4 — Planifier et développer une feature avec `/plan`

### Objectif

Planifier puis développer la feature « La mascotte Orbit accompagne le
diagnostic », décrite avec ses trois tickets, sa DoD et ses limites dans le
[`backlog.md`](backlog.md).

### Déroulement

1. Lancer `/plan` ou séléctionnet le mode "plan" avec la description de la feature et des trois tickets de
   [`backlog.md`](backlog.md) ajouter /grill-me à la fin.
2. Relire le plan : vérifier qu’il respecte les limites, identifie les fichiers
   et tests à explorer, découpe le travail dans l’ordre des dépendances et ne
   suppose pas de nouvel état métier ou de nouvelle API.
3. Faire préciser ou corriger le plan si un critère d’acceptation est oublié.
   Ne commencer l’implémentation qu’une fois le plan cohérent.
4. Quand vous êtes satisfait, appliquer le plan.
5. Vérifier l'application en la lançant et effectuant quelques tests fonctionnels

### Résultat attendu

La feature satisfait les critères d’acceptation des trois tickets et la DoD
définis dans [`backlog.md`](backlog.md), en respectant les limites indiquées.
