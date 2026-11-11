# Module Indicateurs — Le catalogue des KPI

[← Retour à l'index](../README.md) · Route : **`/kpis`** · Menu : *Pilotage › Indicateurs (KPI)*

## À quoi sert ce module

Le catalogue des KPI est le **dictionnaire des indicateurs de votre organisation**. Un
indicateur n'y existe pas parce qu'un graphique en avait besoin : il y est **défini** —
code, nom, définition et formule, unité, sens d'optimisation, fréquence, cible, seuils,
propriétaire — puis **mesuré** période après période.

C'est une règle de produit, et l'écran vide le rappelle : *chaque KPI porte sa formule, ses
seuils, sa source et son propriétaire avant d'être affiché*. Un chiffre sans définition se
discute sans fin ; un chiffre défini se conteste sur ses faits.

Le catalogue alimente ensuite les autres écrans : les [tableaux de bord](dashboards.md), le
[tableau de bord SMI](smi.md), la [prévision KPI](forecast.md) et les
[cartes de contrôle SPC](spc.md), qui viennent tous y chercher des mesures.

## Parcours pas à pas

### Créer un indicateur

1. **Ouvrir** `/kpis` (menu *Pilotage › Indicateurs (KPI)*), puis **Nouveau KPI**.
2. **Code** — minuscules, chiffres, tiret, souligné. Il est unique dans votre organisation,
   et c'est lui qui identifie l'indicateur dans les autres écrans.
3. **Nom**, **catégorie** et **unité** (%, j, ppm…).
4. **Définition / formule** — le texte qui dit *exactement* comment la valeur se calcule.
   C'est le champ qui rend l'indicateur opposable ; une formule écrite ici tranche les
   discussions ultérieures sur « ce que mesure vraiment ce chiffre ».
5. **Sens d'optimisation** — *plus c'est haut, mieux c'est* (OEE, taux de conformité) ou
   *plus c'est bas, mieux c'est* (DPMO, rebut, délai). C'est lui qui décide comment les
   seuils se lisent.
6. **Fréquence** — temps réel, quotidienne, hebdomadaire, mensuelle, trimestrielle,
   annuelle, ou à la demande.
7. **Cible**, **seuil d'alerte** et **seuil critique**.
8. **Secteurs applicables**, si l'indicateur ne concerne qu'une partie de vos activités.

Un indicateur naît en **brouillon**. Il faut l'**activer** pour qu'il entre dans les
tableaux de bord et les analyses.

### Enregistrer une mesure

1. Ouvrir la fiche de l'indicateur, puis **Mesurer**.
2. Renseigner le **début** et la **fin de période**, la **valeur**, l'**unité**, la
   **source** et, au besoin, des **notes**.
3. La mesure rejoint la liste et la courbe de tendance.

La **source** d'une mesure dit d'où vient le chiffre : saisie manuelle, calcul, import, ou
agrégation issue de capteurs. C'est cette mention qui permet, un an plus tard, de savoir si
une valeur a été relevée à la main ou produite par un flux.

### Piloter le cycle de vie

La fiche porte les actions d'état : **Activer**, **Rouvrir** (retour en brouillon),
**Archiver**, **Supprimer**, et **Modifier** la définition. Archiver plutôt que supprimer
garde l'historique des mesures lisible.

### Exploiter les mesures

- **Tendance** : la fiche trace la courbe dès qu'il y a assez de points, et l'annonce quand
  il n'y en a pas assez.
- **Prévision** : `/forecast` propose les indicateurs **actifs** du catalogue comme source de
  mesures, et projette la série vers un horizon avec son intervalle à 95 %.
- **Carte de contrôle** : `/spc` reprend les dernières mesures d'un indicateur pour y
  chercher les signaux des règles de Nelson, et peut ouvrir une CAPA si le procédé est
  hors-contrôle.

## Comment lire le résultat

La **santé** d'une mesure se déduit de la valeur, de la cible et des deux seuils, dans le
sens d'optimisation déclaré :

| Santé | Signification |
|---|---|
| **OK** | la cible est tenue |
| **Alerte** | la valeur est entre le seuil d'alerte et la cible |
| **Critique** | la valeur a franchi le seuil critique |
| **Inconnue** | aucun seuil défini, ou aucune mesure |

Deux lectures à garder en tête :

- **Un indicateur sans cible ni seuils ne peut pas être « au vert »** : il reste en santé
  *inconnue*, et c'est honnête. Ce n'est pas un défaut d'affichage, c'est une définition
  incomplète.
- **Le sens d'optimisation change tout.** Une même valeur de 4 est bonne pour un taux de
  satisfaction et mauvaise pour un délai : c'est le sens déclaré qui le dit, pas le lecteur.

## Bonnes pratiques

- **Écrivez la formule en toutes lettres**, avec son périmètre et sa période de référence.
  « Taux de NC » ne veut rien dire ; « NC internes clôturées / lots produits, par mois, site
  compris » se vérifie.
- **Un propriétaire par indicateur** : sans responsable nommé, personne ne corrige une
  mesure manquante.
- **Dix à quinze indicateurs stratégiques suffisent.** Le catalogue peut en porter beaucoup
  plus, mais une vue de direction qui en montre quarante ne sert plus à décider.
- **N'activez un indicateur que lorsque sa mesure est réellement collectable.** Un
  indicateur actif sans mesure crée un trou visible dans tous les tableaux de bord.
- **Archivez, ne supprimez pas** : supprimer un indicateur emporte son histoire, et la
  comparaison d'une année sur l'autre devient impossible.
- **Déclarez la source de chaque mesure** : c'est le seul moyen de distinguer plus tard une
  saisie d'un flux automatique, et donc de savoir ce qu'un écart met en cause.
