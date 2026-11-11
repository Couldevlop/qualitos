# Module Fournisseurs — Référentiel, audits, certificats et NC fournisseurs

[← Retour à l'index](../README.md) · Routes : **`/suppliers`** et **`/suppliers/non-conformites`** ·
Menu : *Non-conformité › Fournisseurs* et *Non-conformité › NC fournisseurs*

## À quoi sert ce module

Le module tient le **registre de vos fournisseurs** et tout ce qui atteste de leur qualité :
leurs **audits**, leurs **certificats**, et les **non-conformités** que vous avez constatées sur
ce qu'ils vous livrent. Chaque fournisseur porte un **score qualité de 0 à 100** que la
plateforme recalcule seule à chaque événement — vous n'avez jamais à le saisir.

Les deux écrans se répondent :

- **`/suppliers`** — le registre : un fournisseur par ligne, son score, son dernier audit.
- **`/suppliers/non-conformites`** — toutes les NC fournisseurs, tous fournisseurs confondus,
  filtrables ; c'est la vue du travail quotidien.

Ces non-conformités sont **distinctes du registre interne** ([non-conformités](non-conformites.md),
route `/nc`) : elles portent sur une pièce reçue, un lot, et se traitent avec le fournisseur.

## Parcours pas à pas

### Référencer un fournisseur

1. **Ouvrir** `/suppliers`, puis **Nouveau fournisseur**.
2. Renseigner son **code** (unique — un code déjà pris est refusé), son nom, son pays et son
   **type** : matière première, composant, service, sous-traitance de fabrication, logiciel,
   logistique, autre.
3. Le fournisseur naît **prospect** : en cours d'évaluation.

### Faire vivre son statut

Le statut se change depuis la fiche du fournisseur, et la plateforme n'autorise que les
passages qui ont un sens :

| Statut actuel | Ce que vous pouvez en faire |
| --- | --- |
| **Prospect** | approuver, ou mettre en liste noire |
| **Approuvé** | passer sous surveillance, suspendre, mettre en liste noire |
| **Sous surveillance** | réapprouver, suspendre, mettre en liste noire |
| **Suspendu** | réapprouver, remettre sous surveillance, mettre en liste noire |
| **Liste noire** | rien — c'est terminal |

La **liste noire est sans retour**, et c'est voulu : réintégrer un fournisseur exclu passe par
un nouveau référencement, donc un nouveau code. Une fiche en liste noire ne se modifie plus.

### Enregistrer un audit fournisseur

Depuis la fiche, **Nouvel audit** : sa date, son score, le nombre de constats **critiques**,
**majeurs** et **mineurs**, et une synthèse. L'audit entre dans l'historique du fournisseur et
pèse sur son score : **au-delà de 18 mois sans audit**, l'absence d'audit récent dégrade la note.

### Suivre les certificats

**Nouveau certificat** : la norme visée, sa référence, ses dates d'émission et d'expiration —
une date d'expiration qui ne serait pas postérieure à l'émission est refusée. Un **certificat
expiré** dégrade le score ; la fiche compte les certificats expirés en évidence.

### Déclarer et traiter une NC fournisseur

1. Depuis `/suppliers/non-conformites` (**Nouvelle NC**) ou depuis la fiche du fournisseur.
2. Décrire l'écart : **pièce**, **quantité**, **référence de lot**, date de détection,
   **sévérité** (mineure, majeure, critique).
3. Suivre son **statut** : ouverte → en revue → résolue, ou rejetée.
4. Deux cases disent où en est le fournisseur : **8D demandé** et **8D reçu**.
5. **Fiche PDF** : la non-conformité s'imprime à la demande — c'est le document que vous envoyez
   au fournisseur avec la demande de 8D, et celui que vous montrez à l'auditeur. Elle est
   recalculée à chaque demande, jamais stockée, donc toujours à jour de ses pièces jointes.

### Joindre les preuves

Chaque NC et chaque certificat accepte des pièces jointes, rangées par **nature** :
**8D fournisseur** (la pièce attendue en premier sur une NC), **preuve de CAPA**,
**certificat**, **autre**. Les limites sont annoncées avant l'envoi : **10 Mo par fichier**,
**20 fichiers** et **50 Mo** au total par NC ou par certificat.

Une NC **résolue ou rejetée** fige ses documents : l'écran le dit plutôt que d'échouer en
silence. Pour en joindre une de plus, rouvrez-la.

## Comment lire le score qualité

Le score part de **100** et se retranche, de façon entièrement déterministe :

| Ce qui pèse | Effet sur le score |
| --- | --- |
| NC critique non résolue | −15 par NC |
| NC majeure non résolue | −7 par NC |
| NC mineure non résolue | −2 par NC |
| NC résolue dans les 12 derniers mois | −1 par NC (la cicatrice récente) |
| Certificat expiré | −8 par certificat |
| Aucun audit, ou dernier audit de plus de 18 mois | −5 |

Le résultat est borné à l'intervalle 0–100 et **se recalcule à chaque modification** d'une NC,
d'un audit ou d'un certificat. Deux conséquences utiles : le score est explicable ligne par
ligne, et résoudre une NC se voit immédiatement.

## Bonnes pratiques

- **Le statut suit le score, pas l'inverse.** Un fournisseur dont le score chute doit passer
  *sous surveillance* explicitement : c'est cette décision écrite, pas le chiffre, qui engage.
- **Demandez le 8D, puis tracez sa réception.** Les deux cases séparées existent pour que
  « nous l'avons demandé » ne se confonde jamais avec « nous l'avons reçu ».
- **Le 8D du fournisseur est un document reçu**, pas le rapport 8D que vous rédigez sur vos
  propres écarts : celui-là se construit dans la plateforme, voir
  [Rapport 8D](rapport-8d.md).
- **Auditez avant les 18 mois.** Le score vous le rappellera, mais il le fera après.
- **Joignez les preuves à la NC, pas au courriel.** La fiche PDF les liste : l'auditeur
  trouve le 8D et les preuves de CAPA sans vous solliciter.

## Limite connue

La **prédiction de risque fournisseur** est calculée et servie par l'API
(`GET /api/v1/suppliers/{id}/risk-prediction`, modèle pondéré explicable hébergé par le
service d'analyse), mais **aucun écran ne l'affiche encore**. Le score qualité, lui, est bien
visible dans le registre et dans la fiche.
