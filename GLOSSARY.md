# AstroMate

Vocabulaire métier de l’application de démonstration AstroMate, qui simule la surveillance d’une station spatiale unique.

## Station et systèmes

**Station**:
Environnement spatial unique représenté par AstroMate. Son état global résume l’état de ses systèmes.

**Système**:
Sous-ensemble fonctionnel de la station surveillé indépendamment, comme le support de vie, le contrôle thermique, l’alimentation électrique ou les communications.
_À éviter_: Module, lorsqu’il s’agit d’un système fonctionnel.

**Composant**:
Équipement faisant partie d’un système, par exemple un radiateur ou une pompe.

**Disponibilité**:
Indication explicite qu’un système simulé est en ligne ou hors ligne. Une absence de mesure ne suffit pas, à elle seule, à déclarer un système hors ligne.

## Mesures et états

**Métrique**:
Grandeur surveillée, définie par un nom et une unité, telle que la température ou le taux de CO₂.
_À éviter_: Indicateur, lorsqu’il s’agit de la définition de la grandeur plutôt que de sa présentation sur le tableau de bord.

**Mesure**:
Valeur numérique d’une métrique observée à un instant donné.
_À éviter_: Valeur courante, lorsqu’il s’agit d’un échantillon horodaté de l’historique.

**Plage nominale**:
Intervalle inclusif dans lequel une mesure est considérée nominale. Une valeur hors de cette plage déclenche au minimum un état d’avertissement, sauf si elle franchit un seuil critique.

**État**:
Qualification courante d’une métrique, d’un composant, d’un système ou de la station : nominal, avertissement, critique, inconnu ou, pour un système et la station, hors ligne.

**Donnée périmée**:
Mesure valide dont l’horodatage dépasse la durée de fraîcheur convenue. Une donnée périmée ne représente pas l’état courant du système.

## Alertes et historique

**Alerte**:
Enregistrement d’une anomalie suivie de son ouverture à sa résolution. Une alerte de seuil est ouverte lorsqu’une métrique franchit un seuil d’avertissement ou critique, puis résolue automatiquement lorsque la métrique revient dans sa plage nominale.
_À éviter_: Événement, pour désigner une anomalie suivie dans le temps.

**Événement technique**:
Fait horodaté consigné dans le journal, comme une transition d’état, une erreur de donnée, un démarrage ou une opération simulée.
_À éviter_: Alerte, lorsqu’il s’agit d’une information ponctuelle sans anomalie suivie.

**Simulation**:
Production locale de données fictives, sans connexion à une station, à des capteurs ou à un système opérationnel réel.
