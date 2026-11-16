# ADR 0124 — Le PDF du 8D se présente comme l'écran

- **Statut** : accepté
- **Date** : 2026-11-16
- **Portée** : rapport 8D (instantané scellé, assembleur, rendu PDF, écran)
- **Prolonge** : [ADR 0122](0122-un-refus-se-dit-avant-le-clic-et-une-cause-se-trace.md)
  (les figures dans le PDF), [ADR 0123](0123-un-ecart-a-plusieurs-dossiers-capa-tout-ce-qui-le-concerne-les-montre-tous.md)
  (un écart a plusieurs dossiers CAPA)

## Contexte

L'écran du rapport 8D présente les actions décidées **en colonnes** : nature,
intitulé, porteur, échéance, avancement, preuve. Le document émis, lui, les
énumérait **en phrases** :

```
Action [CORRECTIVE] Remplacer le joint du presse-étoupe — DONE — Grace Hopper — due 22/09/2026
```

Ce sont les colonnes d'un tableau aplaties dans une ligne de texte. L'information
y est tout entière, et pourtant elle ne sert plus : on ne compare plus rien d'une
action à l'autre, alors que c'est exactement ce qu'on vient faire dans ces
disciplines — voir d'un coup d'œil ce qui reste ouvert, ce qui est en retard, ce
qui n'est pas prouvé. Le PDF est par ailleurs la pièce qui part chez le client et
que l'auditeur lit : c'est là que la lecture en revue compte le plus.

L'ADR 0122 avait déjà tranché le même écart pour les schémas de cause. Celui-ci
étend la décision à sa dernière forme.

## Décision

**Le document émis se présente comme l'écran.**

### 1. Un tableau est une DONNÉE dans l'instantané scellé, jamais une image

`EightDSnapshot.Table(caption, columns, rows)` et `EightDSnapshot.Row(cells)`,
pour la même raison que les figures : le rendu reste une **fonction pure** de ce
qu'on stocke, donc l'empreinte scellée reste vérifiable. Une image de tableau
ferait dépendre cette empreinte d'un moteur de rendu.

`List<Table> tables` est **tolérant au `null`** : un rapport émis avant cette
décision se relit exactement comme il a été scellé, sans tableau, et son
empreinte continue de tomber juste.

### 2. Une ligne qui n'a pas le compte de cellules est REFUSÉE

Rendue telle quelle, elle décalerait silencieusement les valeurs d'une colonne :
le porteur passerait sous « échéance », l'avancement sous « preuve ». Le document
serait signé, ancré — et faux. Rien dans sa chaîne de confiance ne le dirait,
puisque l'empreinte couvrirait fidèlement une donnée déjà fausse. Un document
qu'on ne peut pas rendre vaut mieux qu'un document qui ment avec un sceau.

### 3. Chaque discipline ne montre que les actions de SA nature

Les mêmes que l'écran (`NATURES_PAR_DISCIPLINE`, côté web) : le **D3** endigue,
le **D6** prévient, le **D5** nomme les trois. Mélanger les natures ferait lire,
sous « actions immédiates d'endiguement », une action préventive à échéance dans
six mois.

Un dossier sans action de cette nature **n'écrit pas de tableau vide** : un
en-tête de colonnes sous un titre de dossier se lit comme un dossier qu'on aurait
ouvert sans rien y mettre.

### 4. Un tableau par dossier, portant son titre

L'ADR 0123 a établi qu'un écart porte plusieurs dossiers CAPA. Le texte les
séparait par une ligne vide — il fallait que le lecteur remarque un blanc. Le
tableau résout cela **par construction**.

### 5. Le tableau remplace l'énumération au D5, la complète au D3 et au D6

Comme à l'écran, et pour la raison que donne l'ADR 0122 : au D5 on lisait deux
fois le même fait, dont une fois inerte. Au D3 et au D6 le texte dit autre chose
que le tableau — ce qu'on a fait pour arrêter l'hémorragie, ce qui a été mené et
prouvé — et les deux restent.

Conséquence sur le **libellé de source du D3** : il ne dit plus « rempli par
l'équipe qualité » quand seul le tableau est servi. Le texte vient de l'équipe,
les actions du dossier CAPA ; les confondre attribuerait à quelqu'un une phrase
qu'il n'a pas écrite, dans un document scellé.

### 6. Une ligne ne se coupe pas, et l'en-tête se répète

Même règle que pour les figures (ADR 0122). Une cellule coupée en deux pages ne
se lit plus ; pire, une colonne de valeurs qui reprend **sans son intitulé** se
lit **de travers** — on prend une échéance pour une date de clôture.

Les largeurs de colonnes sont calées sur les valeurs les plus **longues** qui
existent, pas sur les plus courantes : `CONTAINMENT` et `IN_PROGRESS` sont des
valeurs d'énumération, elles reviennent telles quelles, et trop serrées elles
venaient toucher la colonne suivante — deux valeurs qui se touchent se lisent
comme une seule. Défaut vu sur un **aperçu rendu**, pas à la lecture du code.

## Conséquences

- Le PDF et l'écran montrent la même chose, sous la même forme.
- Les rapports déjà émis sont **inchangés, octet pour octet** (banc dédié).
- L'écran ne porte plus de note affirmant que le PDF n'a que du texte — elle
  était vraie le jour où elle a été écrite et dissuadait d'ouvrir le document.
- ⚠ Les colonnes et les natures sont déclarées **des deux côtés** (TypeScript et
  Java). Un ajout de nature d'action demande de toucher les deux. Les lier
  demanderait de générer l'un depuis l'autre ; dette assumée, consignée ici.

## Vérification

- `EightDSnapshotTest` : une ligne qui n'a pas le compte de cellules est refusée ;
  un tableau sans colonne aussi ; une section relue sans tableau se lit vide.
- `EightDSnapshotAssemblerTest` (28) : le D5 tabule au lieu d'énumérer ; le D3
  n'affiche que l'endiguement et le D6 que la prévention ; **tous** les dossiers
  de l'écart ont leur tableau, chacun nommé ; une discipline sans action de sa
  nature n'écrit pas de tableau vide ; le D3 n'attribue pas à l'équipe qualité un
  tableau qu'elle n'a pas écrit ; le D6 montre dans son tableau une action encore
  en cours que son récit tait — les taire laisserait croire la prévention finie.
- `PdfBoxEightDRenderAdapterTest` (23) : chaque cellule de chaque ligne sort dans
  le texte du PDF ; un tableau qui traverse une page **répète son en-tête** et ne
  perd aucune des 40 lignes ; deux rendus donnent les mêmes octets ; et **un
  rapport scellé avant les tableaux rend les mêmes octets qu'avant**.
- `EightDReportOnPostgresTest` : l'instantané tabulé fait l'aller-retour en base.
- Aperçu rendu et **regardé** page à page avant de conclure.
