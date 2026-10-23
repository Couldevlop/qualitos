# ADR 0096 — Les études de capabilité SPC (cartes X̄ – R, Cp / Cpk)

- **Statut** : Accepté
- **Date** : 2026-10-20
- **Owners** : @Couldevlop
- **Portée** : api-quality-engine (`spc.capability`, migration V145), section
  « SPC » du menu (`/spc/cartes`, `/spc/gage-rr`), écran `/admin/spc`
- **Complète** : ADR 0018 (règles de Nelson, analyse SPC des indicateurs),
  ADR 0078 (droits par client), ADR 0092 (une personne est un utilisateur de
  l'annuaire)

## Contexte

Le support `docs/spc/SPC.pptx` crée une section « SPC » dans le menu de gauche,
avec deux sous-menus : « Control charts » et « Gage R&R ». Le premier est
maquetté (`docs/spc/Études SPC – Registre et capabilité.html` pour le registre,
`docs/spc/Étude de capabilité procédé – Cp _ Cpk 4.html` pour la fiche) et
s'appuie sur le classeur `SPC Cp Cpk Study Template.xlsx`. Le second n'est pas
encore spécifié.

Les notes du support fixent les règles : seules USL et LSL se saisissent, d2,
A2, D3 et D4 viennent de la table ; le Cpk est vert au-delà de 1,67, jaune de
1,33 à 1,67, rouge en deçà ; un procédé dont le Cpk est sous 1,67 n'est pas
maîtrisé et ses formules se lisent en rouge ; dispersion = 6σ / (USL − LSL) ×
100 ; centrage = |X̿ − milieu| / (tolérance / 2). Le bouton « Recharger
l'exemple », l'indicateur « Sous-groupes complets » et la lecture « Hors
tolérance estimé » sont écartés.

## Décisions

### 1. Le serveur calcule, l'écran suit la saisie

`CapabilityCalculation` applique les formules du classeur : moyenne et étendue
de chaque sous-groupe complet, X̿ = Σ X̄ / m, R̄ = Σ R / m, σ = R̄ / d2, Cp =
(USL − LSL) / 6σ, CPU, CPL, Cpk = min (CPU, CPL), limites X̄ (X̿ ± A2 R̄) et R
(D4 R̄, D3 R̄). Seuls les sous-groupes **complets** comptent, et m est leur
nombre. Sans sous-groupe complet ou sans dispersion, les indices restent nuls et
l'étude est refusée à l'enregistrement.

L'écran refait le même calcul, avec la même table, pour suivre la frappe ; ce
qui est enregistré et affiché dans le registre vient toujours du serveur (Cp et
Cpk arrondis à trois décimales). Un test doré, le même côté serveur et côté
écran, vérifie le calcul sur la grille du classeur (Cp 1,840, Cpk 1,769).

### 2. Une table de constantes, n de 2 à 10

`SpcConstants` porte d2, A2, D3, D4 pour n = 2 à 10 : les valeurs du classeur
pour n = 2 à 5, les tables usuelles au-delà. La grille compte vingt
sous-groupes (« SG 01 » à « SG 20 ») et n lignes ; une cellule vide laisse son
sous-groupe incomplet.

### 3. Les seuils sont ceux du client, figés sur l'étude

Les seuils de maîtrise (1,67) et de surveillance (1,33) sont des valeurs par
défaut, réglées par l'administrateur du client dans « Paramètres SPC »
(`spc_settings`, `PUT /api/v1/spc/settings`, droit `authz.manage`). Une étude
recopie les seuils en vigueur à sa création et les garde
(`capable_threshold`, `watch_threshold`, non modifiables) : son verdict ne
change pas quand le réglage change. Le registre classe donc chaque étude avec
ses propres seuils : `CAPABLE` (maîtrisé, vert), `WATCH` (non maîtrisé, à
surveiller, jaune), `NOT_CAPABLE` (non maîtrisé, rouge).

### 4. Un droit `spc.manage`, et le module `dmaic`

Saisir, modifier, supprimer une étude et changer son plan d'actions demandent
l'action `spc.manage`, réglable dans la matrice des droits du client (ADR
0078). La lecture du registre et des fiches est ouverte à tout membre du
client. Les routes portent `@RequiresModule("dmaic")` : la SPC relève du
pilotage Six Sigma (§3.4). Le refus générique des `DELETE` est levé pour
`/api/v1/spc/studies/*`, pour que la décision reste au droit.

### 5. Le meneur est une personne de l'annuaire

Le meneur de l'étude se choisit dans l'annuaire (ADR 0092) : son compte et son
nom recopié. Il n'est jamais saisi en texte libre ni affiché par identifiant.

### 6. Tout est tracé, sans donnée personnelle

Création, révision, plan d'actions, suppression et réglage des seuils sont
journalisés (`spc.study.*`, `spc.settings.updated`) avec la taille du
sous-groupe, Cp, Cpk, le statut et le plan d'actions : ni mesure, ni nom.

### 7. La fiche suit la disposition du classeur

Les formules se lisent **juste à droite de la grille de saisie**, comme les
colonnes V à Y du classeur : la table des constantes (n, d2, A2, D3, D4, la
ligne de la taille choisie mise en évidence), puis Σ X̄, X̿, R̄, σ, 3σ, 6σ, Cp,
CPU, CPL, Cpk, les limites des cartes X̄ et R et le rappel « CP = / Cpk = ». Le
bouton « Masquer les formules » replie ce panneau ; la grille reprend alors
toute la largeur. Sous une largeur d'écran réduite, le panneau passe sous la
grille.

La carte X̄ trace aussi la USL et la LSL (lignes 44 à 53 du classeur), en plus
de UCL, CL et LCL. La carte R ne les trace pas : la tolérance ne s'applique pas
à une étendue.

Les deux pastilles « Procédé maîtrisé » et « Procédé non maîtrisé » (cellules
G39 à J40 du classeur) sont toujours affichées : neutres (« — ») tant qu'aucun
Cpk ne se calcule, puis OUI / NON selon le seuil de l'étude. La courbe de
distribution et de tolérance, sous les deux cartes, se trace d'elle-même dès
qu'un sous-groupe est complet.

La ligne n = 1 de la table du classeur (cartes de valeurs individuelles) n'est
pas reprise : une étude X̄ – R a des sous-groupes de 2 à 10 mesures.

### 8. Deux jeux d'exemple, chargés sans être enregistrés

La personne qui a le droit `spc.manage` peut charger deux jeux d'exemple
(`spc-capability.examples.ts`) : la grille du classeur (poids 500 ± 25 g,
n = 5, maîtrisé, Cp 1,84, Cpk 1,77) et un diamètre usiné décentré
(Ø 12 ± 0,1 mm, n = 4, non maîtrisé, Cp 1,42, Cpk 0,66, sous-groupe 14 hors des
limites de la carte X̄). L'exemple remplit l'identification, les limites, la
taille du sous-groupe et la grille. Il remplace des mesures déjà saisies
seulement après confirmation. Rien n'est enregistré tant que la personne
n'enregistre pas l'étude.

Ce bouton remplace « Recharger l'exemple », écarté au départ parce qu'il
écrasait la saisie sans prévenir.

### 9. Gage R&R : un sous-menu, une page vide

Le sous-menu « Gage R&R » est créé et mène à `/spc/gage-rr`, une page qui ne
porte que son titre. Son contenu (répétabilité et reproductibilité des moyens de
mesure) fera l'objet d'une décision quand il sera spécifié.

> Remplacé par l'ADR 0097 (les études Gage R&R) : la page porte désormais le
> registre et les fiches d'étude R&R.

## Conséquences

- La section « SPC » du menu regroupe « Cartes de contrôle » et « Gage R&R » ;
  l'analyse des règles de Nelson (`/spc`) reste dans « Analyses IA ».
- Une étude enregistrée garde le verdict qu'elle avait : abaisser le seuil ne
  rend pas maîtrisé un procédé jugé non maîtrisé hier.
- Le Cpk affiché pendant la saisie est celui qui sera enregistré, aux arrondis
  près.

## Vérification

- `CapabilityCalculationTest`, `spc-capability.calc.spec.ts` : test doré du
  classeur.
- `spc-capability.examples.spec.ts` : les deux jeux d'exemple et leurs verdicts.
- `capability-study.component.spec.ts` : panneau des formules, table des
  constantes, pastilles de statut, chargement d'un exemple avec ou sans
  confirmation.
- `CapabilityStudyTest`, `CapabilityThresholdsTest` : validations et seuils.
- `SpcStudyControllerTest` : droits, module, refus 422 par champ.
- `EtudesSpcOnPostgresTest` : migration V145 et RLS sur PostgreSQL.

## Références

- CLAUDE.md §3.4 (DMAIC, cartes de contrôle SPC), §6.6 (KPI définis), §18.2.
- `docs/spc/SPC.pptx`, maquettes et classeur de `docs/spc/`.
