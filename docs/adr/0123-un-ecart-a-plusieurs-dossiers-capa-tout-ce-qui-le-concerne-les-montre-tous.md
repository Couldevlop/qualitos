# ADR 0123 — Un écart a plusieurs dossiers CAPA : tout ce qui le concerne les montre tous

- **Statut** : Accepté
- **Date** : 2026-11-16
- **Owners** : @Couldevlop
- **Portée** : `apps/api-quality-engine` (`quality/capa` : `CapaService`,
  `CapaDto.CaseRef` (nouveau) ; `quality/nonconformity` : `NcService`,
  `NcDto.Response`/`NcDto.CapaCaseRef` (nouveau) ;
  `quality/nonconformity/eightd` : `EightDSources`, `EightDSourceAdapter`,
  `EightDSnapshotAssembler`) et `apps/web` (`features/nc` : fiche d'écart ;
  `features/capa` : fiche de dossier). Aucun changement de schéma.
- **Prolonge** : **ADR 0121** (un dossier CAPA qui cite un écart est le dossier
  de cet écart) — qui a fait entrer le second dossier dans les invariants de
  clôture ; celui-ci le fait entrer dans ce qu'on **voit**. **ADR 0122** (un
  refus se dit avant le clic) — dont le décompte était le symptôme visible.
- **S'inscrit dans** : CLAUDE.md §4.2 (CAPA), §4.3 (NC), §3.6 (référentiel
  transverse partagé), §15.1 (sobriété)

## Contexte

Trois symptômes, une seule cause et une question mal posée.

**Le décompte ne correspondait pas au tableau.** La fiche d'un écart annonçait
« 2 dossiers CAPA portent encore des actions à mener » (ADR 0122) au-dessus d'un
tableau d'actions qui n'en montrait **qu'un**. Les actions du second n'existaient
nulle part à l'écran — ni dans la fiche, ni dans le rapport 8D, dont le D5 ne
listait que les actions du premier dossier.

La cause est celle que l'ADR 0121 a laissée ouverte : un écart peut porter
**plusieurs** dossiers. Le premier est désigné par `non_conformities.capa_case_id`
(l'escalade) ; un second, ouvert depuis le registre des CAPA en citant la
référence de l'écart, ne remplace pas ce lien — c'est délibéré, le premier reste
le sien (ADR 0088). Les **invariants** avaient été corrigés : la résolution et
la clôture interrogent déjà tous les dossiers qui citent la référence. Les
**lectures** ne l'avaient pas été : la fiche et le 8D suivaient le lien unique.

Un invariant qui compte plus que ce que l'écran montre est pire qu'une absence
de garde : l'utilisateur lit un refus qu'il ne peut pas instruire.

**Et une étape disparaissait sans un mot.** Sur un dossier résolu, ajouter une
action le ramène en traitement — c'est juste, il reste du travail. Mais le bloc
« Vérifier l'efficacité », visible l'instant d'avant, s'effaçait alors de
l'écran. Rien ne disait qu'il était seulement **reporté** ; on concluait à une
fonction perdue, ou à une donnée abîmée par l'ajout.

## Décision

### Ce qui NOMME applique la portée ; ce qui COMPTE ne l'applique pas

C'est la distinction qui organise tout le reste.

`CapaService.casesOf(escaladeId, ncReference)` rend des `CaseRef`
(identifiant, titre, statut) pour le lien **et** la référence, sur les deux
natures de source, dédupliqués — et **filtrés par la portée** (ADR 0081) :
rendre ici le titre d'un dossier hors de portée le divulguerait par un chemin
détourné, la fiche d'un écart, alors que sa propre fiche le refuse.

`casesFor` / `unsettledCasesFor`, qui servent les invariants, ne filtrent
**pas**. Un refus de clôture qui dépendrait de qui regarde ne serait plus un
invariant : l'écart se clôturerait pour l'un et pas pour l'autre. C'est
exactement pourquoi un motif de blocage porte un **décompte** et jamais un titre
(ADR 0122) — la décision de l'ADR précédente trouve ici sa raison.

### La fiche et le 8D lisent la liste, pas le lien

`NcDto.Response` porte `capaCases` ; `capaCaseId` reste, pour ne pas casser ce
qui s'y appuie et comme **repli** : un serveur qui ne sert pas encore la liste ne
doit pas faire disparaître le tableau que l'écran affichait déjà.

La fiche d'écart affiche **un tableau d'actions par dossier**, chacun titré, et
n'annonce le compte que s'il y a de quoi compter : « 1 dossier » sur le cas
ordinaire serait du bruit, c'est la pluralité qui est inattendue.

Côté 8D, `EightDSources` porte `List<Capa>`. Le D5 énumère les actions **de tous
les dossiers**, un bloc par dossier séparé par une ligne vide, et le titre d'un
dossier ne s'écrit que s'il a du contenu. Le « tout est soldé » du D6 exige que
**tous** soient clos.

**L'instantané 8D ne filtre pas par portée, et c'est délibéré.** Le droit de
lire ce rapport se décide sur l'**écart**, en amont (`NcScope`) : un écart hors
de portée est introuvable et rien n'est collecté. Filtrer ensuite les dossiers
lecteur par lecteur donnerait **deux rapports différents** pour le même écart :
l'instantané est signé et son empreinte ancrée, un contenu qui dépend de qui le
demande ne se vérifie plus. Un 8D est le document de l'écart, pas la vue
personnelle de celui qui l'imprime.

### Une étape reportée se dit

La fiche d'un dossier annonce le report de la vérification d'efficacité, avec le
**nombre d'actions restantes** — qui est la condition du retour. Elle ne le dit
que si une vérification est effectivement attendue (un critère posé, ou la
décision explicite de vérifier) : annoncer une étape dont personne n'a décidé
serait inventer une exigence.

## Alternatives écartées

- **Faire remplacer le lien par le second dossier.** Ce serait défaire l'ADR 0088
  (le premier ouvert est le sien) et déplacer l'arbitraire, pas le supprimer.
- **Ne compter, dans les invariants, que les dossiers que le lecteur voit.** La
  clôture dépendrait de qui regarde ; deux utilisateurs liraient deux vérités sur
  le même écart.
- **Nommer les dossiers dans le motif de blocage.** Un titre divulgue ; un
  décompte n'a rien à cacher. C'est le motif même du choix de l'ADR 0122.
- **Filtrer l'instantané 8D par portée.** Un document scellé dont le contenu
  dépend du lecteur n'est plus vérifiable.
- **Garder un seul tableau, en fusionnant les actions des deux dossiers.** On
  perdrait la seule information qui explique le décompte : quel dossier porte
  quoi.
- **Laisser la vérification d'efficacité disparaître.** C'est ce qui se passait,
  et c'est ce qu'on a pris pour une panne.

## Conséquences

- ✅ Le décompte annoncé et le contenu montré concordent.
- ✅ Le D5 du 8D envoyé au client porte toutes les actions décidées.
- ✅ Les titres de dossiers hors de portée ne fuient pas par la fiche d'un écart.
- ✅ Une étape repoussée s'annonce au lieu de s'effacer.
- ⚠ **La jointure se fait sur `source_ref`, un texte libre** — la référence de
  l'écart. C'est l'héritage de l'ADR 0121, pas une nouveauté ici : un dossier qui
  cite une référence mal saisie ne sera pas rattaché, et deux écarts de clients
  différents portant la même référence restent séparés par le `tenant_id` mais
  pas par une contrainte. Dette consignée ; la corriger demande une colonne
  d'identifiant et une migration des références existantes.
- ⚠ Une fiche d'écart portant plusieurs dossiers émet un appel de liste
  d'actions par dossier. Acceptable au regard du nombre réel (deux, rarement
  plus) et de l'alternative, qui serait de masquer la moitié du travail.

## Tests d'invariant

- `CapaServiceTest` : `casesOf` **ne nomme pas** un dossier hors de portée ;
  `unsettledCasesFor` **le compte quand même** ; le dossier de l'escalade n'est
  compté qu'une fois.
- `NcServiceTest` : la fiche porte tous les dossiers de l'écart, dédupliqués.
- `EightDSourceAdapterTest` : lien et référence, sur les deux natures de source,
  sans doublon.
- `EightDSnapshotAssemblerTest` : le D5 porte les actions des deux dossiers ; un
  dossier sans contenu ne laisse pas de titre orphelin ; le D6 n'est soldé que
  si tous les dossiers sont clos.
- `nc-detail.dossiers.spec.ts` : un tableau par dossier ; le compte ne s'affiche
  qu'au pluriel ; sans liste servie, le dossier du lien reste affiché.
- `capa-detail.effectiveness-pending.spec.ts` : le report se dit avec son
  décompte, et se taît quand la vérification est possible, déjà jugée, ou jamais
  attendue.

## Références

- CLAUDE.md §3.6, §4.2, §4.3, §15.1
- ADR 0081 (portée d'enregistrement), ADR 0088 (le premier dossier est le sien),
  ADR 0108 (verdict d'efficacité), ADR 0121, ADR 0122
