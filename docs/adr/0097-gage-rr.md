# ADR 0097 — Les études Gage R&R (répétabilité et reproductibilité)

- **Statut** : Accepté
- **Date** : 2026-10-23
- **Owners** : @Couldevlop
- **Portée** : api-quality-engine (`spc.gagerr`, migration V146), sous-menu
  « Gage R&R » de la section SPC (`/spc/gage-rr`, `/spc/gage-rr/nouvelle`,
  `/spc/gage-rr/:id`)
- **Complète** : ADR 0096 (études de capabilité SPC, §9 : la page Gage R&R
  restait vide), ADR 0078 (droits par client), ADR 0092 (une personne est un
  utilisateur de l'annuaire)

## Contexte

L'ADR 0096 a créé le sous-menu « Gage R&R » sans contenu. Le classeur
`docs/spc/Etude_RR_Gage_1.xlsx` le spécifie désormais : un guide, une grille
« Données » (k opérateurs × n pièces × r essais, avec la moyenne et l'étendue de
chaque opérateur et la moyenne de chaque pièce), un onglet « Résultats » qui
déroule la méthode **moyenne et étendue** de l'AIAG (MSA, 4e édition) en quatre
étapes, et une table des constantes K1, K2, K3 et D4.

Le guide annonce aussi la méthode **ANOVA** à deux facteurs avec interaction
pièce × opérateur, méthode de référence de l'AIAG, plus précise. Elle n'est pas
calculée dans le classeur.

Une étude R&R dit si un moyen de mesure est assez bon pour juger et piloter un
procédé : un %GRR trop élevé fausse les Cp et Cpk des études de capabilité.

## Décisions

### 1. Le serveur calcule les deux méthodes, l'écran suit la saisie

`GageRrCalculation` reprend les formules du classeur, cellule par cellule :

- étape 1 : X̄ et R̄ de chaque opérateur, X̄diff = max − min des X̄, R̄̄ =
  moyenne des R̄, Rp = max − min des moyennes pièces, UCL<sub>R</sub> = D4 × R̄̄ ;
- étape 2 : K1 (lu pour r), K2 (pour k), K3 (pour n), D4 (pour r) ;
- étape 3 : EV = R̄̄ × K1, AV = √[(X̄diff × K2)² − EV² / (n × r)] ramenée à 0
  si négative, GRR = √(EV² + AV²), PV = Rp × K3, TV = √(GRR² + PV²), chacune en
  % de la variation totale, en % de contribution (rapport des variances) et en
  % de la tolérance (USL − LSL) ;
- étape 4 : ndc = ⌊1,41 × PV / GRR⌋ (0 sans GRR), décision sur le %GRR
  (< 10 % acceptable, 10 à 30 % sous conditions, > 30 % non acceptable),
  discrimination suffisante si ndc ≥ 5, poste dominant (EV ou AV).

`GageRrAnova` ajoute l'ANOVA à deux facteurs croisés avec répétitions : sommes
des carrés (pièces, opérateurs, interaction, répétabilité, total), carrés
moyens, F des pièces et des opérateurs contre l'interaction, F de l'interaction
contre la répétabilité. Quand l'interaction n'est pas significative
(p > 0,25, seuil de l'AIAG), elle est regroupée avec la répétabilité et un
second tableau est calculé sur l'erreur regroupée. Les composantes de variance
en découlent (ramenées à 0 si négatives) ; EV, AV, GRR, PV et TV valent
multiplicateur × √variance, avec les mêmes pourcentages, le même ndc et la même
décision que la première méthode.

La loi F est calculée sans bibliothèque statistique : fonction bêta incomplète
régularisée par fraction continue (Lentz) et ln Γ de Lanczos, en Java comme en
TypeScript. Un reste d'interaction dû aux arrondis (≤ 10⁻¹² × SS total) est
ramené à 0, pour que la décision de regrouper ne dépende pas du flottant.

L'écran refait les deux calculs à la frappe ; ce qui est enregistré et affiché
dans le registre vient toujours du serveur (%GRR à deux décimales). Un test doré
commun aux deux côtés vérifie les nombres du classeur : EV 0,0414, AV 0,0271,
GRR 0,0495, PV 0,2304, TV 0,2357, %GRR 21,00 % (24,74 % de la tolérance),
ndc 6, sous conditions ; ANOVA : interaction regroupée (p 0,358), %GRR 21,16 %,
ndc 6.

### 2. Un plan borné par la table des constantes

La table AIAG ne couvre que k ∈ {2, 3}, r ∈ {2, 3} et n de 2 à 10 : le serveur
refuse tout autre plan (422 sur le champ en cause), la base le vérifie aussi
(`CHECK`). Le multiplicateur σ vaut 5,15 (99 % de la dispersion, valeur des
constantes) ou 6 ; à 6 σ, K1, K2 et K3 sont mises à l'échelle (× 6 / 5,15), D4
ne bouge pas. Le %GRR de la variation totale ne dépend pas du multiplicateur ;
le % de la tolérance, si.

La grille doit être complète et finie, et ne pas être faite de mesures toutes
identiques (TV > 0) : sinon l'étude est refusée. L'écran garde le texte saisi au
plus grand plan (3 × 10 × 3) : réduire le plan ne perd pas les mesures masquées.

### 3. Les personnes viennent de l'annuaire

Le responsable de l'étude et chaque opérateur se choisissent dans l'annuaire
(ADR 0092) : le compte et le nom recopié. Les opérateurs sont exactement k, tous
distincts ; jamais de texte libre ni d'identifiant affiché.

### 4. Même droit, même module que les études de capabilité

Les routes `/api/v1/spc/gage-rr` portent `@RequiresModule("dmaic")`. Lire le
registre et une fiche est ouvert à tout membre du client ; créer, réviser et
supprimer une étude demandent l'action `spc.manage`. Le refus générique des
`DELETE` est levé pour `/api/v1/spc/gage-rr/*`, pour que la décision reste au
droit. Le tenant vient toujours du jeton.

### 5. Une table, des mesures en JSON

`spc_gage_rr_studies` (V146) garde l'identification, les limites, le plan, le
multiplicateur, le responsable, les opérateurs et la grille
(`[opérateur][pièce][essai]`) en JSON, et les résultats recalculés à chaque
écriture (%GRR, %GRR de la tolérance, ndc, décision, %GRR et ndc de l'ANOVA)
pour le registre. La RLS s'applique comme à toute table portant `tenant_id`.

### 6. Tout est tracé, sans donnée personnelle

Création, révision et suppression sont journalisées (`spc.gage-rr.recorded`,
`.revised`, `.deleted`) avec le plan, le multiplicateur, les %GRR, le ndc et la
décision : ni mesure, ni nom.

### 7. La fiche suit le classeur

La grille reprend l'onglet « Données » : pour chaque opérateur ses essais, sa
moyenne et son étendue par pièce, puis la moyenne de la pièce ; la dernière
ligne donne les moyennes par essai, X̄ et R̄ de chaque opérateur et la moyenne
générale. Une étendue au-dessus de UCL<sub>R</sub> est marquée en rouge. Un
bloc copié depuis un tableur se colle d'un coup.

Les résultats déroulent les quatre étapes de l'onglet « Résultats » avec leur
formule et leur application numérique, puis l'interprétation (cellules A42 à
A46), le tableau d'ANOVA (complet, puis regroupé s'il y a lieu), les composantes
de la variance et la comparaison des deux méthodes. Quatre graphiques :
composantes de la variation (seuils de 10 % et 30 %), carte R par opérateur
(UCL<sub>R</sub>), carte X̄ par opérateur (X̿ ± A2 R̄̄ : un bon système a la
plupart des points hors limites), mesures par pièce. Le guide du classeur se
déplie au-dessus de la fiche.

Le bouton « Charger l'exemple » remplit la fiche avec le jeu du classeur (arbre
AX-210, 10,00 ± 0,10 mm, 3 × 10 × 3, 5,15 σ), sans les personnes, et remplace des
mesures déjà saisies seulement après confirmation. Rien n'est enregistré tant
que la personne n'enregistre pas l'étude.

## Conséquences

- Le sous-menu « Gage R&R » mène au registre des études R&R ; l'ADR 0096 §9 est
  remplacée.
- Le verdict affiché pendant la saisie est celui qui sera enregistré, aux
  arrondis près, pour les deux méthodes.
- Un plan hors table AIAG (4 opérateurs, 15 pièces…) n'est pas accepté : il
  demanderait d'autres constantes ou la seule ANOVA, à décider le moment venu.

## Vérification

- `GageRrCalculationTest`, `gage-rr.calc.spec.ts` : test doré du classeur
  (étapes 1 à 4, ANOVA, 6 σ, interaction forte non regroupée, étendue hors
  limite, AV ramenée à 0, sans dispersion).
- `FDistributionTest` : p-values de référence de la loi F.
- `GageRrStudyTest`, `GageRrConstantsTest` : validations champ par champ, plans
  admis.
- `GageRrStudyServiceTest`, `AuditLogGageRrPublisherTest` : tri, isolement par
  client, audit.
- `GageRrStudyControllerTest`, `GageRrDeleteSecurityTest` : droits, module,
  400 de validation, 422 par champ, 404 hors client.
- `GageRrRepositoryAdapterTest`, `EtudesGageRrOnPostgresTest` : stockage,
  migration V146 et contraintes sur PostgreSQL.
- `gage-rr-register.component.spec.ts`, `gage-rr-study.component.spec.ts` :
  registre, grille, calcul en direct, exemple, enregistrement, consultation
  seule.

## Références

- CLAUDE.md §3.4 (DMAIC, MSA), §6.6 (KPI définis), §18.2.
- AIAG, *Measurement Systems Analysis*, 4e édition.
- `docs/spc/Etude_RR_Gage_1.xlsx`.
