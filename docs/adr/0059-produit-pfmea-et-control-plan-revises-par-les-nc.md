# ADR 0059 — Le produit, son PFMEA et son control plan, révisés par ce qui arrive

- **Statut** : Accepté
- **Date** : 2026-08-20
- **Owners** : @Couldevlop

## Contexte

Le module `risk` savait déjà tenir un FMEA : projets, lignes cotées S/O/D, RPN
calculé côté serveur. Il ne savait pas de **quoi** il parlait. Aucun référentiel
produit n'existait, donc rien ne reliait une analyse de risque à une pièce, à sa
nomenclature ou à sa gamme — et rien ne reliait un défaut constaté au poste au
mode de défaillance qui l'avait, ou n'avait pas, anticipé.

Il manquait aussi le document qui rend l'analyse opérante : le **control plan**,
qui traduit le PFMEA en contrôles réellement exécutés en production. Sans lui,
l'analyse de risque reste une étude que rien ne relie au poste de travail.

Enfin, ces deux documents sont des documents **approuvés**. Ils sont affichés au
poste et montrés à l'auditeur. Ils vieillissent : une non-conformité récurrente
contredit une cote d'occurrence, une CAPA close sur efficacité vérifiée la
dément dans l'autre sens. Aujourd'hui, personne ne le voit avant la revue
annuelle — et souvent, personne ne le voit du tout.

## Décisions

### 1. Étendre `risk` plutôt que créer un second moteur FMEA

Le PFMEA n'est pas un module neuf. `fmea_projects` reçoit un `product_id`
nullable, `fmea_items` une opération de gamme, une classification de
caractéristique et une priorité d'action.

**Pourquoi :** un second moteur aurait dupliqué le cycle de vie
DRAFT → ACTIVE → ARCHIVED, la cotation, les statistiques et leurs tests, pour
n'ajouter qu'une clé étrangère. Les deux moteurs auraient divergé au premier
correctif appliqué à un seul des deux.

**Coût assumé :** `risk` reste un module « historique », sans découpage
hexagonal, alors que `product`, `controlplan` et `revisionrequests` en ont un.
La couture est visible dans `PfmeaAdapter`, qui traduit les dépôts Spring Data
de `risk` vers le port dont le moteur de propositions a besoin.

### 2. L'Action Priority lit les trois notes ; le RPN les multipliait

Le RPN donne le même 120 pour (S=10, O=4, D=3) — une défaillance grave — et pour
(S=4, O=10, D=3) — une défaillance fréquente et bénigne. `ActionPriorityCalculator`
lit les trois notes séparément, par bandes, dans une table écrite en toutes
lettres.

**Pourquoi une table et non une formule :** un ingénieur qualité pointe une
table du doigt et la conteste. Il ne conteste pas une formule.

**Ce que nous ne prétendons pas :** la table AIAG-VDA est sous droits. Nos
bandes en suivent l'esprit, elles n'en sont pas la copie. Elles sont documentées
et testées ligne à ligne — c'est plus honnête qu'une fidélité affirmée qui serait
fausse quelque part sans qu'on sache où.

### 3. La boucle propose, elle n'applique pas

Chaque non-conformité créée et chaque CAPA close sur efficacité vérifiée déposent
une **demande de révision** dans `quality_revision_requests`. Un humain accepte
ou refuse ; le refus exige une note.

**Pourquoi :** un document approuvé qui bougerait tout seul serait un écart en
audit de certification. L'auditeur demande qui a décidé, quand, et sur quelle
base — et « le système l'a fait » n'est pas une réponse.

**Corollaire :** accepter n'écrit jamais dans le document en vigueur. La révision
suivante naît en brouillon, et son approbation reste une décision distincte,
réservée à un rôle plus étroit (`DIRECTOR_QUALITY` et au-dessus).

**Le refus est tracé** au journal chaîné, comme l'acceptation. Ne pas le tracer
laisserait croire que la proposition n'a jamais existé, alors que « on n'a pas
bougé » est une décision qualité que l'auditeur voudra lire.

### 4. Compter les NC est une approximation, et elle est nommée comme telle

La table AIAG d'occurrence se lit en défauts par million d'opportunités. La
plateforme n'a ni ordre de fabrication ni quantité lancée : elle ne connaît pas
le dénominateur. `OccurrenceProposalCalculator` traduit donc un **nombre de NC
sur douze mois glissants** en cote.

**Pourquoi l'assumer plutôt que l'ignorer :** la justification affichée à
l'utilisateur dit « 3 NC en 12 mois sur ce mode de défaillance (comptage de
non-conformités, faute de volume produit connu) ». Il peut refuser en une phrase.
Une proposition qui masquerait son approximation serait pire qu'une absence de
proposition.

**Une NC ne fait jamais baisser une cote.** Un défaut survenu ne peut pas être un
argument pour minorer un risque ; seule une CAPA dont l'efficacité a été vérifiée
peut proposer une baisse, et c'est un autre chemin.

### 5. Le rapprochement NC ↔ mode de défaillance reste déterministe et local

`FailureModeMatcher` compare deux sacs de termes — minuscules, accents dépliés,
mots vides écartés — par indice de Jaccard, au-dessus d'un seuil de 0,2.

**Pourquoi pas l'`ai-service` :** le calcul tourne dans le chemin de création
d'une non-conformité. Y brancher un service d'inférence ferait dépendre la saisie
d'un défaut constaté au poste de la disponibilité de ce service. Le jour où l'on
voudra des plongements lexicaux, on fournira une autre implémentation derrière la
même signature.

**Le résultat ne décide de rien** : il ordonne au plus trois candidats, affiche
les termes qui les ont motivés, et attend qu'un humain confirme. « Aucun mode ne
correspond » est un choix explicite, pas l'absence de choix — c'est lui qui
déclenche la proposition de créer une ligne de PFMEA.

### 6. Une seule demande en attente par cible, garantie par la base

`uk_revision_request_pending` est un index unique partiel sur
`(tenant_id, target_type, target_id)` limité aux demandes `PENDING`.

**Pourquoi en base et pas en Java :** deux non-conformités saisies au même
instant perdraient la course d'un contrôle applicatif, et le badge « à réviser »
afficherait vingt propositions identiques. La demande précédente passe
`SUPERSEDED`, elle n'est pas supprimée : l'historique des propositions est
lui-même une preuve.

### 7. `CONTROL_PLAN_LINE` n'existe pas dans l'énumération des cibles

`RevisionTargetType` compte trois valeurs — `PFMEA_ITEM`, `PFMEA_ITEM_CREATE`,
`CONTROL_PLAN_LINE_CREATE` — là où la conception initiale en prévoyait quatre.

**Pourquoi :** aucune source de ce lot ne propose de modifier une ligne de
control plan existante, et rien ne l'applique. Une constante que personne ne
produit ni ne consomme est du code mort déguisé en contrat. La rouvrir demandera
de décider comment une cible survit à la recopie des lignes dans une nouvelle
révision — ce qui n'est pas une question qu'on tranche en passant.

## Ce que ce lot ne tient pas

**L'approbation d'une révision devrait exiger un MFA.** La règle 18.2 §5 le
demande pour toute action critique, et approuver un control plan en est une.
Aucun module de l'engine ne sait aujourd'hui vérifier une revendication MFA du
jeton : l'approbation est protégée par le rôle seul. Mieux vaut une dette nommée
qu'une case cochée.

**L'ancrage blockchain n'est pas branché** sur l'approbation d'un control plan.
La décision est inscrite au journal chaîné du tenant, qui est lui-même ancré
périodiquement ; l'ancrage direct du document reste à faire.

**Les index partiels ne sont pas couverts par les tests.** H2, sur lequel tourne
la suite, ne les connaît pas. Les quatre index de ce lot se vérifient à la main
sur le PostgreSQL de la stack, et l'unicité est doublée côté service — les deux
ceintures, aucune des deux automatisée de bout en bout.

## Conséquences

- Migrations `V110` à `V114`.
- Trois modules neufs en Clean Architecture (`product`, `controlplan`,
  `revisionrequests`), couverts par les règles ArchUnit au même titre que
  `dpoappointments` et `marketplace`.
- Deux entrées au catalogue : `product` (sans dépendance) et `controlplan`
  (dépend de `risk` et de `product` — sans PFMEA il n'a rien à citer, sans
  produit il n'a pas de sujet).
- `NcService` publie un `NcCreatedEvent` après commit ; l'écouteur du moteur
  avale ses propres pannes. Une proposition manquée se rattrape à la prochaine
  NC ; une NC perdue ne se resaisit pas.
