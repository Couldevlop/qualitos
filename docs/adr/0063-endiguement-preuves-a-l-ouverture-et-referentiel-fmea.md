# ADR 0063 — L'endiguement au niveau du dossier, les preuves dès l'ouverture, le barème FMEA à l'écran

- **Statut** : Accepté
- **Date** : 2026-08-30
- **Owners** : @Couldevlop
- **Portée** : CAPA (type de dossier, dialogue de création), NC (liste), FMEA (référentiel de cotation)

## Contexte

Un lot de retours terrain, six demandes, trois modules. Elles se ressemblent
plus qu'il n'y paraît : chacune corrige un écran qui **demande une information
sans donner de quoi la produire**, ou qui **répète une information sans
l'apporter**.

1. Un dossier CAPA ne pouvait être que correctif ou préventif. Le premier
   réflexe devant un écart n'est pourtant ni l'un ni l'autre : on bloque le lot,
   on arrête la ligne, on prévient le client. Ces dossiers partaient en
   « correctif », et un dossier qui n'avait fait que protéger se lisait comme un
   dossier où la cause avait été supprimée.
2. Les pièces justificatives ne pouvaient être jointes qu'après création. Or ce
   qui documente l'écart — la photo du défaut, le relevé, le courriel client —
   est sous la main **au moment de la déclaration**, pas dix minutes plus tard.
3. Le tableau des actions portait une colonne « Non-conformité » qui affichait
   la même valeur sur toutes les lignes.
4. Le filtre de statut des NC proposait `RESOLVED`, un état de passage qui
   renvoyait presque toujours une liste vide.
5. La liste des NC ne disait pas qui avait vu l'écart.
6. Le FMEA demandait de coter Sévérité, Occurrence et Détection de 1 à 10 sans
   jamais montrer l'échelle correspondante.

## Décisions

### 1. `CONTAINMENT` devient une raison d'ouverture de dossier, pas seulement une nature d'action

L'ADR 0055 avait introduit `CapaActionType.CONTAINMENT` : la nature de chaque
**action**. Il manquait le pendant au niveau du **dossier**.

Les deux ne se déduisent pas l'un de l'autre et ne se remplacent pas : le type
du dossier dit pourquoi il a été ouvert, la nature de l'action dit ce qu'elle
fait. Un dossier correctif porte presque toujours une ou deux mesures
d'endiguement ; un dossier d'endiguement, lui, se juge sur la rapidité et
l'étendue de la protection, jamais sur la disparition de la cause.

La migration V120 **remplace** la contrainte `chk_capa_cases_type` plutôt que de
l'assouplir : le domaine reste fermé côté base, une valeur inventée par un
script d'import échoue à l'écriture et non des mois plus tard au chargement JPA.

**Aucune donnée existante n'est requalifiée.** Requalifier après coup un dossier
qu'on n'a pas instruit serait inventer une intention.

### 2. Les pièces choisies avant la création partent après elle

Le serveur classe une preuve **sous** un dossier : il n'existe rien à quoi la
rattacher tant que le dossier n'a pas d'identifiant. Le dialogue retient donc les
fichiers dans une file locale et les dépose, un par un, après la création.

Conséquence assumée : **un dépôt refusé n'annule pas la création**. Détruire un
dossier parce qu'une pièce a été refusée perdrait la déclaration elle-même,
c'est-à-dire l'information la plus difficile à reproduire. Le message dit alors
ce qui est réellement joint, et la pièce se rejoint depuis la fiche.

Les limites (10 Mo, 10 pièces, formats) sont rejouées côté client **avant** la
création, pour qu'un refus prévisible n'arrive pas après coup.

### 3. Une information constante par dossier s'écrit une fois, dans l'en-tête

La colonne « Non-conformité » du tableau des actions affichait
`case.sourceNonConformity` — la même valeur sur chaque ligne. Elle occupait la
largeur d'une colonne pour répéter une constante, et repoussait hors écran les
colonnes qui, elles, varient d'une action à l'autre.

L'information **n'est pas perdue** : elle remonte dans l'encart « Source » de
l'en-tête du dossier, à côté du type de source et de sa référence.

### 4. Le filtre NC ne propose que des états où l'on trouve quelque chose

`RESOLVED` disparaît du sélecteur. Ce n'est pas une suppression d'état — le
statut existe toujours, la fiche l'affiche, la transition reste — c'est le retrait
d'un filtre qui promettait un résultat qu'il ne donnait presque jamais.

### 5. « Détecté par » : un nom recopié au signalement, lu dans le jeton

Le nom vient du JWT (`name`, à défaut `preferred_username`), **jamais du corps de
la requête** : un « détecté par » que l'appelant pourrait écrire lui-même
attribuerait un signalement à n'importe qui (OWASP A01, même invariant que
`reporterId`).

Il est **recopié**, pas joint par une clé étrangère : le nom doit rester lisible
le jour où le compte est renommé, désactivé ou supprimé de l'annuaire.

Les NC antérieures gardent une colonne vide et la liste affiche « — ».
Reconstituer un nom depuis l'annuaire d'aujourd'hui pour un signalement d'hier
produirait une attribution plausible et fausse.

### 6. Le barème FMEA est embarqué dans l'écran, en anglais, et ne se traduit pas

Coter S, O et D sans l'échelle sous les yeux, c'est produire des RPN qui ne se
comparent pas : le « 8 » de l'un n'est pas le « 8 » de l'autre.

Le référentiel (barèmes Sévérité / Occurrence / Détection, plus un exemple
complet de PFMEA) est donc consultable depuis la liste et la fiche FMEA, sans
quitter l'analyse en cours.

Deux choix méritent d'être écrits :

- **Constante du code, pas donnée de tenant.** *(Point REMPLACÉ par l'ADR 0064
  du 2026-08-31 : le barème devient redéfinissable par le tenant, servi par
  défaut et jamais copié, et chaque redéfinition s'inscrit au journal chaîné.
  L'objection ci-dessous reste valable — c'est le SILENCE qu'elle vise, et c'est
  lui que 0064 supprime.)* Ce ne sont pas les données d'une
  organisation, ce sont les règles de lecture des chiffres. Les stocker en base
  supposerait qu'un tenant puisse les modifier — et un barème modifié
  silencieusement invalide toutes les cotations passées.
- **Le contenu reste dans la langue du référentiel d'origine.** Une échelle de
  cotation traduite librement n'est plus la même échelle. Seule l'interface qui
  l'entoure est traduite.

Le fichier `fmea.reference.ts` est **généré** depuis `docs/QUALITOS BACKLOG.xlsx`
(feuilles 2 et 4) : une recopie à la main de 30 lignes chiffrées est une source
d'erreurs sans contrepartie.

## Conséquences

- ✅ Un dossier d'endiguement ne se lit plus comme un correctif.
- ✅ Ce qui documente l'écart est joint au moment où on l'a sous la main.
- ✅ Le tableau des actions gagne la largeur d'une colonne pour ce qui varie.
- ✅ Un signalement reste attribuable après le départ de son auteur.
- ✅ Les cotations FMEA d'un même tenant sortent du même barème.
- ⚠ La suggestion d'actions par l'IA a désormais trois branches ; une quatrième
  valeur de `CapaType` sans branche associée casserait la compilation (`switch`
  exhaustif sur l'énumération) — c'est voulu.
- ⚠ `reporter_name` est une copie : si l'état civil change dans l'annuaire, les
  anciens signalements gardent l'ancien nom. C'est le comportement attendu d'une
  piste d'audit, pas un défaut.

## Tests d'invariant

- `CapaContainmentAndNcReporterOnPostgresTest` (tag `migration`) rejoue **toutes**
  les migrations sur un vrai PostgreSQL, puis vérifie que `CONTAINMENT` est
  accepté, que `CORRECTIVE`/`PREVENTIVE` le restent, qu'une valeur inventée est
  refusée par `chk_capa_cases_type`, et que `reporter_name` accueille un nom
  comme une absence. H2 ne peut rien dire du remplacement d'une contrainte
  existante.
- `CurrentUserDisplayNameTest` — le nom vient du jeton, et reste vide plutôt que
  deviné (hors contexte, principal non-JWT, claims absents ou blancs).
- `NcReporterNameTest` — le nom du corps de requête est ignoré au profit du jeton.
- `CapaServiceTest.suggestActions_containment_…` — le prompt d'un dossier
  d'endiguement demande de protéger, pas de remonter à la cause.
- `capa-create-dialog.component.spec.ts` — rien ne part avant la création, les
  dépôts s'enchaînent un par un, un refus ne détruit pas le dossier, les limites
  mordent avant l'envoi.
- `capa-detail.actions-table.spec.ts` — la colonne a disparu **et** l'écart
  d'origine reste lisible dans l'en-tête.
- `nc-list.component.spec.ts` — `RESOLVED` absent du filtre, colonne « Détecté
  par » présente, « — » plutôt qu'un UUID.
- `fmea-reference-dialog.component.spec.ts` — les trois barèmes sont complets de
  10 à 1, le texte n'est pas reformulé, et chaque RPN de l'exemple est bien le
  produit de ses trois cotations.

## Références

- ADR 0052 — tableau des actions CAPA
- ADR 0055 — nature des actions CAPA et motifs de clôture
- ADR 0061 — preuve documentaire par étape PDCA
- CLAUDE.md §3.4 (DMAIC / Poka-Yoke), §4.2 (CAPA), §4.3 (NC), §4.5 (Risk & FMEA),
  §18.2 #2 (identité issue du JWT)
- ISO 9001 §10.2 ; méthode 8D, étape D3
