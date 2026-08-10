# ADR 0052 — Le tableau des actions CAPA devient lisible en audit (amende l'ADR 0050)

- **Statut** : Accepté
- **Date** : 2026-08-09
- **Owners** : @Couldevlop
- **Amende** : [ADR 0050](./0050-preuves-jointes-capa.md) (point 1 : rattachement de la preuve)

## Contexte

Le tableau des actions d'une fiche CAPA (§4.2) portait cinq colonnes : titre, statut,
échéance, date de complétion, bouton d'avancement. Un auditeur qui ouvre ce tableau
pose quatre questions auxquelles rien ne répondait :

1. **« Quand cette action a-t-elle été décidée ? »** — la fiche ne connaissait que
   `created_at`, la date de saisie dans l'outil. Les deux coïncident souvent, jamais
   par construction : une action arrêtée en comité le 12 mars et saisie le 2 avril
   se serait datée du 2 avril. Afficher `created_at` sous l'intitulé « décidé le »
   aurait produit une chronologie fausse dans un dossier qui sert de preuve.
2. **« Qui la porte ? »** — l'action ne stockait qu'`assignee_id`, un UUID. La colonne
   n'existait pas, et l'aurait-elle existé qu'elle aurait affiché
   `9c1f2b7e-0000-4000-8000-…`, ce qui n'apprend rien à personne.
3. **« De quel écart procède-t-elle ? »** — le lien vers la non-conformité existait
   côté NC (`capa_case_id`) mais ne remontait jamais jusqu'à l'écran.
4. **« Qu'est-ce qui prouve qu'elle a été faite ? »** — l'ADR 0050 avait posé la
   preuve au niveau du **dossier**, en écrivant explicitement que « le rattachement
   à l'action s'ajoutera sans rien casser si le besoin se confirme ». Il se confirme :
   une CAPA à huit actions dont trois seulement sont étayées ne se lit pas depuis une
   liste de pièces en vrac au bas de la fiche.

S'ajoutait un défaut d'usage : corriger le libellé ou le statut d'une action ouvrait
un dialogue. On corrige un libellé en le comparant aux lignes voisines ; un dialogue
les masque au moment précis où on en a besoin.

## Décision

1. **Date de décision portée par l'action** — colonne `decided_on DATE`, distincte de
   `created_at`. Saisie à la création (le formulaire la pré-remplit au jour même),
   ou **déduite explicitement** du jour d'enregistrement quand elle est omise, et
   corrigeable ensuite. Les lignes antérieures restent à `NULL` et l'écran affiche
   « — » : recopier `created_at` fabriquerait rétroactivement une décision que
   l'organisation n'a jamais enregistrée.
2. **Nom du porteur dénormalisé sur l'action** — colonne `assignee_name VARCHAR(255)`,
   qui **double** `assignee_id` sans le remplacer. L'identifiant reste ce qui rattache
   l'action à un compte ; le nom est ce qui se lit. Voir la justification ci-dessous :
   ce n'est pas un second annuaire, c'est l'absence d'annuaire consultable.
3. **Non-conformité d'origine portée par le DOSSIER**, pas répétée sur chaque action —
   `CaseResponse.sourceNonConformity {id, reference, title}`. Résolue par le lien réel
   (`non_conformities.capa_case_id`, le plus ancien écart rattaché), à défaut par la
   référence saisie dans `source_ref`, et **nulle** si cette référence ne désigne aucun
   écart. Résolue sur la fiche (`findById` et mutations), **jamais sur la liste
   paginée** : vingt dossiers vaudraient vingt requêtes pour une colonne que la liste
   n'affiche pas.
4. **Preuve rattachable à une action** — colonne `capa_evidences.action_id`, nullable.
   `NULL` = preuve du dossier (ADR 0050, comportement d'origine, toutes les lignes
   existantes). Non `NULL` = preuve de cette action. **Une seule pièce par action**,
   garantie par un index unique partiel : la colonne d'un tableau montre un document,
   pas une liste, et deux pièces rendraient la cellule indécidable. Remplacer se fait
   en deux gestes — retirer, puis reverser — qui se consignent tous les deux.
5. **Compteurs séparés, plafond de poids commun** — dix pièces au niveau du dossier,
   une par action ; le plafond cumulé de 25 Mo reste global. Le premier compteur
   protège la lisibilité d'une liste, le second protège un disque, et un disque ne
   distingue pas une pièce de dossier d'une pièce d'action.
6. **Rien de nouveau côté stockage ni côté refus** — même `ObjectStorage`, même liste
   blanche de types, mêmes signatures binaires vérifiées, mêmes codes (400 / 404 / 409
   / 413 / 503), même verrou de clôture, même inscription au journal chaîné (§11.5) —
   le journal porte en plus l'action visée, sans quoi il dirait qu'une preuve a quitté
   le dossier sans dire laquelle de ses lignes elle étayait. Clé d'objet préfixée
   `…/capa/{capa}/actions/{action}/`.
7. **Édition en ligne du libellé et du statut**, sans dialogue. Accessible au clavier
   (le focus entre dans le champ à l'ouverture, `Entrée` enregistre, `Échap` annule) et
   toujours annulable. Un échec serveur **laisse la ligne ouverte** : refermer
   effacerait la saisie que l'utilisateur doit justement corriger.
8. **PATCH réellement partiel sur une action** — `ActionRequest` n'est plus validé par
   Jakarta sur la mise à jour (un champ absent doit rester intouché), donc le **service**
   refuse un libellé vide, fait d'espaces, ou de plus de 255 caractères, via une
   `CapaValidationException` traduite en **400** (`Invalid CAPA Input`). Sans ce
   gestionnaire le refus tombait dans l'attrape-tout et sortait en 500 : l'API aurait
   annoncé une panne là où l'utilisateur avait vidé un champ. Conséquence : l'avancement
   d'un statut n'envoie plus le libellé, et n'écrase donc plus une correction faite
   entre-temps par quelqu'un d'autre.
9. **Colonne « Complétée » retirée** — la date de complétion passe dans l'infobulle du
   badge de statut. Huit colonnes tiennent ; neuf repoussaient le contenu hors écran.

## Justification

**Sur le nom du porteur — pourquoi dénormaliser plutôt que résoudre.** La plateforme
n'a pas d'annuaire exploitable depuis le moteur qualité, et ce n'est pas un oubli
qu'on pourrait contourner :

- `api-core` expose `/api/v1/users`, mais `UserDto.Response` ne porte **aucun champ de
  nom** — seulement `email`, `keycloakId`, `roles`. Il n'y a rien à résoudre.
- La route est réservée à `ADMIN` / `ADMIN_TENANT` / `SUPER_ADMIN`. Un Manager Qualité
  ou un Auditeur qui ouvre une fiche CAPA reçoit **403**. Afficher un nom aurait donc
  imposé d'élargir une politique d'autorisation pour une étiquette d'affichage.
- Les espaces d'identifiants divergent : le front alimente les champs d'assignation
  avec le `sub` du jeton (le `keycloakId`), tandis que `/api/v1/users/{id}` attend
  l'UUID applicatif. Une résolution naïve renverrait 404.
- Le moteur qualité n'a **aucun client HTTP vers api-core** ; en créer un pour une
  colonne aurait ajouté un couplage de service, un mode dégradé, et un cache à tenir.

La plateforme a déjà tranché exactement cette question, et on suit sa convention plutôt
que d'en inventer une seconde : le plan d'actions Ishikawa (migration `V100`) porte
`responsible VARCHAR(255)` — « un nom libre : tous les responsables n'ont pas de
compte » — et `decided_on DATE` — « la date de la réunion, du comité, de la revue »,
avec le même avertissement de ne pas la confondre avec une échéance. Le dashboard et le
mode TV affichent de même un `owner` qui est une chaîne. Les colonnes ajoutées ici
portent les mêmes noms et le même sens ; seul `assignee_name` diffère de `responsible`,
parce que l'action CAPA porte déjà un `assignee_id` qu'il s'agit de compléter, pas de
remplacer.

Il y a mieux qu'un pis-aller : **un dossier d'audit doit montrer le nom enregistré au
moment de la décision**. Une résolution vivante réécrirait l'histoire à chaque départ,
mariage ou changement d'affectation — le compte-rendu de comité de mars afficherait le
titulaire actuel du poste. La dénormalisation n'est donc pas seulement le seul chemin
praticable, c'est le bon.

**Sur la preuve par action.** L'ADR 0050 avait raison sur le niveau normatif : on
présente « les preuves de la CAPA », et ISO 9001 §10.2 raisonne au dossier. Mais la
question posée en revue est plus fine : « celle-ci, comment savez-vous qu'elle est
faite ? ». Les deux niveaux coexistent sans se concurrencer parce qu'ils répondent à
deux questions différentes, et parce que le second n'a rien coûté : même stockage,
même service, mêmes bornes.

**Sur l'édition en ligne.** Le dialogue reste la bonne réponse quand on crée (il y a
sept champs à remplir). Il est la mauvaise quand on corrige deux champs sur une ligne
qu'on lit en même temps que ses voisines.

## Alternatives écartées

- **Résoudre le nom via `api-core` depuis le front** : impossible sans élargir
  `/api/v1/users/**` aux rôles métier — une modification de politique d'autorisation
  pour une colonne d'affichage — et de toute façon inutile, l'annuaire ne stockant
  pas de nom. Afficher l'`email` à la place aurait exposé une donnée personnelle dans
  un tableau exporté et partagé.
- **Créer un client engine → api-core** : couplage de service, mode dégradé à définir,
  cache à invalider, et le défaut de fond intact — le nom affiché serait celui
  d'aujourd'hui, pas celui de la décision.
- **Ajouter `first_name` / `last_name` à `AppUser`** : migration api-core + synchronisation
  Keycloak + reprise de l'existant, pour une colonne de tableau. Reste ouvert si un
  besoin d'annuaire apparaît pour lui-même ; ce ne serait alors pas le même chantier.
- **Renommer `created_at` en « date de décision »** : gratuit, et faux. La colonne
  aurait menti sur toutes les actions saisies après coup, dans le document même qui
  sert à prouver la chronologie.
- **Rétro-remplir `decided_on` depuis `created_at`** : même mensonge, appliqué d'un
  coup à l'historique. `NULL` et « — » disent la vérité : l'information n'a pas été
  enregistrée.
- **Plusieurs preuves par action** : la cellule devient une liste, la colonne triple de
  largeur, et le remplacement d'une pièce cesse d'être un geste identifiable. Le
  dossier reste là pour ce qui déborde.
- **Un compteur unique pour les deux niveaux** : dix actions étayées auraient saturé le
  dossier et interdit d'y verser la moindre pièce d'ensemble.
- **Répéter la non-conformité sur chaque `ActionResponse`** : plus simple à consommer,
  mais laisse croire qu'elle peut différer d'une ligne à l'autre alors qu'elle est une
  propriété du dossier.
- **Valider `ActionRequest` avec `@Valid` sur le PATCH** : rendait le libellé obligatoire
  à chaque appel partiel, donc interdisait l'avancement de statut seul — celui-là même
  qui évite d'écraser les corrections concurrentes.
- **Un dialogue d'édition** : voir ci-dessus. Écarté pour la correction, conservé pour
  la création.

## Conséquences

- ✅ Le tableau répond aux quatre questions d'un auditeur sans quitter la fiche :
  quoi, quand décidé, par qui, sur quel écart, avec quelle preuve.
- ✅ Aucune infrastructure nouvelle : le stockage objet, les bornes, les signatures
  binaires et les codes de refus des preuves de dossier servent tels quels.
- ✅ Le PATCH partiel supprime une classe entière d'écrasements concurrents
  (l'avancement de statut ne renvoie plus le libellé).
- ✅ Les preuves de dossier et d'action ne se mélangent jamais : la liste du dossier
  filtre `action_id IS NULL`, sans quoi l'écran aurait montré deux fois la même pièce
  et laissé croire à un dossier deux fois mieux étayé.
- ⚠ **Le nom du porteur est une saisie, pas une identité vérifiée.** Rien ne garantit
  qu'il corresponde à `assignee_id`, ni qu'il soit orthographié pareil d'une action à
  l'autre. C'est le prix d'un affichage lisible en l'absence d'annuaire ; le jour où un
  annuaire nommé existera, il pourra pré-remplir ce champ sans changer le modèle.
- ⚠ **Le plafond de 25 Mo est partagé** entre le dossier et ses actions. Dix actions
  portant chacune une pièce lourde peuvent le heurter. Le refus est explicite et chiffré,
  jamais une suppression silencieuse — mais un dossier volumineux devra arbitrer.
- ⚠ **Les actions antérieures n'ont ni date de décision ni porteur nommé.** Les colonnes
  affichent « — » jusqu'à ce que quelqu'un les renseigne. C'est visible, et c'est voulu.
- ⚠ Huit colonnes débordent en largeur sur un portable : le tableau défile dans son
  propre conteneur. Le corps de page, lui, ne bouge pas.
- ⚠ Le nom du porteur est une donnée personnelle stockée en clair dans
  `capa_actions.assignee_name` et exportée avec le dossier. C'est le même statut que
  `ishikawa_actions.responsible` ; toute purge RGPD devra couvrir les deux.

## Tests d'invariant

- `CapaServiceTest` (62 cas, dont 15 nouveaux) : la date saisie est conservée, la date
  omise est déduite du jour **sans passer par `created_at`**, le nom vide retombe à
  `null`, un libellé vide ou trop long est refusé sans troncature, la mise à jour ne
  touche pas ce qu'elle ne reçoit pas, l'écart d'origine se résout par le lien réel
  puis par la référence, ne montre rien quand la référence ne désigne aucun écart, et
  la liste paginée n'interroge **jamais** le dépôt des non-conformités.
- `CapaEvidenceServiceTest` (39 cas, dont 12 nouveaux) : la clé d'objet désigne
  l'action, une action d'un autre dossier est refusée en 404, la seconde pièce d'une
  action est refusée, les deux compteurs ne se confondent pas, le plafond de poids
  s'applique aux deux niveaux, la liste du dossier ne remonte pas les pièces d'actions,
  une pièce de dossier ne se supprime pas par la route d'une action, et le journal
  porte l'action visée (`"actionId":null` pour une pièce de dossier).
- `CapaActionEvidenceControllerTest` (16 cas) : les six codes de refus, le 201 nominal,
  et l'auteur retenu depuis le sujet du jeton — nul quand le sujet n'est pas un UUID.
- `CapaControllerTest` (26 cas, dont 3 nouveaux) : l'édition en ligne en 200, le libellé
  vide en **400 et non 500**, et l'écart d'origine exposé dans la réponse.
- `CapaEntityCallbacksTest` (8 cas, dont 2 nouveaux) : `prePersist` ne touche pas à
  `decided_on` — c'est tout ce qui la distingue de `created_at`.
- Front `capa-detail.actions-table.spec.ts` (27 cas) : l'ordre des colonnes, la date de
  décision affichée et jamais reconstituée, le nom du porteur sans repli sur l'UUID,
  l'écart d'origine, le rangement de la pièce dans la ligne de son action, le dépôt sur
  l'action **visée** et non sur la dernière ligne rendue, les trois refus distincts, le
  stockage coupé, et l'édition en ligne — focus au clavier, `Entrée`/`Échap`, refus d'un
  libellé blanc sans appel serveur, charge utile réduite au libellé et au statut, ligne
  laissée ouverte sur échec.
- Front `capa.service.spec.ts` : les trois routes d'action-preuve, le PATCH partiel, et
  l'étanchéité des deux niveaux en mode démonstration.

## Références

- CLAUDE.md §4.2 (CAPA), §4.3 (non-conformités), §15.1 (i18n), §18.2 #2 (tenant issu du
  jeton), §22 (invariants de développement).
- ISO 9001:2015 §10.2 — non-conformité et action corrective.
- ADR 0050 — preuves jointes au dossier CAPA (amendé sur son point 1).
- ADR 0051 — traces et suites du dossier CAPA (journal chaîné, §11.5).
- Migration `V100__create_ishikawa_actions.sql` — précédent `responsible` / `decided_on`
  sur le plan d'actions Ishikawa, et son édition en ligne côté écran.
- Migration `V104__capa_action_decision_and_evidence.sql`.
