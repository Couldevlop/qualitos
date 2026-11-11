# Module Instruments — Calibration et parc d'équipements

[← Retour à l'index](../README.md) · Route : **`/calibration`** · Menu : *Opérations › Instruments*

## À quoi sert ce module

Un résultat de mesure ne vaut que ce que vaut l'instrument qui l'a produit. Ce module tient le
**parc des équipements de mesure et de contrôle**, leur **plan de calibration**, les
**enregistrements** de chaque opération et les **études MSA** (analyse du système de mesure)
exigées par les référentiels automobile et aéronautique.

Il répond aux trois questions d'un auditeur : quels instruments avez-vous, quand ont-ils été
calibrés pour la dernière fois, et lesquels sont en retard.

## Parcours pas à pas

### 1. Enregistrer un équipement

1. **Ouvrir** `/calibration`, puis **Nouvel équipement**.
2. Renseigner son **code**, son **nom**, et si vous les connaissez le fabricant, le modèle, le
   numéro de série et l'emplacement.
3. Cocher **Critique** si une dérive de cet instrument a des conséquences qualité directes : la
   liste le signale alors par une marque.
4. Un équipement peut être **rattaché à un capteur** du [parc IoT](iot.md) : la mesure remontée par
   le capteur et l'instrument de référence désignent alors le même objet.

Un équipement est **En service**, **Hors service** ou **Réformé**. La fiche permet de le mettre
hors service, de le remettre en service, de le réformer ou de le supprimer.

### 2. Définir son plan de calibration

Sur la fiche de l'équipement, **Définir un plan** :

- la **périodicité** en mois ;
- la **première échéance** — elle n'est utilisée que tant qu'aucune calibration n'a été
  enregistrée ; ensuite, ce sont les enregistrements qui pilotent la prochaine échéance ;
- la **procédure** de référence, la **tolérance** admise et la **référence d'accréditation** du
  laboratoire, si l'opération est sous-traitée.

### 3. Enregistrer une calibration

**Enregistrer une calibration** demande la date de réalisation, **qui l'a réalisée** (une personne
de votre organisation ou un organisme externe), le **résultat**, les **mesures** relevées et la
**référence du certificat**.

Le résultat est l'un des trois suivants :

| Résultat | Ce qu'il signifie |
| --- | --- |
| Conforme | l'instrument tient sa tolérance, il reste utilisable |
| Conforme sous condition | il est utilisable avec une réserve, à documenter dans les mesures |
| Non conforme | il ne tient pas sa tolérance |

La prochaine échéance est alors recalculée à partir de la date de réalisation et de la périodicité
du plan. Une **échéance forcée** permet de la fixer autrement — un retour de laboratoire qui
impose une date plus courte, par exemple.

### 4. Ajouter une étude MSA

**Ajouter une étude** enregistre une analyse du système de mesure, de l'un des quatre types de la
trame AIAG : **R&R (répétabilité / reproductibilité)**, **justesse (biais)**, **linéarité**,
**stabilité**. Vous saisissez la date, la **valeur** de l'étude, le **seuil** d'acceptation retenu,
le **résultat** — *Acceptable*, *Marginal*, *Inacceptable* — et un commentaire.

Pour conduire une étude Gage R&R complète (plan, opérateurs, calcul EV / AV / GRR / ndc et décision
AIAG), utilisez l'écran dédié : [Études Gage R&R](gage-rr.md).

## Comment lire le résultat

La liste des instruments se filtre par **statut** et par **horizon d'échéance**. Chaque ligne porte
sa prochaine calibration et son état d'échéance :

- **En retard** — l'échéance est dépassée. C'est le serveur, avec son horloge, qui en décide : la
  liste ne déduit jamais un retard de l'heure du poste de travail.
- **Échéance proche** — l'échéance tombe dans l'horizon que vous avez choisi.
- **Au-delà de l'horizon ou sans plan** — soit l'échéance est lointaine, soit aucun plan n'a été
  défini. Les deux cas méritent un coup d'œil, mais pas la même action.

Le compte des échéances dépassées porte sur **tout votre parc**, et pas seulement sur la page
affichée : un retard ne se cache pas en page 3.

La fiche d'un équipement affiche sa synthèse : dernière et prochaine calibration, dernier résultat,
nombre d'enregistrements conformes, conformes sous condition et non conformes, et nombre d'études
MSA acceptables et inacceptables.

## Bonnes pratiques

- **Marquez « critique » ce qui l'est vraiment.** Si tout est critique, plus rien ne l'est, et
  l'alerte de retard perd son pouvoir de déclencher une action.
- **Un instrument non conforme n'attend pas** : ouvrez une [non-conformité](non-conformites.md) et
  interrogez-vous sur les mesures déjà prises avec lui. C'est la question que posera l'auditeur.
- **Gardez la référence du certificat** : c'est elle qui raccroche votre enregistrement à la preuve
  émise par le laboratoire accrédité.
- **Reliez le plan à la périodicité réelle d'usage.** Un instrument utilisé en continu ne se
  calibre pas au même rythme qu'un instrument de secours.
- **Les études MSA précèdent les décisions SPC** : un [Cp / Cpk](spc.md) calculé avec un système de
  mesure non capable ne mesure que le bruit de l'instrument.
