# Preuves jointes à une CAPA — conception

**Date** : 2026-08-07 · **Module** : CAPA (§4.2) · **Norme visée** : ISO 9001 §10.2

## Le problème

Une CAPA se clôt sur une vérification d'efficacité, et l'efficacité se prouve. Aujourd'hui
le dossier ne porte aucune pièce : l'auditeur doit croire sur parole que l'action a produit
son effet, ou aller chercher la preuve ailleurs — dans un partage réseau, une boîte mail.
Une CAPA sans preuve attachée n'est pas une CAPA close, c'est une CAPA qu'on affirme close.

## Décisions

| Question | Décision | Raison |
| --- | --- | --- |
| Niveau de rattachement | **Le dossier**, pas l'action | C'est le niveau où la preuve a valeur d'audit, et celui que désigne ISO 9001 §10.2. Le rattachement à l'action s'ajoutera sans rien casser si le besoin se confirme. |
| Nature de la preuve | **Fichier téléversé** | C'est ce qu'un auditeur attend qu'on lui tende. La référence à un document de la GED viendra en second temps. |
| Formats | PDF, images (jpeg/png/webp/heic), **docx/xlsx** | Arbitrage assumé : le classeur reste modifiable, donc une preuve bureautique peut diverger de celle produite devant l'auditeur. Le confort de saisie l'emporte ici. |
| Taille par pièce | 10 Mo | Déjà la limite multipart du serveur : aucune infrastructure à toucher. |
| Nombre par CAPA | 10 pièces | Un dossier de preuve se lit. Au-delà, l'auditeur ne trouve plus rien. |
| Poids total par CAPA | 25 Mo | Dix fois 10 Mo feraient 100 Mo par dossier, sur un disque partagé à 77 %. |
| Borne atteinte | **Refus explicite** (409) | Supprimer silencieusement une preuve dans un dossier d'audit serait indéfendable. |
| Après clôture | **Lecture seule**, comme les actions | Un dossier qu'on peut regarnir après coup ne prouve plus rien : la pièce a pu être fabriquée après le constat. |

## Architecture

Le module réutilise l'infrastructure de pièces jointes des non-conformités plutôt que d'en
inventer une seconde : `ObjectStorage` (S3-compatible), clé tenantisée, URL présignée à TTL
court, 503 franc quand le stockage est coupé.

```
CapaEvidenceController (multipart)
        │
CapaEvidenceService ──── bornes, validation, verrou d'état
        ├── CapaEvidenceRepository   (métadonnées : clé, type, taille, nom, auteur, date)
        └── ObjectStorage            (binaire : tenants/{tenant}/capa/{capa}/{uuid}.{ext})
```

**Ordre d'écriture** : la ligne de métadonnées d'abord, le binaire ensuite. Si le `put`
échoue, la transaction annule la ligne — aucune métadonnée orpheline. À la suppression,
l'ordre est inverse pour la même raison. C'est le comportement déjà retenu pour les photos.

**Sécurité** : le type MIME déclaré est falsifiable, donc la signature binaire est vérifiée
(`%PDF-` pour le PDF, `PK\x03\x04` pour docx/xlsx qui sont des ZIP, signatures d'image pour
le reste) et doit correspondre au type annoncé. L'extension de la clé vient du type validé,
jamais du nom de fichier client — qui peut porter une traversée de chemin. Le tenant vient
du jeton, jamais du corps de requête (§18.2 #2).

## Contrat HTTP

| Verbe | Route | Réponse |
| --- | --- | --- |
| POST | `/api/v1/capa/cases/{id}/evidences` (multipart `file`) | 201 |
| GET | `/api/v1/capa/cases/{id}/evidences` | 200, URL présignées |
| DELETE | `/api/v1/capa/cases/{id}/evidences/{evidenceId}` | 204 |

Refus : 400 type non admis ou signature incohérente · 409 borne atteinte ou dossier clos ·
413 pièce trop lourde · 503 stockage désactivé · 404 dossier ou pièce absents.

## Écran

Une section « Preuves » dans la fiche CAPA : liste des pièces (nom, poids, date, auteur),
bouton de dépôt, retrait unitaire. Le bouton disparaît sur un dossier clos et quand la
borne est atteinte, avec la raison affichée plutôt qu'un refus découvert au clic. Quand le
stockage est coupé, la section le dit au lieu d'afficher une erreur brute.

## Infrastructure

Le stockage objet est désactivé en préproduction (`qualitos.storage.s3.enabled=false`) : les
photos de NC y répondent déjà 503. Un MinIO tourne sur le cluster (namespace `openlab`).
L'activation — bucket, identifiants, valeurs Helm — fait partie de cette livraison ; sans
elle la fonctionnalité serait livrée muette. Elle ranime au passage les photos de NC.

## Tests

Service : bornes (taille, nombre, poids cumulé), signatures binaires, verrou d'état,
isolation par tenant, ordre d'écriture. Contrôleur : les six codes de refus. Front : dépôt,
liste, retrait, états stockage coupé / borne atteinte / dossier clos.
