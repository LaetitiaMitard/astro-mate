# Déroulé des démos Copilot

Ce document décrit les démonstrations en direct qui accompagnent la
présentation « Initiation à l’IA agentique ». Elles suivent l’ordre des slides :
chaque démo correspond à une slide marquée « ▶ Démo » et s’appuie sur la
précédente.

Les démos de la partie 4 font passer une même évolution, la mascotte Orbit du
[backlog](backlog.md), par toutes les étapes du parcours. La partie 5 rejoue
ensuite cette évolution en mode délégué pour comparer les deux approches.

## Vue d’ensemble

| Démo | Partie | Slide | Ce qu’on montre |
|---|---|---|---|
| 1 | 1 · Bases de l’IA | Que contient le contexte d’un agent ? | `/context` sur une première question |
| 2 | 2 · Assets IA | Les instructions | Les instructions du dépôt, puis `/init` |
| 3 | 2 · Assets IA | Les skills | Le skill `enquete-regression` sur l’incident |
| 4 | 2 · Assets IA | Les agents personnalisés | L’agent `revue-accessibilite` en lecture seule |
| 5 | 2 · Assets IA | Les hooks | Le hook qui protège les documents de référence |
| 6 | 4 · Ingénierie | Comprendre le besoin | `grill-me` sur la feature Orbit |
| 7 | 4 · Ingénierie | Planifier | `/plan` sur la feature Orbit |
| 8 | 4 · Ingénierie | Implémenter sous contrôle des tests | Les tests d’Orbit d’abord, puis le code |
| 9 | 4 · Ingénierie | Relire et passer le relais | La revue du diff d’Orbit |
| 10 | 5 · Concepts avancés | Les worktrees Git, La délégation asynchrone | Orbit délégué dans un worktree |
| 11 | 5 · Concepts avancés | Les evals | L’enquête avec et sans le skill |

La partie 3 (interfaces et commandes) et la slide MCP n’ont pas de démo
dédiée : les commandes de la partie 3 sont utilisées au fil des autres démos.

## Préparation

- **Outils** : Java 21, Node.js 20 ou ultérieur, Copilot CLI et VS Code avec
  Copilot. Les démos sont écrites pour Copilot CLI ; le hook de la démo 5 ne
  fonctionne qu’avec lui.
- **Historique Git complet** : `git rev-parse --is-shallow-repository` doit
  répondre `false`, sinon l’enquête de la démo 3 est impossible.
- **Application** : vérifier avant l’atelier que `./mvnw verify` passe depuis
  `backend/` et que l’application démarre (voir le README).
- **Branche de travail** : faire les démos sur une branche dédiée, par exemple
  `demo/atelier`, sans modifier l’historique partagé.
- **Commandes** : vérifier que `/context`, `/init`, `/plan`, `/agent`,
  `/create-skill` et `/delegate` sont disponibles dans la version de Copilot
  utilisée. Si une commande manque, demander le même objectif en langage
  naturel, sans prétendre qu’elle a été exécutée.

Assets fournis dans `.github/`, actifs dès l’ouverture du projet :

- [`copilot-instructions.md`](../.github/copilot-instructions.md) : instructions
  du dépôt ;
- [`skills/enquete-regression/`](../.github/skills/enquete-regression/SKILL.md) :
  démarche d’enquête sur une régression ;
- [`skills/grill-me/`](../.github/skills/grill-me/SKILL.md) : clarification
  d’un besoin par questions successives ;
- [`agents/revue-accessibilite.agent.md`](../.github/agents/revue-accessibilite.agent.md) :
  relecteur accessibilité en lecture seule ;
- [`hooks/proteger-references.json`](../.github/hooks/proteger-references.json) :
  refuse toute modification de `docs/specifications.md` et `docs/edb.md` par
  les outils d’édition de l’agent.

**Régression volontaire.** Le comportement décrit dans le
[ticket incident](incidents.md) est une régression introduite pour l’atelier :
les démos la diagnostiquent, mais ne la corrigent jamais.

---

## Partie 1 · Bases de l’IA

### Démo 1 — Ce que contient le contexte

**Slide :** « Que contient le contexte d’un agent ? »

1. Lancer Copilot CLI à la racine du dépôt.
2. Avant toute question, lancer `/context` : les instructions de départ, la
   description des outils et `copilot-instructions.md` occupent déjà de la
   place.
3. Demander : « Explique en cinq lignes l’architecture du backend
   d’AstroMate. »
4. Relancer `/context` : l’historique et les fichiers lus par l’agent ont
   fait grossir le contexte.

**À montrer :** le contexte n’est pas qu’une conversation. Chaque fichier lu
et chaque résultat d’outil s’y accumulent.

---

## Partie 2 · Assets IA

### Démo 2 — Les instructions

**Slide :** « Les instructions »

1. Ouvrir [`.github/copilot-instructions.md`](../.github/copilot-instructions.md)
   et le commenter : forme du projet, commandes de build et de test,
   conventions, consigne sur la régression volontaire.
2. Lancer `/init`. Le fichier généré (par exemple `AGENTS.md`, ignoré par Git)
   est un point de départ, pas une vérité.
3. Comparer les deux fichiers : qu’a deviné `/init`, qu’a-t-il manqué, qu’a-t-il
   inventé ?

**À montrer :** des instructions maintenues par l’équipe apportent des règles
qu’un fichier généré ne peut pas deviner. Chaque ligne consomme du contexte à
chaque requête (démo 1).

### Démo 3 — Un skill d’enquête

**Slide :** « Les skills »

1. Ouvrir le [ticket incident](incidents.md) et présenter le skill
   [`enquete-regression`](../.github/skills/enquete-regression/SKILL.md) : sa
   description, sa démarche et le rapport attendu.
2. Dans une nouvelle session, demander : « Analyse l’incident décrit dans
   docs/incidents.md et identifie le commit qui l’a introduit, avec le SHA et
   la preuve. » Sans le nommer, l’agent doit charger le skill à partir de sa
   description.
3. Vérifier le diagnostic soi-même avec `git show` sur le commit proposé : le
   diff doit expliquer le symptôme, le message de commit ne suffit pas.
4. Optionnel : demander à `/create-skill` de générer un skill équivalent sous un
   autre nom, comparer les deux, puis supprimer le skill généré pour éviter
   deux skills concurrents.

**À montrer :** le skill structure l’enquête, mais le diagnostic se vérifie
dans l’historique. Ne rien corriger.

### Démo 4 — Un agent personnalisé en lecture seule

**Slide :** « Les agents personnalisés »

1. Présenter [`revue-accessibilite.agent.md`](../.github/agents/revue-accessibilite.agent.md) :
   son rôle, ses consignes et ses outils limités à `read` et `search`.
2. Le sélectionner avec `/agent`, puis demander : « Relis l’accessibilité du
   tableau de bord : frontend/src/features/dashboard/. »
3. Lui demander ensuite de corriger un des points relevés : il ne peut pas
   modifier de fichier.

**À montrer :** limiter les outils d’un agent est un vrai garde-fou. Cet agent
reviendra à la démo 9 pour relire Orbit.

### Démo 5 — Un hook de protection

**Slide :** « Les hooks »

1. Présenter [`proteger-references.json`](../.github/hooks/proteger-references.json)
   et son script : un hook `preToolUse` qui examine le fichier ciblé par les
   outils d’édition.
2. Demander à l’agent : « Ajoute une section sur la mascotte Orbit dans
   docs/specifications.md. » L’édition est refusée avant toute écriture, avec
   le motif renvoyé par le hook.
3. Observer si l’agent tente de contourner le refus par une commande shell.
   Le hook ne couvre pas ce cas : c’est la permission demandée pour la
   commande qui fait barrage.

**À montrer :** un hook s’exécute à chaque fois, sans dépendre de la bonne
volonté du modèle, mais il ne protège que ce qu’il couvre.

---

## Partie 4 · Méthodes d’ingénierie : la feature Orbit

Les démos 6 à 9 font passer la feature « La mascotte Orbit accompagne le
diagnostic » du [backlog](backlog.md) par le parcours de la présentation. Le
backlog joue le rôle de spécification : objectif, trois tickets, critères
d’acceptation, définition de terminé et limites.

### Démo 6 — Comprendre le besoin

**Slide :** « Comprendre le besoin »

1. Dans une nouvelle session, demander : « Utilise grill-me sur la feature
   Orbit de docs/backlog.md. »
2. Répondre aux questions de l’agent, une à la fois. Exemples attendus : quel
   système mettre en avant si plusieurs sont dégradés, que faire si l’état est
   inconnu, quel ton pour les messages.
3. Montrer le [glossaire](../GLOSSARY.md) : l’agent doit employer les mots du
   métier (système, état, alerte) et non des synonymes ambigus.

**À montrer :** l’agent pose les questions au lieu de deviner. Le résultat est
un besoin clarifié, sans ligne de code.

### Démo 7 — Planifier

**Slide :** « Planifier : du quoi au comment »

1. Lancer `/plan` avec la feature Orbit et ses trois tickets, en joignant les
   réponses de la démo 6.
2. Relire le plan avec le backlog : critères d’acceptation couverts, fichiers
   et tests à créer, ordre des tickets, respect des limites (pas de nouvelle
   API, pas de nouvel état métier, pas de modification de la simulation).
3. Faire corriger le plan s’il oublie un critère ou sort du périmètre.

**À montrer :** on corrige le plan, pas le code. Une mauvaise direction coûte
ici quelques secondes.

### Démo 8 — Implémenter sous contrôle des tests

**Slide :** « Implémenter sous contrôle des tests »

1. Faire écrire d’abord les tests du ticket 1 : une réaction pour chaque état
   global (NOMINAL, WARNING, CRITICAL, OFFLINE, UNKNOWN), le système mis en
   avant et le cas sans système.
2. Lancer `npm test` depuis `frontend/` : les nouveaux tests échouent.
3. Demander l’implémentation « pour que les tests passent, sans modifier les
   tests », puis relancer `npm test`.
4. Enchaîner sur les tickets 2 et 3 si le temps le permet. Sinon, s’en tenir
   au ticket 1 en direct.

**À montrer :** les tests sont un juge objectif. Vérifier dans le diff que
l’agent n’a pas modifié les tests pour les faire passer.

### Démo 9 — Relire avant de livrer

**Slide :** « Relire et passer le relais »

1. Sélectionner l’agent `revue-accessibilite` (démo 4) et lui faire relire le
   diff d’Orbit.
2. Relire soi-même le diff avec la définition de terminé du backlog : aucune
   nouvelle règle de seuil, aucun état métier ajouté, aucune ressource
   distante, mention « Simulation — données fictives, non opérationnelles »
   conservée.
3. Lancer `./mvnw verify` depuis `backend/`, démarrer l’application et
   parcourir le tableau de bord au clavier, avec la réduction des animations
   activée.
4. Optionnel : demander une note de passation (décisions, fait, reste à faire)
   pour reprendre dans une nouvelle session.

**À montrer :** l’agent aide à vérifier, le développeur décide. C’est aussi
l’occasion de reprendre les angles de la slide « Mettre en production » :
accessibilité, qualité, cohérence.

---

## Partie 5 · Concepts avancés

### Démo 10 — Déléguer Orbit dans un worktree

**Slides :** « Les worktrees Git » et « La délégation asynchrone »

1. Créer un worktree depuis la branche de départ, sans Orbit :
   `git worktree add ../astro-mate-orbit -b demo/orbit-delegue`.
2. Y lancer une session Copilot et lui confier toute la feature en une
   demande : « Implémente la feature Orbit de docs/backlog.md, tickets 1 à 3,
   en respectant les critères d’acceptation et la définition de terminé. »
   La laisser tourner en arrière-plan, ou la confier à l’agent cloud avec
   `/delegate`.
3. Pendant ce temps, continuer à travailler dans le dossier principal.
4. Comparer ensuite le résultat délégué avec la version guidée des démos 6 à 9.

**À montrer :** chaque agent travaille dans son dossier, sans conflit. La
délégation n’est fiable que si la tâche est bien spécifiée et le résultat
vérifiable. Avec `/delegate`, l’agent cloud ouvre une pull request : la fermer
après la démo.

### Démo 11 — Évaluer un skill

**Slide :** « Les evals »

1. Ouvrir deux sessions neuves sur deux branches de travail : l’une avec le
   skill `enquete-regression`, l’autre où le dossier du skill est retiré.
2. Envoyer exactement la même demande que dans la démo 3.
3. Comparer les deux réponses sur des critères fixés à l’avance : commit
   correct, preuves tirées du diff, faits séparés des hypothèses, nombre
   d’étapes.

**À montrer :** on mesure l’effet d’un skill sur un cas reproductible au lieu
de le juger à l’impression.

---

## Conclusion

Revenir sur le fil complet : un contexte maîtrisé, des assets qui guident et
protègent l’agent, une évolution menée étape par étape, puis la même évolution
déléguée et une mesure objective. Terminer sur le message de la présentation :
l’IA accélère, le développeur reste responsable.
