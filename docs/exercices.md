# Déroulé de la démo Copilot

Cette démo suit un même fil rouge : donner du contexte à Copilot, enquêter sur
un incident, prendre du recul sur l’échange, puis planifier et réaliser une
évolution du tableau de bord. Chaque séquence s’appuie sur la précédente pour
montrer un workflow continu plutôt qu’une série d’exercices indépendants.

Les commandes `/init`, `/create-skill`, `/debug` et `/plan` sont celles de
Copilot Chat : si une commande n’est pas disponible dans l’environnement de la
démo, demander à l’assistant de suivre le même objectif sans prétendre qu’une
commande a été exécutée.

Pour toute modification de code, travailler dans un clone personnel ou une
branche de travail, sans modifier l’historique Git partagé. Les commandes de
développement et de validation du projet sont décrites dans le README.

## 1. Donner à Copilot le contexte du projet

Ouvrir AstroMate à la racine du dépôt et lancer `/init`. Le fichier généré
(par exemple `AGENTS.md`) sert de point de départ, pas de vérité automatique :
le relire et le confronter au README et au code avant de l’utiliser.

Pendant la démo, vérifier que les instructions décrivent le projet réellement
présent : frontend React/TypeScript/Vite, backend Java/Spring Boot, persistance
locale, données de simulation fictives et commandes de validation documentées.
Les compléter si nécessaire avec les règles utiles au travail de Copilot :

- pour un bug, commencer par un test qui reproduit le problème, puis conserver
  un test de non-régression pour toute correction autorisée ;
- pour une évolution, expliciter le comportement attendu et les tests concernés ;
- avant de conclure, vérifier les critères d’acceptation, exécuter les contrôles
  pertinents et ajuster la documentation si nécessaire ;
- éviter les changements sans rapport avec le besoin et ne pas inventer
  d’architecture, de scripts ou de règles métier.

**À montrer :** les instructions donnent un contexte vérifiable et aident
Copilot à proposer des actions adaptées au dépôt, sans reprendre aveuglément
son contenu généré.

## 2. Enquêter sur l’incident avec un skill

Ouvrir le [ticket incident](incidents.md), puis demander à `/create-skill` de
créer un skill réutilisable pour rechercher l’origine d’une régression dans un
dépôt. Relire le skill et s’assurer qu’il propose une démarche fondée sur des
preuves : reproduire le symptôme, examiner les commits et leurs diffs, comparer
un état fonctionnel à l’état observé, et séparer les faits vérifiés des
hypothèses.

Dans une nouvelle requête, faire utiliser ce skill pour analyser l’incident.
Demander le commit suspect (SHA et justification), puis vérifier le diagnostic
dans l’historique et dans le code plutôt que de se fier au seul message de
commit.

**Point d’attention pour cette démo :** le comportement décrit par cet incident
correspond à une régression volontaire de l’atelier. L’objectif est de la
reproduire et de montrer comment étayer un diagnostic, pas de la corriger.
Ne pas modifier le code ou les tests pour changer ou masquer ce comportement.

**À montrer :** un skill réutilisable structure l’enquête, mais ne remplace ni
la vérification humaine ni les éléments concrets de l’historique.

## 3. Débriefer l’échange avec `/debug`

Dans la conversation de l’enquête, lancer `/debug` et demander une analyse de la
requête utilisée pour retrouver le commit. Examiner quelles informations du
projet et du skill ont été prises en compte, ce qui a conduit au diagnostic,
et quelles vérifications étaient nécessaires.

**À montrer :** `/debug` sert à comprendre le cheminement d’une requête et à
repérer les améliorations possibles, pas à tenir le diagnostic pour vrai sans
le confronter au dépôt.

## 4. Planifier puis réaliser une évolution

Passer du diagnostic à une évolution indépendante de l’incident : la feature
« La mascotte Orbit accompagne le diagnostic », décrite dans le
[`backlog`](backlog.md). Lancer `/plan` — ou sélectionner le mode « plan » — en
fournissant la description de la feature et ses trois tickets, puis ajouter
`/grill-me` si cette commande est disponible dans l’environnement.

Relire le plan avec le backlog : vérifier les critères d’acceptation, les
fichiers et tests à explorer, l’ordre des dépendances et les limites de la
feature. Faire préciser le plan avant de passer à l’implémentation s’il oublie
un critère ou suppose un nouvel état métier, une nouvelle API ou une
modification de la simulation.

Une fois le plan cohérent, le faire appliquer. Lancer l’application et
parcourir le tableau de bord, les états d’Orbit, le système mis en avant et la
navigation au clavier. Vérifier également le respect de la réduction des
animations et l’absence de ressources distantes. Utiliser les commandes de
validation indiquées dans le README et signaler explicitement tout contrôle
non exécuté.

**À montrer :** le plan transforme un besoin et ses contraintes en étapes
vérifiables ; l’implémentation reste guidée par les critères du backlog et les
tests, sans élargir le périmètre.

## Conclusion de la démo

Revenir sur le fil complet : contexte fiable, enquête traçable, analyse de
l’échange, puis plan et livraison d’une évolution. Terminer en relisant le diff
et en vérifiant que le résultat respecte le backlog ainsi que les limites
documentées.
