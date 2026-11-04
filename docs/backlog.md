# Backlog

## Feature — La mascotte Orbit accompagne le diagnostic

### Objectif

Ajouter au tableau de bord une mascotte de station, « Orbit », qui réagit à l’état
global déjà calculé et aide à repérer le système à examiner. La mascotte apporte
une touche ludique sans cacher les états, alertes ou informations existants.
Elle ne crée pas de nouvel état métier et ne modifie pas la simulation.

Les trois tickets ci-dessous sont à réaliser dans l’ordre.

### Ticket 1 — Définir les réactions d’Orbit

Définir, à partir des états globaux existants, une expression et un court message
en français pour NOMINAL, WARNING, CRITICAL, OFFLINE et UNKNOWN. Déterminer
également quel système mettre en avant lorsqu’un système nécessite une attention
particulière. Les messages doivent rester cohérents avec les états et alertes
réels d’AstroMate.

**Critères d’acceptation**

- Chaque état global existant possède une réaction explicite.
- Le système mis en avant, lorsqu’il y en a un, est déterminé à partir des
  informations déjà disponibles; aucune nouvelle règle de seuil n’est introduite.
- Le cas sans système à mettre en avant est prévu.

### Ticket 2 — Afficher Orbit sur le tableau de bord

Créer une mascotte simple à l’aide du CSS ou des assets locaux déjà présents, et
l’afficher sur le tableau de bord avec son expression, son message et l’état
correspondant.

**Critères d’acceptation**

- L’apparence et le message changent selon l’état global actuel.
- La mascotte ne remplace ni ne masque les indicateurs, alertes et statuts
  existants.
- Toute animation respecte la préférence système de réduction des animations.
- Aucun asset distant ni service externe n’est nécessaire.

### Ticket 3 — Relier Orbit au système à examiner

Rendre la mascotte et son éventuel lien vers le système concerné utilisables au
clavier et compréhensibles par les technologies d’assistance. Ajouter des tests
des réactions et du parcours de navigation.

**Critères d’acceptation**

- Si un système doit être examiné, une action accessible permet d’ouvrir son
  détail.
- En l’absence de système à mettre en avant, aucun lien invalide n’est affiché.
- Le statut et le message restent compréhensibles sans dépendre uniquement de la
  couleur ou de l’illustration.
- Des tests couvrent les états globaux, le système mis en avant, son absence et
  l’accès au clavier.

### DoD de la feature

- Les critères d’acceptation des trois tickets sont satisfaits.
- Les tests couvrent les réactions, le système mis en avant, son absence et le
  parcours clavier.
- Les tests concernés passent; toute validation non exécutée est signalée
  explicitement.
- La mascotte ne masque aucune information existante, respecte la réduction
  des animations et utilise uniquement des ressources locales.
- Le diff est relu et n’introduit ni nouvelle règle de seuil, ni état métier,
  ni modification de la simulation.

### Limites

- Pas de nouvelle API, de persistance, de compte utilisateur ni de génération
  par LLM.
- Ne pas changer les seuils, le calcul des états ou le scénario thermique.
- Conserver la mention « Simulation — données fictives, non opérationnelles ».
