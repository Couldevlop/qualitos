# ADR 0121 — Un dossier CAPA qui cite un écart EST le dossier de cet écart

- **Statut** : Accepté
- **Date** : 2026-11-15
- **Owners** : @Couldevlop
- **Portée** : `apps/api-quality-engine` (`quality/capa` : `CapaService` ;
  `quality/nonconformity` : `NcService` ; `quality/dmaic/resolution` :
  `ResolutionPorts.Projects`, `ResolutionService`, `ResolutionAdapters`),
  `apps/web` (`features/capa` : fenêtre d'ouverture d'un dossier) et la
  migration `V170__capa_rattachee_a_son_ecart.sql`.
- **Prolonge** : **ADR 0088** (un écart a un dossier) — qui posait le lien à
  l'escalade seulement ; **ADR 0108** (verdict d'efficacité) — qui sépare
  « fait » de « efficace ».
- **S'inscrit dans** : CLAUDE.md §3.6 (le référentiel transverse : l'agrégation
  est dans la donnée), §4.2 (CAPA), §4.3 (NC)

## Contexte

Quatre défauts, un même terrain : la frontière entre le registre des écarts, les
dossiers CAPA et les projets DMAIC.

1. **Le dossier ouvert depuis la liste CAPA restait orphelin.** Le lien
   `non_conformities.capa_case_id` n'était posé que par l'escalade depuis la
   fiche de l'écart. Ouvert depuis le registre des CAPA en choisissant la
   référence de l'écart, le dossier n'était rattaché à rien : la fiche de
   l'écart n'affichait pas son tableau d'actions, son rapport 8D ne le voyait
   pas, et sa clôture ne l'attendait pas.
2. **La source annonçait « interne » pour une réclamation client.** La liste
   d'ouverture ne propose qu'une source d'écart — « Non-conformité interne » —
   parce que seule l'escalade sait d'où vient l'écart. Un dossier né d'un écart
   externe s'affichait donc « interne » sur l'écran même qui sert à répondre au
   client.
3. **Un écart passait « résolu » au-dessus d'actions non exécutées.** La garde
   de la **clôture** existait (`unsettledCasesFor`), celle de la **résolution**
   non : la fiche de l'écart disait le travail fait pendant que celle du dossier
   affichait « en cours », et les deux sont lues par les mêmes personnes.
4. **Le projet DMAIC restait ouvert après que sa fiche n'eût plus rien à
   retenir.** Questionnaire complet, actions à 100 % : il fallait encore revenir
   sur la charte cliquer « avancer », et des projets achevés restaient comptés
   comme actifs dans les indicateurs.

## Décision

**Dériver plutôt que demander, et poser le lien à l'ouverture.**

1. **Le lien se pose à l'ouverture du dossier**, pas seulement à l'escalade : si
   la source désigne le registre des écarts et que la référence citée y désigne
   un écart, cet écart reçoit le dossier — **s'il n'en a pas déjà un**. Le
   premier ouvert reste le sien (ADR 0088) ; un second garde sa référence, donc
   la clôture de l'écart l'attend quand même. L'écart est lu sous verrou
   d'écriture : deux ouvertures simultanées ne peuvent pas s'écraser.
2. **La source d'un dossier né sur un écart est l'origine de cet écart**, jamais
   un choix de l'utilisateur : `EXTERNAL` donne `EXTERNAL_NON_CONFORMITY`, sinon
   `NON_CONFORMITY`. Le champ de la fenêtre le dit, faute de quoi il annoncerait
   autre chose que ce qui sera écrit.
3. **Résoudre un écart exige des actions exécutées**, c'est-à-dire aucun dossier
   `OPEN` ni `IN_PROGRESS` (`unexecutedCasesFor`). Un dossier `RESOLVED` ne
   bloque pas la résolution — ses actions sont faites, il reste à juger de leur
   efficacité — mais la **clôture** de l'écart, elle, continue d'attendre ce
   jugement (ADR 0108). Le refus nomme les dossiers en travail : « il reste des
   CAPA » envoie chercher, « il reste : Remplacer le roulement » dit quoi faire.
4. **Le projet DMAIC se clôt de lui-même** dès que sa fiche de résolution ne
   retient plus rien, selon **exactement** la condition qu'un passage manuel
   devait déjà franchir (`completionBlockers`). Rien ne se clôt plus tôt
   qu'avant : c'est la main de trop qui disparaît. Le jugement reste à la fiche,
   l'écriture au projet ; seul un projet `ACTIVE` est touché — suspendu, annulé
   ou déjà clos, il ne bouge pas dans le dos de celui qui l'y a mis — et la
   phase passe à `CONTROL`, un projet clos en phase « Mesurer » se lisant comme
   un abandon.

La migration `V170` rattrape l'existant : l'écart sans dossier reçoit le plus
ancien de ceux qui le citent, et la source de tout dossier citant un écart
s'aligne sur l'origine de cet écart. Les autres sources (audit, risque,
réclamation, alerte) ne sont pas touchées : elles ne désignent pas ce registre.

## Alternatives écartées

- **Proposer les deux sources d'écart dans la liste.** Elle demande à
  l'utilisateur ce que la base sait déjà, et une réponse fausse reste possible.
- **Interdire d'ouvrir un dossier depuis la liste.** Ce chemin est légitime et
  utilisé ; le fermer déplace le problème sans le régler.
- **Remplacer le lien quand un second dossier cite l'écart.** Le tableau
  d'actions du premier disparaîtrait de la fiche.
- **Refuser la résolution tant qu'un dossier n'est pas clos.** Ce serait
  confondre « fait » et « efficace » : l'efficacité se mesure après, et l'écart
  resterait bloqué des mois (ADR 0108).
- **Clore le projet sur un bouton de la fiche.** C'est le geste de trop qu'on
  retire ; et deux portes vers la même transition, c'est deux règles à tenir.

## Conséquences

- ✅ La fiche d'un écart montre les actions de son dossier, quel que soit le
  chemin par lequel ce dossier est né.
- ✅ Les deux fiches ne se contredisent plus sur l'état du travail.
- ✅ Un projet DMAIC achevé n'est plus compté comme actif.
- ⚠ Un écart dont le dossier est encore `OPEN` ne peut plus être résolu :
  l'ordre de travail est imposé — définir, exécuter, résoudre, clore.
- ⚠ La clôture automatique du projet n'inscrit pas d'acteur propre dans le
  journal du projet ; la trace reste celle de l'enregistrement de la fiche.

## Tests d'invariant

- `CapaServiceTest` : l'ouverture dérive la source de l'origine de l'écart, pose
  le lien, ne le remplace pas s'il existe, et n'invente rien quand la référence
  ne désigne personne.
- `NcServiceTest` : la résolution est refusée et nomme les dossiers non
  exécutés ; un dossier `RESOLVED` ne la bloque pas.
- `ResolutionServiceTest` : une fiche complète aux actions à 100 % clôt le
  projet une seule fois ; une fiche incomplète ou une action inachevée ne clôt
  rien.
- `ResolutionAdaptersTest` : seul un projet `ACTIVE` de son tenant se clôt ; la
  phase passe à `CONTROL` et la date du premier passage est gardée.
- `capa-create-dialog.source-ref.spec.ts` : l'indice de source dérivée paraît
  pour un écart, et pour lui seul.

## Références

- CLAUDE.md §3.6, §4.2, §4.3
- ADR 0088, ADR 0108
- `V170__capa_rattachee_a_son_ecart.sql`
