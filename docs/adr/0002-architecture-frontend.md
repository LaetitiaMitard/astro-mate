# ADR 0002 — Organisation du frontend par fonctionnalité

- **Statut :** accepté
- **Date :** 4 novembre 2026

## Contexte

Le frontend React/TypeScript regroupe des responsabilités de navigation, de
chargement et de présentation dans son composant racine. Les écrans du tableau
de bord, des systèmes, des alertes et du journal constituent des parcours
métier distincts; la validation des réponses HTTP et les types de données sont
également des responsabilités identifiables.

La V1 reste une application locale de taille modeste. Une architecture en
couches très abstraite, un nouveau routeur ou un gestionnaire d’état global
ajouteraient des concepts et des dépendances sans besoin correspondant.

## Décision

Organiser le frontend selon une architecture **feature-first avec des couches
légères** :

- `app/` compose l’application, son shell, sa navigation et ses mécanismes
  transversaux de rafraîchissement.
- `features/` regroupe le code de chaque parcours métier : tableau de bord,
  systèmes, alertes et journal.
- `shared/api/` porte les modèles échangés avec l’API, leur validation runtime
  et les appels HTTP.
- `shared/domain/`, `shared/lib/` et `shared/` contiennent uniquement les
  règles et outils réellement communs aux fonctionnalités.

Les fonctionnalités peuvent dépendre de `shared/`, mais pas de `app/` ni
directement les unes des autres. `app/` orchestre les fonctionnalités. Un
élément qui n’est utilisé que par une fonctionnalité reste dans celle-ci.

La migration ne change ni les routes, les styles, les contrats API ou les
comportements existants, ni les dépendances frontend.

## Conséquences

- Les responsabilités métier et leurs tests sont regroupés par fonctionnalité.
- La frontière API est distincte des modèles de domaine et de présentation.
- Le shell conserve la responsabilité des routes et du rafraîchissement
  transversal; les écrans restent propriétaires de leur présentation et de
  leurs états locaux.
- L’ajout d’une bibliothèque de routage, d’état ou de composants nécessite un
  besoin explicite et une décision séparée.
