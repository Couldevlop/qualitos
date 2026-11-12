# ADR 0113 — L'équipe est nommée sur la fiche d'écart, et le D1 du 8D la reprend

- **Statut** : Accepté
- **Date** : 2026-11-11
- **Owners** : @Couldevlop
- **Portée** : `apps/api-quality-engine` (`quality/nonconformity` :
  `NcTeamMember`, `NcTeamMemberRepository`, `NcService`, `NcDto`,
  migration `V165__nc_quality_team.sql` ; `nonconformity/eightd` :
  `EightDSources`, `EightDSourceAdapter`, `EightDSnapshotAssembler`),
  `apps/web` (`shared/user-picker` : `UserTeamPickerComponent` ;
  `features/nc` : fenêtre de déclaration et fiche d'écart)
- **Complète** : ADR 0091 (sélecteur de personne de l'annuaire, `accountId` =
  `sub` du jeton) et le rapport 8D. Aucun des deux n'est révisé.

## Contexte

Le rapport 8D agrège les disciplines depuis les données du dossier : D2 depuis
la non-conformité, D4 depuis l'Ishikawa et les 5 Pourquoi, D5/D6 depuis la CAPA,
D7 depuis les actions préventives. Trois disciplines seulement se saisissaient à
la main, faute de source dans la plateforme : **D1 « Constituer l'équipe »**, D3
(mesures de sauvegarde) et D8 (reconnaissance).

Pour D3 et D8, c'est juste : personne d'autre ne sait ce qui a été endigué, ni
qui mérite d'être cité. Pour D1, c'était une **anomalie** : la fiche d'écart sait
depuis le premier jour qui s'en occupe — c'est même la première question posée en
revue. On la retapait donc dans le rapport, en texte libre, des semaines après
l'ouverture, de mémoire. Une discipline qu'on ressaisit est une discipline qu'on
oublie : le D1 partait vide, et un 8D dont le D1 est vide se discute mal devant
un client ou un auditeur.

Le texte libre posait un second problème : « M. Durand », « Marie D. » et
« marie.durand » désignent la même personne sans qu'aucune requête ne puisse le
savoir. On ne pouvait pas demander « sur quels écarts untel est-il sollicité ? ».

## Décision

**L'équipe est nommée à l'ouverture de la fiche, et elle est obligatoire.** Le
serveur refuse une création sans équipe (`@NotEmpty`). La fenêtre de déclaration
y pose **le déclarant d'office en tête** : il est celui qui a constaté l'écart, et
une équipe pré-remplie se corrige tandis qu'un champ vide se saute.

**Une table fille, et non une colonne de texte.** `nc_team_members` porte
`(tenant_id, nc_id, account_id, display_name, position)`, avec unicité sur
`(nc_id, account_id)` et deux index — par écart (la fiche la charge à chaque
ouverture) et par compte (« où suis-je sollicité ? »). Un nom caché au milieu
d'une chaîne séparée par des virgules ne se cherche pas.

**Deux colonnes pour une personne**, comme pour le signalant de la fiche :
l'identifiant de compte (le `sub` du jeton, jamais un libellé saisi) **et** le nom
figé à la saisie. Le nom est une photographie : un rapport 8D relu trois ans plus
tard porte les noms de l'époque, pas ceux d'un annuaire qui a bougé.

**L'ordre de saisie est de l'information.** `position` le conserve : le premier
nommé est, par convention d'atelier, celui qui pilote — l'écran l'annonce, et le
D1 écrit « Champion » devant son nom, « Member » devant les autres. Un tri
alphabétique perdrait le pilote.

**Le D1 reprend l'équipe, et la saisie libre vient ensuite sans la remplacer** :
elle dit ce que l'annuaire ne sait pas (un renfort extérieur, le rôle de chacun).
Une discipline est « renseignée » dès qu'il y a quelque chose à montrer, que ce
soit l'équipe de la fiche ou une saisie à la main — comme D3 et D8 ; seule
l'origine change le libellé.

**L'équipe se remanie tant que l'écart vit, en bloc.** Une mise à jour qui ne
parle pas de l'équipe ne la touche pas (`@Size(min = 1)` et non `@NotEmpty` :
l'absence signifie « inchangée ») ; une liste **fournie** ne peut pas être vide —
un écart dont plus personne ne répond ne se revoit pas. Sur un écart clos, annulé
ou rejeté, le serveur refuse toute modification et l'écran se contente de la
lire : c'est l'archive du D1.

**Borne haute : 12 personnes.** Au-delà, ce n'est plus une équipe de résolution
mais une liste de diffusion, et le D1 devient illisible. La borne protège aussi
l'écriture : une requête ne verse pas mille lignes filles d'un coup.

### Ce que la décision ne fait pas

- **Les écarts ouverts par une machine n'ont pas d'équipe** : un constat d'audit
  (`AuditFindingNcService`) ou un import EHR (`EhrConnectorService`) passe une
  liste vide. L'obligation pèse sur l'humain qui ouvre une fiche, pas sur un
  automate qui n'a personne à nommer. La fiche le dit alors explicitement, et le
  D1 reste à saisir — au lieu d'un blanc qu'on prend pour un bug.
- **Aucun droit n'est attaché à l'appartenance à l'équipe.** La visibilité des
  enregistrements reste celle de l'ADR 0081 ; être nommé sur un écart ne donne
  rien de plus. Un périmètre de visibilité fondé sur l'équipe serait une autre
  décision.

## Alternatives écartées

- **Garder le texte libre en l'alimentant d'un pré-remplissage.** Le champ
  resterait du texte : pas de requête par personne, pas d'ordre fiable, et deux
  orthographes du même nom resteraient deux personnes.
- **Réutiliser le signalant comme équipe.** Celui qui constate n'est pas toujours
  celui qui traite, et une équipe compte plusieurs personnes par définition.
- **Rendre l'équipe facultative.** C'est l'état d'avant sous un autre nom : le
  champ facultatif qu'on peut sauter est sauté, et le D1 repart vide.
- **Référencer l'annuaire par clé étrangère sans figer le nom.** Le rapport
  afficherait les noms d'aujourd'hui sur des faits d'hier, et une personne partie
  de l'organisation deviendrait un trou dans un document scellé.

## Conséquences

- ✅ Le D1 du rapport 8D est renseigné sans ressaisie, dès l'ouverture de l'écart.
- ✅ Les personnes sont désignées dans l'annuaire et affichées par leur nom —
  jamais un UUID ni du texte libre.
- ✅ « Sur quels écarts untel est-il sollicité ? » devient une requête indexée.
- ✅ La liste d'écarts lit toutes les équipes en **une** requête (pas de N+1).
- ⚠ Les écarts antérieurs à la migration n'ont pas d'équipe : leur fiche le dit,
  et elle se remplit au premier remaniement. Aucune reprise de données n'est
  possible — l'information n'existait nulle part sous forme exploitable.
- ⚠ Un composant de plus à maintenir côté écran (`qos-user-team-picker`), mais il
  enveloppe `qos-user-picker` au lieu de refaire la recherche : une seconde
  implémentation divergerait sur la saisie libre et les personnes inconnues.

## Tests d'invariant

- `NcQualityTeamTest` : une ligne par personne dans l'ordre de saisie
  (`position` 0..n), tenant du jeton, doublons et comptes vides écartés,
  remplacement en bloc, mise à jour muette sans effet, lecture par lot sur la
  liste.
- `NcControllerTest` : création sans équipe, équipe vide, membre sans nom, équipe
  au-delà de la borne → 400 ; mise à jour avec liste vide → 400 ; équipe remaniée
  → 200 ; la fiche rend l'équipe.
- `EightDSnapshotAssemblerTest` : D1 écrit « Champion » puis « Member », la saisie
  libre suit sans remplacer les noms, et une saisie seule renseigne la discipline.
- `user-team-picker.component.spec.ts` : ordre de saisie conservé, pilote annoncé,
  personnes déjà nommées exclues des propositions, borne haute, liste reçue jamais
  modifiée en place.
- `nc-create-dialog.team.spec.ts` / `nc-detail.team.spec.ts` : déclarant en tête,
  refus de déclarer sans équipe, équipe transmise telle quelle, écart clos en
  lecture seule, équipe vidée non enregistrable.

## Références

- CLAUDE.md §4.3 (Non-Conformance Management), §18.2 #2 (tenant depuis le jeton).
- ADR 0091 — sélecteur de personne de l'annuaire.
- `docs/wiki/modules/rapport-8d.md`, `docs/wiki/modules/non-conformites.md`.
