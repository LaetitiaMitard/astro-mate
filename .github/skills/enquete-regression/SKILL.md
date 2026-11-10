---
name: enquete-regression
description: Rechercher l’origine d’une régression dans un dépôt Git à partir de preuves. Utiliser quand un comportement fonctionnait auparavant et ne fonctionne plus, ou quand un ticket d’incident demande le commit responsable.
---

# Enquêter sur une régression

Objectif : identifier le commit qui a introduit un comportement erroné et
justifier ce diagnostic par des preuves vérifiables. Ce skill sert à
diagnostiquer, pas à corriger : ne modifier ni le code ni les tests sans
demande explicite.

## Démarche

1. **Reformuler le symptôme.** À partir du ticket, écrire en une ou deux
   phrases le comportement attendu et le comportement observé, avec les
   données d’entrée exactes.
2. **Reproduire.** Chercher un test existant qui couvre le scénario. S’il n’y
   en a pas, proposer un test minimal qui échoue sur l’état actuel. Exécuter
   ce test avec les commandes documentées du dépôt.
3. **Localiser le code concerné.** Rechercher les règles métier liées au
   symptôme (noms de méthodes, messages du journal, constantes) et noter les
   fichiers candidats.
4. **Remonter l’historique.**
   - `git log --oneline -- <fichier>` pour lister les changements du fichier ;
   - `git log -S'<extrait>'` ou `git log -G'<regex>'` pour trouver le commit
     qui a ajouté ou retiré une condition ;
   - `git show <sha>` pour lire le diff complet ;
   - si le test de reproduction est fiable, `git bisect run <commande>` pour
     isoler le commit automatiquement.
5. **Comparer.** Vérifier qu’au commit parent le comportement est correct et
   qu’au commit suspect il ne l’est plus.

## Règles

- Le message d’un commit n’est pas une preuve : seul le diff compte. Un
  changement de comportement peut se cacher dans un commit au titre sans
  rapport.
- Séparer clairement les **faits vérifiés** (commande exécutée, résultat
  observé) des **hypothèses**.
- Ne pas affirmer qu’une commande ou un test a été exécuté s’il ne l’a pas été.
- Si l’historique est superficiel (clone `--depth`), le signaler et demander
  à récupérer plus d’historique avant de conclure.

## Rapport attendu

- Symptôme reformulé et scénario de reproduction.
- Commit suspect : SHA, titre, auteur, date.
- Extrait du diff en cause et explication du lien avec le symptôme.
- Vérifications effectuées et leur résultat.
- Niveau de confiance et points restant à vérifier.
