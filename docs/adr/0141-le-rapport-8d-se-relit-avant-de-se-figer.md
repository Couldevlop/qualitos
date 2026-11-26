# 0141 — Le rapport 8D se relit avant de se figer

- **Statut** : accepté
- **Date** : 2026-11-26
- **Décideurs** : direction qualité, architecte principal
- **Concerne** : `nonconformity/eightd` (moteur qualité), écran du rapport 8D (SPA)
- **Complète** : ADR 0139 (le rapport montre ce que la plateforme sait)

## Contexte

Le PDF du rapport 8D n'existait qu'**après l'émission**. Et l'émission n'est pas un
geste anodin : elle fige le contenu dans un instantané, signe son empreinte
(Ed25519 + ML-DSA-65), l'ancre, et attribue un code de vérification public. Elle est
donc gardée en conséquence — l'écart doit être clos, et §18.2 #5 exige un second
facteur, comme pour l'approbation d'un plan de surveillance. Une fois émis, le rapport
ne se réémet jamais.

Il s'ensuivait une impasse que la recette a nommée : **il fallait sceller pour voir**.
Celui qui prépare le rapport ne pouvait ni le relire sur papier, ni le faire relire par
le pilote, ni l'envoyer en revue avant de l'arrêter. L'initiateur comme le validateur
restaient sans document, et le seul chemin vers un PDF passait par un acte
irréversible, opposable, exigeant un second facteur dont tous les comptes ne disposent
pas. La relecture — la seule chose qui aurait pu éviter d'émettre un rapport faux —
était précisément ce que la plateforme rendait impossible.

Un 8D se rédige à plusieurs et se relit avant de partir chez le client. La liste de
recette le demande en un mot : « Ajouter un bouton de *Save as PDF* ».

## Décision

Le rapport se télécharge **en l'état**, sans être émis, par une route ouverte en
lecture : `GET /api/v1/nc/{ncId}/8d/draft-pdf`.

Ce document **n'est pas** le rapport opposable, et il ne le laisse pas croire :

- il porte en tête, avant tout contenu, « BROUILLON — document de travail, non émis :
  ni signé, ni ancré » ;
- son pied d'intégrité est remplacé par une mention qui dit la même chose et renvoie à
  l'émission. Pas de QR code : il mènerait à un code de vérification qui n'existe pas ;
- il n'a ni empreinte annoncée, ni code de vérification — le contrat du port de rendu
  dit désormais qu'une URL de vérification nulle **signifie** brouillon.

**Rien n'est écrit en base.** Préparer un brouillon n'est pas un acte de pilotage, et
deux brouillons du même rapport peuvent légitimement différer : c'est leur raison
d'être. L'ancrage, la signature et le journal d'audit restent l'apanage de l'émission.

Une fois le rapport **émis**, cette route remet le document scellé. Il n'y a plus de
version de travail qui vaille : deux documents concurrents du même 8D — l'un signé,
l'autre non, le brouillon étant toujours le plus récent — sèmeraient le doute sur celui
qui fait foi.

## Conséquences

- L'émission reste gardée à l'identique : écart clos, rôle de pilotage, second facteur.
  Rien n'est assoupli ; une porte **à côté** est ouverte, et elle ne scelle rien.
- Le contrat déterministe du rendu est préservé : même instantané et même URL de
  vérification — nulle comprise — produisent les mêmes octets.
- Un brouillon peut circuler. C'est le but, et c'est pourquoi il se désigne lui-même
  comme tel sur chaque exemplaire imprimé : la mention est dans le document, pas dans
  le nom du fichier, qu'un destinataire peut renommer.
- L'écran efface le bouton « Enregistrer en PDF » dès que le rapport est émis, pour que
  l'interface ne propose jamais deux documents concurrents.

## Alternatives écartées

**Assouplir l'émission** (la permettre sans second facteur, ou sur un écart non clos) :
c'est la preuve opposable qu'on affaiblirait pour un besoin de relecture. §18.2 #5 ne
laisse pas le choix, et un 8D émis sur un écart ouvert affirmerait que le problème est
réglé alors qu'il court.

**Un aperçu à l'écran seulement** (impression du navigateur) : la mise en page du
document remis au client diffère alors de ce qu'on relit, et c'est justement la mise en
page qu'on veut vérifier. Le navigateur n'imprime pas les figures de cause de la même
façon.

**Marquer le brouillon par un filigrane en diagonale** : lisible, mais il traverse le
texte et gêne la relecture — or relire est tout l'objet de ce document. Une mention en
tête et un pied explicite se voient autant et n'abîment rien.
