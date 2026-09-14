# APQP — projets, cycle en V et dossier PPAP

[← Retour à l'index](../README.md) · Routes : **`/apqp`** (liste des projets),
**`/apqp/:projetId`** (le cycle), **`/apqp/:projetId/ppap`** (le dossier PPAP)

## À quoi sert ce module

L'**APQP** (*Advanced Product Quality Planning*) est la méthode par laquelle on prépare un
produit avant qu'il n'entre en production : ce qu'il doit faire, comment on le fabriquera,
comment on saura qu'il est conforme, et ce qu'on remet au client pour le prouver.

QualitOS en tient **le suivi**, pas la théorie : pour chaque programme, la liste des
livrables attendus, qui en est responsable, pour quand, où l'on en est, et la pièce qui le
prouve. Le **dossier PPAP** — ce qu'on remet au client — se remplit tout seul à partir de
cette liste.

## Un projet par programme

Vous ne menez pas un seul APQP : vous en menez un **par programme**. Une introduction de
produit pour un client, un transfert d'outillage pour un autre — et chacun a son propre
dossier PPAP à remettre.

`/apqp` affiche donc la **liste de vos projets**, avec pour chacun son avancement et l'état
de son dossier. On clique sur une ligne pour entrer dans le projet.

Un projet porte un **type**, et c'est sur lui qu'on filtre la liste :

| Type | Ce qu'il ouvre |
| --- | --- |
| **NPI** | *New Product Introduction* — un produit nouveau entre en production |
| **ToW** | *Transfer of Work* — un produit existant change de site ou de ligne |
| **Nouveau client** | Un produit que vous connaissez, pour un client qui ne vous connaissait pas |
| **Autre** | Requalification, relance, évolution majeure |

À sa création, un projet reçoit d'emblée **les cinq phases du référentiel et leurs 48
livrables**. Vous les adaptez ensuite : renommer, retirer, ajouter. Ce sont vos données.

## Le cycle en V

Dans un projet, les phases se lisent **en V** : on descend de la planification vers la
conception du processus — le point bas du programme —, puis on remonte vers la production
série. Cliquer sur un jalon ouvre la phase et ses livrables, sans quitter l'écran.

Vous pouvez déplacer une phase d'un rang, en ajouter une sixième, en retirer une. Le V se
redessine.

> **Réinitialiser depuis le référentiel** remplace le cycle du projet par celui d'origine.
> C'est **définitif** : vos phases, vos livrables et les pièces qui les prouvent sont
> perdus. L'écran vous le demande deux fois.

## Un livrable, un seul formulaire

Tous les livrables se remplissent **de la même façon**, et tous se cochent. Ouvrir un
livrable donne :

| Champ | Ce qu'on y met |
| --- | --- |
| **Livrable acquis** (la case) | Ce qui pilote tout le reste — voir ci-dessous |
| **Artefact attendu** | Sous quelle forme le livrable est attendu : « Plan de surveillance de pré-lancement (entrées, spécification, méthode, taille et fréquence d'échantillon, plan de réaction) ». C'est ce texte qu'un auditeur confronte à la pièce que vous versez. |
| **Requis au dossier PPAP** | Si ce livrable compose le dossier remis au client. Pré-coché sur les douze livrables que le référentiel signale, **mais c'est vous qui décidez** : les exigences varient d'un client à l'autre. |
| **Responsable** | Un nom. Il peut être extérieur à l'organisation. |
| **Échéance** | Pour quand. |
| **Statut** | Non démarré · En cours · **Bloqué** · Terminé. « Bloqué » n'est pas un avancement : il dit que la cause est ailleurs — attente client, fournisseur, décision. |
| **Avancement** | De 0 à 100 %. |
| **Notes** | Tout ce qui ne rentre pas ailleurs : « reçu par courriel le 3 septembre, version 2 ». |
| **Pièces jointes** | Word, Excel, PDF ou image — 10 Mo par fichier, 5 par livrable. |
| **Renvoi** (facultatif) | Si le livrable est déjà tenu ailleurs dans QualitOS — une AMDEC, un plan de surveillance, un cycle PDCA, une CAPA —, désignez l'enregistrement et le bouton **Ouvrir la fiche** y mène. |

### La case pilote

C'est la règle à retenir : **la case, le statut et l'avancement disent la même chose.**

- **Cocher** met le statut à « Terminé » et l'avancement à 100 %.
- **Décocher** les ramène en arrière.

Vous ne pouvez donc pas vous retrouver avec « Terminé à 40 % », ni avec « Non démarré » sous
une case cochée. C'est voulu : ces deux états ne se lisent pas.

Qui a coché et quand sont **enregistrés par la plateforme**, pas saisis. C'est ce que
l'auditeur demande en premier. Décocher les efface — garder la trace d'un achèvement retiré
la rendrait fausse.

### Le renvoi ne peut pas être mort

Si vous désignez un enregistrement, QualitOS **vérifie qu'il existe** chez vous. Un lien qui
ne mène nulle part est pire qu'une absence de lien : il affirme qu'une preuve existe.

Et le renvoi va **par deux** : le module ET l'identifiant, ou ni l'un ni l'autre.

## Le dossier PPAP

`/apqp/:projetId/ppap` liste les **livrables requis** au dossier — ceux que vous avez
marqués « requis au dossier PPAP » —, avec la barre de complétude et, sur chaque ligne, si
la pièce est versée.

Ce n'est **pas une seconde liste** à tenir d'accord avec le cycle : c'est une vue du cycle
lui-même. Ce que vous cochez dans le V se voit ici aussitôt, et réciproquement.

« Acquis » et « prouvé » ne sont pas la même chose : on coche une déclaration, on verse une
pièce. Les deux se lisent séparément sur chaque ligne.

## Qui peut quoi

| | Lire | Écrire |
| --- | --- | --- |
| Utilisateur, Auditeur | ✅ la liste, les cycles, les dossiers | ❌ |
| Manager Qualité, Directeur Qualité, Admin Tenant, Super Admin | ✅ | ✅ ouvrir un projet, refondre un cycle, cocher un livrable, verser une pièce |

Lire est ouvert à tous : on ne peut pas demander à quelqu'un de livrer une AMDEC processus
en lui cachant à quel moment elle est due. Écrire ne l'est pas : supprimer une phase emporte
ses livrables, supprimer un projet emporte tout son cycle et ses preuves.

## Langue

Les phases, les livrables et leurs artefacts viennent du référentiel : ils **suivent la
langue de l'interface**, dans les six langues servies. Dès que vous reformulez un texte,
c'est le vôtre qui s'affiche — dans toutes les langues. On ne traduit pas ce que vous avez
écrit.

## Pour aller plus loin

- [AMDEC produit / PFMEA et plans de surveillance](produit-pfmea-control-plan.md) — ce vers
  quoi renvoient plusieurs livrables du cycle
- [PDCA](pdca.md) et [CAPA](capa.md) — les autres modules qu'un livrable peut désigner
- [Standards Hub](standards-hub.md) — IATF 16949, dont l'APQP et le PPAP sont des exigences
