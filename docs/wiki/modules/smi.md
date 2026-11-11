# Module SMI — Tableau de bord du Système de Management Intégré

[← Retour à l'index](../README.md) · Routes : **`/smi`**, **`/smi/risques`**, **`/smi/exigences`** · Menu : *Pilotage › Tableau de bord SMI*

## À quoi sert ce module

Une organisation certifiée sur plusieurs référentiels (ISO 9001, ISO 14001, ISO 45001…)
pilote en réalité **un seul système de management**, structuré par les mêmes sept chapitres
communs. Le tableau de bord SMI donne cette vue unique : où en est la conformité de chaque
norme adoptée, quels risques majeurs sont ouverts, quelles actions sont en retard, quel
audit vient, et ce qu'il faut traiter dans les sept jours.

Ce module **ne stocke rien**. Il relit les autres : le [Standards Hub](standards-hub.md)
pour la conformité, le [registre des risques](risques-opportunites.md), les actions
[CAPA](capa.md), le planning des [audits](audits.md), les étalonnages
([instruments](calibration.md)) et les [changements](moc.md). Un chiffre affiché ici se
retrouve donc toujours dans le module qui le produit — et c'est là qu'on le corrige.

La lecture est ouverte à toute personne connectée. Les écritures restent celles des modules
d'origine : le SMI ouvre les écrans, il ne se substitue pas à eux.

## Parcours pas à pas

### Lire la vue consolidée

1. **Ouvrir** `/smi` (menu *Pilotage › Tableau de bord SMI*).
2. **Choisir le périmètre** avec la barre de normes en haut : *Tous*, ou une norme adoptée.
   Le filtre s'applique à la conformité, aux risques et au prochain audit. Les actions CAPA
   et les étalonnages, qui ne portent aucune norme, restent comptés en entier.
3. **Lire les quatre indicateurs** : conformité globale, actions en retard (dont critiques),
   risques majeurs ouverts, prochain audit. Chaque carte est un lien vers le module
   concerné.
4. **Parcourir les trois blocs** : conformité par référentiel, matrice des risques 5 × 5,
   et « À traiter cette semaine ».

### Ouvrir les risques d'une case de la matrice

1. Basculer la matrice entre **Brut** et **Résiduel**.
2. **Cliquer une case** (gravité × probabilité) : l'écran `/smi/risques` liste les risques
   ouverts de cette case.
3. **Affiner** par type, par processus ou par site.
4. **Sélectionner une ligne** pour voir son détail : cotation brute, résiduelle visée,
   origine, actions liées, prochaine revue.
5. Depuis ce détail, **Créer une action CAPA** (nature, responsable et échéance sont
   demandés) ou **Ouvrir la fiche risque** dans le registre.

### Lire la matrice des exigences

1. Depuis le bas du tableau de bord, suivre **« Matrice des exigences · chapitres communs ×
   normes »**, ou ouvrir `/smi/exigences`.
2. Le tableau croise les **sept chapitres communs** (4 à 10) avec chaque norme adoptée.
   Une colonne « Module QualitOS » rappelle, pour chaque chapitre, les modules qui en
   fabriquent les preuves.
3. **Cliquer une case** pour voir combien d'exigences sont prouvées, lesquelles restent à
   prouver, et ouvrir soit **Compléter les preuves** dans le Standards Hub, soit
   **Créer une action CAPA**.

### Ajouter une norme au système

1. Depuis `/smi` ou `/smi/exigences`, le bouton **« Ajouter une norme »**.
2. La fenêtre ne propose que des **normes de système de management certifiables** non
   encore adoptées ; celles que le catalogue rattache à une norme déjà pilotée sont
   présentées en tête.
3. Valider : la norme est adoptée par le Standards Hub, avec sa feuille de route de
   certification. Le tableau de bord se recharge sur *Tous*, et la matrice gagne sa colonne.

> Ce geste est **réservé à l'administrateur de l'organisation**. Les autres profils lisent
> « L'administrateur de votre organisation ajoute les normes. » — y compris le directeur et
> le manager qualité.

## Comment lire le résultat

Chaque indicateur a une définition unique, la même dans tout le produit :

| Indicateur | Définition |
|---|---|
| **Conformité** d'une norme | exigences prouvées / exigences, telle que le Standards Hub la calcule |
| **Conformité globale** | moyenne des normes affichées — ou la norme filtrée, si un filtre est posé |
| **Actions en retard** | actions CAPA non terminées, échéance dépassée, dans un dossier ni clos ni rejeté ; *critiques* = dossier de criticité critique |
| **Risques majeurs ouverts** | risques non clos (acceptés compris) dont le niveau brut atteint 10 |
| **Matrice des risques** | risques non clos, sur la cotation visée (brute ou résiduelle) |
| **À traiter cette semaine** | échéances d'ici sept jours, retards compris, huit lignes au plus |
| **Case des exigences** | *couvert* si toutes les exigences du chapitre ont une preuve, *partiel* si une partie, *écart* si aucune, *sans objet* si la norme n'a pas d'exigence sous ce chapitre |

Deux lectures à ne pas confondre :

- **Un risque sans cotation résiduelle visée n'apparaît pas** dans la grille résiduelle.
  L'écran l'annonce sous la matrice, avec le nombre de risques concernés : ce n'est pas une
  disparition, c'est une cotation qui manque.
- **Chaque norme est couverte par ses propres preuves.** Une preuve posée sur l'ISO 9001
  §5.2 ne compte pas pour l'ISO 14001 §5.2 : la matrice mesure norme par norme. La
  mutualisation des preuves entre référentiels est traitée séparément par la
  [co-couverture IMS](standards-ims-et-doc-gen.md), qui rapproche les clauses sans
  transférer les preuves.

## Bonnes pratiques

- **Commencez par la semaine**, pas par la conformité : le bloc « À traiter cette semaine »
  est le seul qui demande une action aujourd'hui.
- **Traitez les écarts par chapitre**, pas norme par norme : un chapitre en écart sur deux
  normes se comble souvent par une seule preuve bien placée.
- **Ne corrigez jamais un chiffre ici** : le SMI ne fait que relire. Une action en retard se
  solde dans la [CAPA](capa.md), un risque dans son [registre](risques-opportunites.md).
- **Fixez les cotations résiduelles** des risques que vous traitez : sans elles, la vue
  résiduelle sous-estime votre exposition réelle.
- **Une norme de plus se décide**, elle ne s'essaie pas : l'adoption crée une feuille de
  route de certification complète et se voit immédiatement dans la conformité globale.
