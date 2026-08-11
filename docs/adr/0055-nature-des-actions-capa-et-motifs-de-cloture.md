# ADR 0055 — Nature des actions CAPA (endiguement) et motifs de clôture énoncés avant le clic

- **Statut** : Accepté
- **Date** : 2026-08-11
- **Owners** : @Couldevlop
- **Complète** : [ADR 0052](./0052-tableau-actions-capa.md) (tableau des actions)

## Contexte

Deux manques distincts se rejoignaient sur le même écran.

**1. Toutes les actions se ressemblaient.** Une action de CAPA portait un titre, un
statut, une échéance, un porteur — mais rien ne disait ce qu'elle *fait*. Or un
traitement réel commence presque toujours par des mesures prises le jour même pour
arrêter l'hémorragie : trier le lot suspect, arrêter la ligne, prévenir le client,
remettre en conformité l'existant. Ces mesures **contiennent l'effet** ; elles ne
touchent pas à la cause. C'est l'étape D3 de la méthode 8D, et c'est très exactement
la distinction que porte l'ISO 9001 §10.2 entre « corriger » et « éliminer les causes ».

Sans cette distinction, un dossier où l'on a seulement trié le lot se lit comme un
dossier où l'on a corrigé le réglage de la presse : les deux affichent « toutes les
actions faites ». Le second seul empêche la récidive. Un dossier pouvait donc se
clore, régulier en apparence, sur un problème intact.

**2. Le refus de clôture arrivait après le clic.** Le verrou introduit en août
(un dossier ne se clôt plus au-dessus d'un écart resté ouvert) faisait ce qu'il
fallait, mais l'utilisateur ne le découvrait qu'en cliquant sur « Efficace —
clôturer » : il recevait un **409 portant une phrase anglaise construite par le
service**, sur un écran qui parle six langues. Il devait ensuite instruire lui-même
ce qu'il lui restait à faire. Le même écran affichait un bouton qui promettait une
clôture qu'il ne pouvait pas tenir.

## Décision

1. **`capa_actions.action_type`** — énumération fermée `CONTAINMENT` / `CORRECTIVE` /
   `PREVENTIVE` (V106), non nulle, **défaut `CORRECTIVE`** à la création comme pour
   les lignes existantes. Le `DEFAULT` SQL est retiré après remplissage : la valeur
   par défaut vient du service, qui est testable.
2. **Un dossier dont TOUTES les actions sont des mesures d'endiguement ne peut pas
   être clôturé.** Le refus porte sur la vérification d'efficacité positive
   seulement ; déclarer les actions **inefficaces** reste possible à tout moment.
3. **`CaseResponse.closureBlockers`** — liste de `{code, count}` calculée sur la
   **fiche** (jamais sur la liste paginée). Quatre codes : `NO_ACTION`,
   `ACTIONS_NOT_DONE`, `CONTAINMENT_ONLY`, `OPEN_NON_CONFORMITIES`.
4. **Un code et un décompte, jamais une phrase.** La phrase se construit côté écran,
   dans la langue de l'utilisateur.
5. **La liste est vide, pas nulle, quand rien ne s'oppose** à la clôture ; elle est
   **nulle** sur la liste paginée, où elle n'est pas calculée. Un dossier clos ou
   rejeté renvoie une liste vide sans interroger la base.
6. **L'écran énonce les obstacles au-dessus du bloc d'efficacité** et **éteint** le
   bouton de clôture tant qu'il en reste un.

## Justification

**Pourquoi un type sur l'ACTION et non sur le dossier.** `capa_cases.type`
(CORRECTIVE / PREVENTIVE) dit pourquoi le dossier a été ouvert. Il ne dit rien de ce
que chaque action fait, et les deux ne se déduisent pas l'un de l'autre : un dossier
correctif porte typiquement un endiguement, deux correctives et une préventive. Mettre
l'information sur le dossier aurait forcé à choisir une nature pour l'ensemble, c'est-à-dire
à perdre celle qui compte.

**Pourquoi CORRECTIVE par défaut, y compris pour l'existant.** Ce n'est pas un choix
de commodité. Aucune mesure d'endiguement n'a jamais pu être enregistrée comme telle,
faute de colonne pour le dire : toutes les actions déjà saisies l'ont été comme des
actions correctives, et c'est exactement ce que le défaut inscrit. Conséquence utile :
**aucun dossier existant ne devient inclôturable** du fait de cet ADR.

**Pourquoi bloquer, et pas seulement signaler.** Un type qui ne porte aucune
conséquence est une décoration : il serait renseigné au hasard, puis plus du tout.
Surtout, l'enjeu est un registre qualité qui sert de preuve : y inscrire « problème
traité » quand seule la manifestation a été contenue est une affirmation fausse, et
c'est précisément ce qu'un auditeur cherche. Le refus reste étroit — une seule action
non-endiguement suffit à lever l'obstacle — et il ne s'applique jamais au constat
d'échec, qui doit pouvoir se consigner en toutes circonstances.

**Pourquoi un code et un décompte plutôt qu'un message.** Renvoyer un texte tout fait
l'aurait figé en français (ou en anglais, comme le 409 d'origine) dans une interface
qui parle six langues, et aurait obligé le navigateur à afficher une phrase serveur
qu'il ne peut ni traduire ni mettre en forme. Un code se traduit, se teste, se compte,
et se prête à d'autres usages (filtre, statistique) qu'une phrase interdit.

**Pourquoi tous les obstacles d'un coup.** N'en montrer qu'un enverrait l'utilisateur
le corriger pour en découvrir un autre — deux allers-retours là où un seul écran
suffit. La liste couvre donc aussi ce qui bloque la *résolution* qui précède la
clôture : ce qu'on veut savoir, c'est ce qui sépare de la fin, pas ce qui bloque
l'étape suivante prise isolément.

**Pourquoi seulement sur la fiche.** Comme `sourceNonConformity` (ADR 0052) : le
calcul coûte une requête par dossier, et vingt dossiers vaudraient vingt requêtes pour
une information que la liste n'affiche pas.

**Pourquoi vide ≠ nul.** Une liste vide **dit** « rien ne s'y oppose », ce qui est une
information : c'est elle qui allume le bouton. Un `null` signifie « non calculé ».
Les confondre obligerait l'écran à deviner, et à choisir entre un bouton allumé par
défaut (qui échouera) ou éteint par défaut (qui bloquera à tort).

**Pourquoi éteindre le bouton plutôt que le laisser échouer.** Un bouton promet une
action. Un bouton qui promet ce qu'il ne peut pas tenir vaut moins qu'un bouton éteint
dont la liste au-dessus dit pourquoi.

## Conséquences

- ✅ Un dossier ne peut plus se clore sur des mesures qui n'ont rien réglé, et le
  registre cesse d'affirmer le contraire de ce qui s'est passé.
- ✅ L'obstacle est connu **avant** le clic, dans la langue de l'utilisateur.
- ✅ Aucun dossier existant n'est bloqué rétroactivement (défaut `CORRECTIVE`).
- ✅ Le tableau des actions distingue enfin ce qui contient de ce qui corrige.
- ⚠ **Une requête de comptage de plus** sur chaque lecture de fiche (elle existait
  déjà à la clôture ; elle a désormais lieu à chaque affichage). Assumé : c'est le
  prix d'un écran qui sait ce qu'il montre. La liste paginée n'est pas touchée.
- ⚠ **Un test existant a dû être réécrit** : il vérifiait que déclarer une efficacité
  *non démontrée* n'interrogeait pas le dépôt des NC. Le même décompte sert maintenant
  à énoncer les obstacles ; le test surveille désormais le statut, qui est ce qu'il
  voulait réellement dire.
- ⚠ Le type se corrige en ligne : requalifier une action après coup est possible, et
  souhaitable (on s'aperçoit souvent en relisant qu'une action rangée en « corrective »
  n'a fait que contenir). Aucune trace spécifique n'est posée sur ce changement — le
  journal du dossier consigne la mise à jour de l'action, pas le détail du champ.

## Tests d'invariant

- `CapaServiceTest` — défaut CORRECTIVE ; PATCH sans type ne requalifie pas ;
  clôture refusée sur endiguement seul ; **acceptée dès qu'une action corrective ou
  préventive existe** ; efficacité négative jamais bloquée ; `isContainmentOnly`
  faux sur liste vide (un `allMatch` répondrait « oui » à une question qui ne se pose pas).
- `CapaServiceTest` (blocages) — les quatre codes, leurs décomptes, le cumul de
  plusieurs obstacles, la liste **vide** quand rien ne bloque, la liste **nulle** sur
  `findAll`, et l'absence de requête sur un dossier clos.
- `capa-detail.closure-blockers.spec.ts` — bandeau affiché/absent, bouton éteint/allumé,
  « Non efficace » toujours accessible, accords singulier/pluriel, code inconnu
  (serveur plus récent que le bundle) traité sans casse.
- Migration **V106** — contrainte `CHECK` sur le domaine : une valeur inventée par un
  import échouerait à l'écriture, pas des mois plus tard au chargement d'une fiche.

## Références

- CLAUDE.md §4.2 (CAPA), §15.1 (i18n), §22-12
- ISO 9001:2015 §10.2 (non-conformité et action corrective) — « éliminer les causes »
- Méthode 8D, étape D3 (actions de confinement provisoires)
- [ADR 0050](./0050-preuves-jointes-capa.md), [ADR 0052](./0052-tableau-actions-capa.md)
