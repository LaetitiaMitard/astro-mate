# Incidents

## Le rétablissement des données n’apparaît pas lorsque la température reste en alerte

### Description

Après une température invalide sur un radiateur, le journal signale correctement
un problème de qualité de données. Lorsque le radiateur transmet ensuite une
température physiquement valide mais encore en alerte, la nouvelle valeur
s’affiche et l’alerte de seuil apparaît, mais le journal ne signale pas le
rétablissement des données.

Le rétablissement n’apparaît que lorsque la température revient dans sa plage
nominale. L’application reste accessible et les autres écrans fonctionnent.

### Étapes de reproduction

Utiliser le jeu de télémétrie fourni par le formateur. Il transmet
successivement les valeurs suivantes pour le même radiateur, avec des
horodatages croissants et sans mesure intermédiaire :

1. Partir d’une température nominale de 22 °C.
2. Recevoir une température invalide de −300 °C et constater un événement DATA WARNING dans le journal.
3. Recevoir une température de 27 °C, physiquement valide mais de niveau WARNING.
4. Consulter la température du radiateur, les alertes et les événements DATA du journal.
5. Recevoir une température nominale de 22 °C et consulter à nouveau le journal.

Le même problème peut être recherché avec une température de retour de 31 °C,
de niveau CRITICAL, à la place de 27 °C.

### Comportement attendu

Dès la réception de 27 °C, la mesure doit être affichée et une alerte de seuil
WARNING doit être ouverte. Un événement DATA INFO doit également signaler le
rétablissement des données et la fin de l’épisode de valeurs invalides : une
température hors plage nominale n’est pas nécessairement une donnée physiquement
invalide.

Le même rétablissement est attendu pour une mesure valide de 31 °C,
indépendamment de l’alerte CRITICAL.

### Comportement observé

À 27 °C, la mesure et l’alerte de seuil sont correctement mises à jour, mais aucun
événement DATA INFO de rétablissement n’apparaît. Le problème de qualité de données
ne semble pas terminé dans le journal.

L’événement de rétablissement n’apparaît qu’à la réception de 22 °C.

### Impact

Le journal laisse croire que les données restent invalides alors que des
températures exploitables sont de nouveau reçues. Il devient difficile de
distinguer la fin du problème de qualité des données du retour à un fonctionnement
thermique nominal.
