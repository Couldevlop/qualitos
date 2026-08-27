# ADR 0062 — Toute empreinte se calcule sur un horodatage que la base rend à l'identique

- **Statut** : Accepté
- **Date** : 2026-08-27
- **Owners** : @Couldevlop
- **Portée** : journal d'audit chaîné (§11.5), scellement des control plans (§3.4), et toute empreinte à venir

## Contexte

Le journal d'audit se présente comme un **registre inaltérable** : chaque
événement porte une empreinte SHA-256 de son contenu, chaînée à celle du
précédent, et l'écran d'administration propose de recalculer la chaîne pour
prouver qu'aucune ligne n'a bougé.

En préproduction, ce contrôle répondait **« Chaîne rompue »** sur la totalité du
registre — les 19 événements, le premier compris — avec le motif
`Integrity hash mismatch (tamper)`.

Aucune ligne n'avait été touchée. Deux détails le disaient déjà : une altération
réelle casse la chaîne **à partir d'un point**, jamais depuis l'origine ; et
aucun `Previous hash mismatch` n'était signalé, donc le chaînage entre
événements était parfaitement cohérent. Ce n'était pas la donnée qui avait
bougé, c'était le **recalcul** qui ne retombait pas.

### La cause

L'empreinte est calculée sur l'objet **en mémoire**, avant l'écriture, puis
recalculée à la vérification sur la ligne **relue**. Elle ne peut donc porter que
des valeurs qui traversent la base sans changer d'un caractère.

`Instant` porte la nanoseconde. `TIMESTAMP WITH TIME ZONE` n'en garde que la
microseconde — et il **arrondit** : `.123456789` ressort en `.123457`. L'instant
haché à l'écriture n'était donc pas celui relu, et l'empreinte devenait
irrécupérable. Sur *tous* les événements, systématiquement.

Le défaut a survécu à 46 bancs d'essai du paquet `auditlog` parce qu'ils tournent
sur des **dépôts simulés** qui rendent l'objet d'origine : une doublure ne peut
pas voir un champ que la base abîme.

### Le même défaut, ailleurs, en silence

Le scellement des control plans (ADR 0058) portait la même faille. L'empreinte
scellée inclut `approvedAt`, calculée avant `repo.save()`. Le commentaire du code
promettait : « rejouer le calcul sur le document rendu par l'API suffit à
démontrer qu'il est bien celui qui a été signé ». Cette promesse était **fausse**.

La différence avec le journal est instructive : le journal *criait* son défaut,
faute de quoi il serait passé inaperçu ; le scellement, lui, n'a pas d'écran de
vérification — il aurait échoué le jour d'un audit, devant l'auditeur.

## Décision

**Tout horodatage qui entre dans le calcul d'une empreinte est ramené à la
microseconde avant ce calcul**, par `StorableInstant.micros(...)`.

### Pourquoi tronquer et non arrondir

Un instant tronqué à la microseconde n'a plus rien à arrondir : la base le rend
tel quel. Arrondir côté application fonctionnerait aussi, mais ferait dépendre
l'écriture d'une règle qui appartient au moteur de base de données — et qui
changerait avec lui.

### Pourquoi une classe partagée plutôt que deux corrections locales

Parce que ce n'est pas un bug, c'est une **classe de bug** : elle frappe partout
où une empreinte rencontre un horodatage, et elle est invisible aux tests qui
utilisent des doublures. La règle est donc écrite une fois, avec son pourquoi, à
un endroit que l'on trouve en cherchant « empreinte » ou « horodatage ».

### Ce que les bancs doivent désormais prouver

Un banc qui rejoue le calcul sur l'objet en mémoire ne prouve rien. Deux niveaux
de vérification ont été ajoutés :

- `AuditChainSurvivesPostgresTest` — un **vrai PostgreSQL**, migrations rejouées,
  écriture puis relecture, comparaison des empreintes. Il constate au passage
  l'arrondi de la base, pour que le fait soit écrit noir sur blanc quelque part.
- `ControlPlanServiceTest#theSealSurvivesTheRoundTripThroughTheDatabase` — le
  geste de l'auditeur : approuver avec une horloge à la nanoseconde, puis
  recalculer l'empreinte sur le plan **tel que la base le rendra**.

Les deux échouent sans le correctif. C'est la seule chose qui les rend utiles.

## Conséquences

### Les événements écrits avant ce correctif restent invérifiables

Leur empreinte a été calculée sur une valeur que plus personne ne possède : les
nanosecondes sont perdues, elles ne se devinent pas. Aucun correctif ne peut les
rendre recalculables.

**Nous ne réécrivons pas leurs empreintes.** Recalculer et réécrire les
empreintes d'un registre en écriture seule est exactement le geste que ce
registre existe pour rendre impossible ; le faire une fois, fût-ce pour une bonne
raison, retire toute valeur à la mécanique. Un journal qu'on répare n'est plus un
journal.

Conséquence assumée : sur un environnement dont le journal contient des
événements antérieurs au correctif, la vérification continuera de signaler ces
lignes-là. La coupure est datée et connue ; c'est une information exacte, pas une
avarie.

En **préproduction**, dont le journal ne contient que des données de
démonstration, la purge du registre est la solution propre — elle repart d'une
chaîne vérifiable. En **production**, la question se tranche avec le responsable
qualité : purger un journal d'audit est une décision d'exploitation, pas une
décision technique.

### Les control plans scellés avant ce correctif

Même situation : leur empreinte n'est pas rejouable. Ils restent approuvés et
lisibles ; c'est la démonstration d'intégrité qui manque. Une révision du plan
produit un nouveau scellement, correct celui-là.

## Alternatives écartées

- **Exclure l'horodatage de l'empreinte.** Ce serait ouvrir un trou : la date
  d'un événement fait partie de ce que l'on prouve.
- **Stocker l'horodatage en texte.** On garderait la nanoseconde, au prix de
  tris et de filtres de dates devenus faux ou coûteux, sur une colonne indexée.
- **Rendre le vérificateur tolérant.** Impossible : les nanosecondes perdues ne
  se retrouvent pas. Et un vérificateur qui tolère est un vérificateur qui ne
  vérifie plus.
