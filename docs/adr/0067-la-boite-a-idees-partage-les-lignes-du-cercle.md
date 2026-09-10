# ADR 0067 — La boîte à idées partage les lignes du cercle

- **Statut** : Accepté
- **Date** : 2026-09-10
- **Owners** : @Couldevlop
- **Portée** : Module `ideas` (dépôt, vote, arbitrage), module `circle` (propositions)
- **S'inscrit dans** : CLAUDE.md §3.6 (« l'agrégation est dans la donnée, pas dans
  l'UI »), ADR 0065 (l'autorisation avant la lecture du corps)

## Contexte

`circle.CircleProposal` couvrait déjà le cycle
`PROPOSED → UNDER_REVIEW → APPROVED → REJECTED → IMPLEMENTED → MEASURED` de la
maquette « Boîte à idées » (`docs/ECART-nouveaux-modules-qms.md`), mais deux
choses lui manquaient : le **vote**, et une **saisie hors cercle**. Une
proposition n'existait qu'à l'intérieur d'un cercle de qualité — l'opérateur
qui voit un rebut à son poste, sans être membre d'un cercle constitué, n'avait
nulle part où le dire.

Or l'isolation entre clients de `circle_proposals` reposait **entièrement**
sur ce cercle parent : aucune colonne `tenant_id` sur la ligne elle-même, le
tenant se déduisait par jointure. Rendre le cercle facultatif sans y remédier
aurait produit une ligne sans client déterminable — mélangée à toutes les
autres à la première liste.

Deux défauts préexistants se sont révélés en même temps : l'acteur d'une
proposition (`proposedBy`, `validatedBy`) était lu du **corps de la requête**,
en contradiction avec CLAUDE.md §18.2, et la garde « l'arbitre n'est pas le
proposeur » comparait deux valeurs écrites par le même appelant.

## Décisions

### 1. Un seul objet, deux façades — et pourquoi pas deux tables

`CircleProposal` reste la seule ligne. Le module `ideas`, neuf et en Clean
Architecture, l'expose sous `/api/v1/ideas` ; le module `circle` continue de
l'exposer sous `/api/v1/circles/{id}/proposals`, inchangé pour l'appelant.

Une table `ideas` séparée aurait dupliqué le référentiel que
`docs/ECART-nouveaux-modules-qms.md` désignait déjà comme le même objet vu de
deux portes : une idée validée en cercle et une idée déposée au comptoir
suivent la même machine à états, la même piste d'audit, le même compteur de
vote. Les distinguer en base aurait fallu choisir, à chaque lecture
transverse (référentiel §3.6, dashboards), laquelle des deux tables
interroger — et aurait rouvert exactement le problème que l'ADR 0057 avait
tranché pour les référentiels : ne pas faire cohabiter deux vérités pour une
même notion.

### 2. `tenant_id` porté par la ligne : sans lui, une idée sans cercle est une ligne sans client

`ALTER TABLE circle_proposals ADD COLUMN tenant_id UUID NOT NULL` (V125), avec
reprise : chaque ligne existante hérite du tenant de son cercle avant que la
contrainte ne se pose. Un déclencheur (`trg_circle_proposals_tenant_matches_circle`)
referme la porte que la reprise a ouverte : si la ligne **a** un cercle, son
tenant doit être celui du cercle — un `CHECK` ne pouvant pas interroger une
autre table, seul un déclencheur le peut. Le service refuse plus tôt, avec un
message lisible ; le déclencheur reste le filet, pas la première ligne de
défense.

Amorcer le tenant à l'écriture plutôt qu'à la lecture (contrairement à l'ADR
0066 sur le cycle APQP) : ici il n'y a rien à amorcer, seulement une colonne
manquante sur des lignes qui existent déjà et savent, par leur cercle, à qui
elles appartiennent. La reprise se fait une fois, dans la migration, jamais
au moment d'une requête.

### 3. La fenêtre de vote se ferme à la décision

`IdeaStatus.voteOpen()` répond vrai pour `PROPOSED` et `UNDER_REVIEW`, faux
pour les quatre statuts qui suivent une décision (`APPROVED`, `REJECTED`,
`IMPLEMENTED`, `MEASURED`). Voter sur une idée déjà tranchée poserait une
question sans objet : le compteur doit dire l'adhésion **au moment de la
décision**, pas celle d'aujourd'hui, sans quoi une idée approuvée il y a six
mois continuerait de voir son score bouger et laisserait croire que le vote
a encore un effet.

`VoteClosedException` rend 409 et non 422 : la demande de voter est
parfaitement licite dans l'absolu, c'est le moment qui ne l'est plus — à la
différence d'une transition d'état impossible (`IdeaStateException`, 422),
qui ne deviendra jamais licite quel que soit le moment.

### 4. Le décompte est calculé, jamais stocké

`ProposalVoteJpaRepository.countByIdea` agrège `proposal_votes` à chaque
lecture du tableau ; aucun compteur dénormalisé sur `circle_proposals`. Un
compteur stocké se désynchronise à la première voix retirée en dehors du
chemin qui l'incrémente — migration, script, correctif manuel — et personne
ne le détecterait avant qu'un score affiché ne mente. Le calcul coûte une
jointure groupée sur une table indexée par tenant ; c'est le prix accepté
pour qu'un score affiché soit toujours celui des lignes de `proposal_votes`,
sans état intermédiaire à tenir en cohérence.

La clé primaire de `proposal_votes` — `(proposal_id, voter_id)` — porte elle
aussi une règle plutôt que de la déléguer au service : « une voix par personne
et par idée » reste vraie même pour deux clics simultanés, ce que le code
seul laisserait passer entre la lecture et l'écriture.

### 5. L'acteur vient du jeton — le défaut corrigé, et pourquoi il comptait double ici

`CircleDto.ProposalRequest` portait un champ `proposedBy` ; `ApproveProposalRequest`
et `RejectProposalRequest` portaient un `validatedBy`. Les trois ont été
retirés : l'auteur d'un dépôt et l'arbitre d'une décision viennent désormais
de `CurrentUser.requireUserId()`, jamais du corps (CLAUDE.md §18.2). Le module
`ideas`, neuf, ne pouvait pas reproduire ce défaut — mais corriger le module
`circle` était la condition pour que les deux façades racontent la même
proposition sans contredire l'audit l'une de l'autre : un dépôt fait depuis
l'écran cercle et un dépôt fait depuis l'écran idées doivent produire la même
garantie sur qui a proposé quoi.

Le défaut comptait double ici précisément parce qu'une garde existait déjà et
semblait fonctionner : « le validateur ne peut pas être le proposeur »
(`CircleService.approveProposal`). Tant que les deux valeurs venaient du même
appelant, la garde comparait ce qu'il avait écrit à ce qu'il avait écrit — un
appelant malveillant s'auto-validait en changeant simplement l'UUID envoyé.
La garde ne devient vraie qu'une fois l'acteur lu du jeton des deux côtés de
la comparaison ; c'est le même défaut que l'ADR 0065, mais côté donnée plutôt
que côté autorisation.

### 6. Déposer et voter ouverts à tous, arbitrer réservé

`IdeaController` porte `@PreAuthorize("isAuthenticated()")` en tête de classe
pour `board`, `submit`, `vote`, `unvote` : n'importe quel utilisateur
authentifié du tenant. Les cinq transitions (`review`, `approve`, `reject`,
`implement`, `measure`) portent chacune
`hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')`
— exactement la liste qui gouverne déjà les quatorze autres référentiels de
méthode du module (ADR 0066 §6).

Réserver le dépôt aux membres d'un cercle aurait reconduit le défaut d'origine
sous une autre forme : une boîte à idées qui exige un cercle pour y déposer
n'en est plus une. Mais retenir une idée engage l'organisation à la mettre en
œuvre, et l'écarter la retire de la vue de tous avec un motif qui doit rester
défendable en audit — ce sont des actes de pilotage, pas des actes d'usage.

## Conséquences

- ✅ Une idée se dépose sans jamais être passée par un cercle de qualité ;
  une idée née en cercle se vote et s'arbitre indifféremment depuis les deux
  écrans, sur la même ligne.
- ✅ Le score affiché sur une idée tranchée est figé par construction, sans
  code dédié pour l'empêcher de bouger : il n'existe simplement plus de vote
  possible à compter après décision.
- ✅ `proposedBy` et `validatedBy` ne peuvent plus être falsifiés par le corps
  de la requête, sur les deux façades — la garde « l'arbitre n'est pas
  l'auteur » redevient une vraie garde.
- ⚠ Le contrat de l'API cercle change : `ProposalRequest`, `ApproveProposalRequest`
  et `RejectProposalRequest` perdent un champ chacun. Un appelant externe qui
  envoyait encore `proposedBy` ou `validatedBy` voit ce champ simplement
  ignoré (les DTO ne le portent plus) et non rejeté — mais un appelant qui
  s'appuyait sur ce champ pour attribuer une proposition à un tiers (import,
  script de reprise) perd ce comportement sans avertissement au niveau HTTP.
- ⚠ La migration V125 recopie le tenant depuis le cercle parent pour chaque
  ligne existante, une fois, à l'application de la migration : une ligne dont
  le cercle aurait déjà été supprimé avant V125 (orpheline de fait, cas non
  rencontré en production à ce jour) resterait sans tenant déterminable et
  ferait échouer la contrainte `NOT NULL` plutôt que de se voir attribuer un
  tenant deviné.
- ⚠ `proposed_by_name` est copié au dépôt, pas résolu à l'affichage (même
  choix que `reporter_name` sur la non-conformité) : les propositions
  antérieures à V125 n'ont pas ce nom et l'affichent `null` plutôt qu'un nom
  reconstitué a posteriori.

## Vérifications

- `IdeaTenantBackfillOnPostgresTest` — la reprise donne à chaque proposition
  existante le tenant de son cercle ; une idée sans cercle devient possible ;
  le déclencheur refuse un cercle d'un autre tenant ; un même votant ne vote
  pas deux fois pour la même idée (contrainte de clé, pas seulement le
  service) ; les voix disparaissent avec l'idée (`ON DELETE CASCADE`).
- `IdeaTest` — la fenêtre de vote par statut ; le parcours complet déposée →
  mesurée ; refus d'approuver hors séquence ; refus qu'un proposeur valide sa
  propre idée ; refus d'écarter sans motif ; aucun retour en arrière depuis
  un statut engagé.
- `IdeaMapperTest` — aucune perte d'information dans la traduction
  `CircleProposal` ↔ `Idea`, dans les deux sens, sur les six statuts.
- `CircleProposalActorTest`, `CircleServiceTest`, `CircleControllerTest`,
  `CircleGoldenPathTest` — l'acteur d'une proposition de cercle vient du
  jeton et non du corps, y compris sur le chemin complet dépôt → approbation.
- `HexagonalArchitectureTest` (deux règles dédiées) — `ideas.domain` et
  `ideas.application` sans dépendance à Spring, JPA ou Jakarta.
- Suite `IdeaControllerTest` / façade `/api/v1/ideas` — dépôt et vote ouverts
  à tout authentifié ; les cinq transitions fermées hors des quatre rôles
  d'arbitrage ; 404 sur une idée inconnue du tenant, 409 sur un vote fermé,
  422 sur une transition impossible.
- Front `IdeasBoardComponent` — quatre colonnes tenues par le statut reçu du
  serveur ; le vote et son retrait ; les commandes d'arbitrage absentes pour
  qui n'a pas le rôle ; six langues.

## Références

- `CLAUDE.md` §3.6 (l'agrégation est dans la donnée, pas dans l'UI), §16
  (modèle de rôles), §18.2 (aucun acteur ni `tenant_id` lu du corps).
- `docs/ECART-nouveaux-modules-qms.md` — la boîte à idées, rang 1 de l'ordre
  proposé, livrée par ce lot.
- ADR 0057 — les référentiels appartenant à un tenant, même refus d'une
  double vérité pour une même notion.
- ADR 0065 — l'autorisation décidée avant la lecture du corps ; même défaut
  d'acteur falsifiable, ici côté donnée plutôt que côté filtre de sécurité.
- ADR 0066 — le cycle APQP appartient au tenant, même liste de rôles réservée
  au pilotage qualité.
- Migration `V125__ideas_tenant_and_votes.sql`.
