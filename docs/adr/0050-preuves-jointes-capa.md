# ADR 0050 — Les preuves se joignent au dossier CAPA, pas à l'action

- **Statut** : Accepté
- **Date** : 2026-08-07
- **Owners** : @Couldevlop

## Contexte

Une CAPA (§4.2) se clôt sur une vérification d'efficacité, et l'efficacité se prouve
(ISO 9001 §10.2). Le dossier ne portait aucune pièce : l'auditeur devait croire sur
parole que l'action avait produit son effet, ou aller chercher la preuve ailleurs —
un partage réseau, une boîte mail. Une CAPA sans preuve attachée n'est pas une CAPA
close, c'est une CAPA qu'on affirme close.

L'infrastructure existait déjà pour les photos de non-conformité (§4.3) : stockage
objet S3-compatible, clé tenantisée, URL présignée à durée de vie courte, 503 franc
quand le stockage est coupé. La question n'était donc pas *comment stocker*, mais
*à quoi rattacher* et *jusqu'où laisser aller*.

## Décision

1. **Rattachement au dossier**, pas à l'action. C'est le niveau où la preuve a valeur
   d'audit, et celui que désigne ISO 9001 §10.2. Le rattachement à l'action s'ajoutera
   sans rien casser si le besoin se confirme ; l'inverse — désagréger après coup des
   pièces versées au dossier — obligerait à réattribuer à la main.
2. **Fichier téléversé**, pas référence à un document de la GED. C'est ce qu'un
   auditeur attend qu'on lui tende. La référence GED viendra en second temps.
3. **Formats admis** : PDF, images (jpeg/png/webp/heic), **docx/xlsx**. Arbitrage
   assumé : un classeur reste modifiable, donc une preuve bureautique peut diverger de
   celle produite devant l'auditeur. Le confort de saisie l'emporte ici, et le PDF
   reste disponible pour qui veut figer.
4. **Trois bornes, refusées explicitement** : 10 Mo par pièce (déjà la limite multipart
   du serveur — aucune infrastructure à toucher), 10 pièces par dossier, 25 Mo cumulés.
   Une borne atteinte donne un refus, jamais une suppression silencieuse de la pièce la
   plus ancienne : effacer d'office dans un dossier d'audit serait indéfendable.
5. **Lecture seule après clôture**, comme les actions. Un dossier qu'on peut regarnir
   après coup ne prouve plus rien : la pièce a pu être fabriquée après le constat.
6. **Signature binaire vérifiée** contre le type déclaré (`%PDF-`, `PK\x03\x04` pour les
   formats OOXML qui sont des ZIP, signatures d'image usuelles). Le content-type client
   est falsifiable. L'extension de la clé d'objet vient du type **validé**, jamais du
   nom de fichier — qui peut porter une traversée de chemin. Le tenant vient du jeton
   (§18.2 #2).
7. **Ordre d'écriture** : la ligne de métadonnées d'abord, le binaire ensuite. Si le
   `put` échoue, la transaction annule la ligne — aucune métadonnée orpheline. À la
   suppression, l'ordre est inverse pour la même raison. C'est déjà le comportement des
   photos de NC.

## Justification

Le rattachement au dossier suit la norme et l'usage : on présente « les preuves de la
CAPA », pas « les preuves de l'action 3 ». Les bornes ne protègent pas le disque en
premier lieu — elles protègent la lisibilité : un dossier de preuve se lit, et
au-delà d'une dizaine de pièces l'auditeur ne trouve plus rien. Le plafond cumulé
existe parce que dix pièces pleines feraient 100 Mo par dossier, sur un disque partagé
déjà occupé à 77 %.

## Alternatives écartées

- **Rattacher la preuve à l'action** : plus fin, mais désaligné de la norme et de la
  question posée en audit. Reste ajoutable par-dessus.
- **Référencer un document de la GED plutôt que téléverser** : élégant sur le papier,
  mais impose de publier d'abord dans la GED ce qui est souvent une photo prise devant
  la machine. Le geste doit rester d'un seul tenant.
- **Aucune borne, purge par tâche de fond** : une purge qui efface une pièce d'un
  dossier d'audit sans que personne ne le décide est indéfendable devant un auditeur.
- **N'admettre que le PDF** : plus rigoureux — un PDF ne se modifie pas discrètement —
  mais impose une conversion manuelle à chaque dépôt de relevé Excel. Le refus se
  paierait en preuves jamais versées.

## Conséquences

- ✅ Un dossier CAPA se justifie par ses pièces, consultables depuis la fiche, sans
  quitter la plateforme.
- ✅ Aucune infrastructure nouvelle : même stockage objet, même adaptateur, mêmes codes
  de refus que les photos de NC. Activer le stockage ranime les deux d'un coup.
- ⚠ **Le stockage objet doit être activé sur l'environnement** (`STORAGE_S3_ENABLED`).
  Tant qu'il ne l'est pas, toutes les routes répondent 503 et l'écran l'énonce — la
  section indique que le stockage est coupé au lieu d'afficher une erreur brute.
  L'activation en préproduction et en production reste à faire.
- ⚠ Un docx/xlsx versé en preuve peut diverger de la version montrée en audit : la
  signature binaire atteste du format, pas de l'immuabilité du contenu.
- ⚠ La suppression d'une preuve relève de la règle générique DELETE sur `/api/v1/**`
  (Manager Qualité et au-dessus) : un simple utilisateur dépose mais ne retire pas.

## Tests d'invariant

- `CapaEvidenceServiceTest` (22 cas) : les trois bornes, les signatures binaires par
  format, le verrou d'état sur dossier clos et rejeté, l'isolation par tenant,
  l'ordre d'écriture, la neutralisation du nom de fichier.
- `CapaEvidenceControllerTest` (13 cas) : les six codes de refus — 400, 404, 409, 413,
  503 — et le 201 nominal.
- Front (21 cas) : dépôt multipart sous le champ `file`, liste, retrait, et les trois
  états d'écran — stockage coupé, borne atteinte, dossier clos.

## Références

- CLAUDE.md §4.2 (CAPA), §4.3 (non-conformités), §18.2 #2 (tenant issu du jeton).
- ISO 9001:2015 §10.2 — non-conformité et action corrective.
- ADR 0021 — jeton de service (mêmes principes d'authentification côté engine).
