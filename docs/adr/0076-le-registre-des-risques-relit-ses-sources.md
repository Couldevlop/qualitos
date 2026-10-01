# ADR 0076 — Le registre des risques et opportunités relit ses sources

- **Statut** : Accepté
- **Date** : 2026-10-01
- **Owners** : @Couldevlop
- **Portée** : Module `riskregister` (api-quality-engine), écrans `/risques` (web),
  boutons « Créer un risque » des écrans AMDEC, NC, audit et changement
- **S'inscrit dans** : CLAUDE.md §4.5 (Risk Management), §3.6 (référentiel
  transverse), §18.2 (tenant et acteur issus du jeton), ISO 9001 §6.1 et §10.3,
  IATF 16949 §6.1.2

## Contexte

La maquette `docs/Registre des risques et opportunites.pptx` décrit un registre
unique du système de management : un onglet « Risques » (cotation brute
gravité × probabilité, cotation résiduelle, statut) et un onglet
« Opportunités » (gain attendu × faisabilité, échéance visée), un formulaire de
création, puis une **fiche** qui s'enrichit avec le temps : actions liées,
cotation résiduelle visée, historique des cotations, prochaine revue,
vérification d'efficacité. Une diapositive ajoutée précise **comment un risque
arrive dans le registre** : saisie directe, ligne d'AMDEC de l'APQP dont la
criticité dépasse un seuil, constat d'audit ou NC, analyse d'impact d'un
changement (MOC).

Le module `risk` existant porte l'AMDEC ; il n'a pas de registre au sens §6.1.

## Décisions

### 1. Deux tables, pas une table à colonnes optionnelles

`risk_register_risks` et `risk_register_opportunities` partagent la même
identification (type, processus, site, propriétaire, origine, exigences) mais
pas la même cotation. Une table unique aurait rendu facultatives des colonnes
obligatoires pour l'un des deux registres, et ses CHECK n'auraient plus rien
garanti. Le domaine partage l'`Identification` ; la base garde ses NOT NULL.

### 2. Le niveau se calcule au serveur

Faible 1–4, Moyen 5–9, Élevé 10–14, Critique (risque) ou Prioritaire
(opportunité) 15–25. Le serveur rend score et niveau ; l'écran n'en montre
qu'un aperçu pendant la saisie. La cotation résiduelle visée n'existe qu'en
modification, par ses deux notes ou pas du tout, et ne dépasse jamais la brute
(domaine **et** CHECK).

### 3. Un risque issu d'un objet relit cet objet, dans le client du jeton

Un bouton « Créer un risque » ouvre `/risques/nouveau?origine=…&source=<id>`.
Le formulaire demande un **brouillon** au serveur
(`GET /api/v1/risk-register/sources/{origine}/{id}`), qui relit l'objet dans
son module — ligne d'AMDEC, NC, constat d'audit (via son plan, le constat n'a
pas de tenant), changement — **dans le tenant du jeton**. À l'enregistrement,
la référence affichée (« PFMEA-7 #3 », « NC-2026-0042 ») est **reprise de
l'objet relu**, jamais du corps de la requête : un risque ne peut pas se dire
issu d'une AMDEC qui n'existe pas, ni d'un objet d'un autre client (404 dans
les deux cas, indiscernables). L'origine et la source sont ensuite figées.

La règle de seuil de la diapositive est tenue au serveur : une ligne d'AMDEC
n'est éligible que si son RPN atteint le seuil critique du projet ou si sa
priorité d'action est HAUTE ; un constat de conformité ne l'est pas. Les notes
AMDEC (1–10) deviennent des notes de registre (1–5) arrondies au-dessus.

Le brouillon expose le texte de l'objet source : il est réservé aux profils qui
peuvent créer le risque.

### 4. Un risque se traite par une vraie CAPA ; une opportunité par ses actions

« Créer une action CAPA » ouvre un dossier **préventif** par `CapaService`
(journal de cycle de vie compris), source `RISK`, référence du risque,
criticité du niveau brut. La fiche retrouve ses CAPA par ce couple : le lien
est porté par le dossier, sans table de liaison qui pourrait le contredire.
`chk_capa_cases_source` est réécrite sur l'énumération complète — elle ignorait
déjà `SPC_ALERT` et `ANOMALY`.

Une opportunité n'est pas un écart : ses actions (ACT-n, numérotées dans le
client) vivent dans `risk_register_actions`, pour ne pas fausser les
indicateurs CAPA (délai de clôture, récidive).

### 5. Pas de suppression ; un suivi lisible à côté du journal opposable

Un risque se clôt, une opportunité s'écarte : la fiche compte comme preuve
d'exigence. `risk_register_events` garde la chronologie lisible (codes, la
phrase se compose dans la langue de l'écran) ; le journal d'audit chaîné garde
la trace opposable, **sans aucun texte libre** (ni intitulé, ni propriétaire).
Les changements d'une même révision sont espacés d'une microseconde pour que
leur ordre ne dépende pas de la base.

### 6. Droits et module

Lecture : tout authentifié. Écriture : `QUALITY_MANAGER`, `DIRECTOR_QUALITY`,
`ADMIN_TENANT`, `SUPER_ADMIN`. Les écritures exigent le module `risk`
(`@RequiresModule`), comme l'AMDEC ; les lectures restent ouvertes après
résiliation.

## Conséquences

- Nouvelle section de navigation « Risques & opportunités » (module `risk`).
- La matrice de co-couverture du SMI pourra lire `requirements` des deux
  registres (projet ultérieur, annoncé par la maquette).
- Processus, site et propriétaire restent du texte, proposés depuis les valeurs
  déjà employées, faute de référentiel des processus et d'annuaire lisible par
  les non-administrateurs.
