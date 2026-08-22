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

### 8. Le second facteur est exigé au moment de la signature

Approuver un control plan et accepter une proposition de révision passent par
`StepUpGuard`, qui lit dans le jeton la preuve qu'un second facteur a été
présenté — `acr` (palier atteint) ou `amr` (méthodes employées). Le contrôle est
**fail-closed** : sans preuve lisible, 403.

**Pourquoi pas le rôle :** un directeur qualité entré par mot de passe seul porte
exactement le même rôle qu'un directeur qualité entré par mot de passe et code à
usage unique. Seul le jeton distingue les deux.

**Pourquoi 403 et non 401 :** la session est valide, c'est sa force qui ne l'est
pas. Un 401 déclencherait une reconnexion silencieuse qui reproduirait le même
jeton, sans jamais demander le code.

**Ce que le realm doit fournir** — vérifié sur un Keycloak 25 réel, réponses à
l'appui :

| Situation | Étapes | `acr` |
|---|---|---|
| Connexion ordinaire | mot de passe | `silver` |
| `acr_values=gold` demandé | mot de passe + code | `gold` |

Le front demande `gold` quand le serveur répond 403 « step-up-required » : le
code est réclamé au moment de signer, pas à chaque connexion.

**Le piège, mesuré :** un OTP conditionné par le RÔLE ne relève pas l'`acr` — un
directeur qualité peut présenter un vrai second facteur et obtenir malgré tout un
jeton `acr=1`. Seul un sous-flux conditionné au PALIER l'inscrit. Et sans
sous-flux déclarant le palier 1, Keycloak traite une connexion qui ne demande
aucun palier comme une demande du palier maximal, et impose alors le code à tout
le monde. Les deux sous-flux vont donc par paire.

`infra/keycloak/apply-step-up.sh` pose tout cela sur un realm déjà en service —
`--import-realm` ne réécrit pas un realm existant. Il construit un flux neuf à
côté de l'ancien et bascule le realm dessus : le retour arrière est un champ à
reposer, et l'ancien flux n'est jamais touché.

### 9. Deux preuves à l'approbation : le journal, et le document

`ControlPlanService` inscrit `controlplan.plan.approved` et
`controlplan.plan.revision-opened` au journal du tenant, via un port dédié. Le
journal est ancré par lots, racine de Merkle soumise à la chaîne : y inscrire
l'approbation la rend infalsifiable sans multiplier les écritures on-chain.

**Ce que le journal ne dit pas :** il prouve que l'approbation a eu lieu, pas ce
qui a été approuvé. Les lignes vivent dans une autre table, qu'un accès direct à
la base modifierait sans laisser de trace au journal.

**Le scellement ferme ce trou.** À l'approbation, `ControlPlanFingerprint` rend
le SHA-256 du plan ET de ses lignes ; l'empreinte est signée (Ed25519 + ML-DSA-65)
puis ancrée, et les trois valeurs sont écrites sur le plan. Rejouer le calcul sur
le document rendu par l'API suffit alors à démontrer qu'il est bien celui qui a
été signé — c'est le geste de l'auditeur qui vérifie lui-même.

**Ce qui entre dans l'empreinte, et pourquoi ce n'est pas neutre :** l'identité
du plan, son approbateur, et chaque ligne entière — y compris le lien vers la
ligne de PFMEA qui la justifie. Pas les identifiants techniques des lignes :
ouvrir une révision les recopie, et s'ils comptaient, une révision qui ne change
rien produirait une empreinte différente. Pas la précision d'écriture d'une
tolérance non plus : `10.0` et `10.00` sont le même nombre, et laisser le pilote
de base décider de l'empreinte ferait accuser des documents intacts.

**L'échec du scellement fait échouer l'approbation**, transaction comprise
(§18.2 #5). Un plan approuvé mais non scellé serait affiché au poste et montré à
l'auditeur avec une preuve manquante que rien ne signalerait. Le scellement
précède donc l'écriture : une seule écriture, jamais de ligne approuvée sans
preuve, même transitoirement.

**Les plans approuvés avant cette migration ne sont pas scellés rétroactivement.**
Sceller après coup certifierait un contenu qu'on n'a pas vu approuver. Ils restent
couverts par le journal chaîné, et l'absence de scellement se lit telle quelle.

### 10. Un alias de rôle, parce que le realm et le code ne s'accordaient pas

Le realm nomme le rôle `quality_director` ; les neuf `@PreAuthorize` du code
qualité écrivent `DIRECTOR_QUALITY`. Un vrai directeur qualité ne correspondait
donc à aucune règle et se voyait refuser l'approbation — un refus muet, invisible
des bancs Web qui fabriquent eux-mêmes l'autorité qu'ils testent.

L'alias est posé une fois, dans le convertisseur d'autorités, et vaut dans les
deux sens. Même esprit que la compatibilité ROLE_ADMIN / ROLE_ADMIN_TENANT déjà
en place.

### 11. Ce que le realm ne reçoit qu'une fois se pose au déploiement

`--import-realm` n'importe le realm qu'au **premier** démarrage sur une base
vide. Sur un environnement déjà en service, modifier `realm-export.json` ne
change donc rien — et rien ne le signale.

`deploy.sh` pose désormais, après démarrage et de façon rejouable, ce que
l'import ne peut plus poser : l'anti-force-brute du realm master, la politique de
mot de passe, **l'URI de post-déconnexion du client web**, et **les paliers
d'authentification** (`apply-step-up.sh`, qui ne reconstruit rien s'il trouve le
realm déjà en place).

**Pourquoi l'URI de post-déconnexion figure dans cette liste :** c'est un réglage
distinct des URI de redirection, et Keycloak ne retombe pas dessus. Attribut
absent = aucune redirection autorisée après déconnexion. Mesuré sur la
préproduction : la connexion fonctionnait, la déconnexion répondait
`HTTP 400 — Invalid redirect uri`, et l'utilisateur restait sur une page d'erreur
Keycloak. Une panne qui n'apparaît qu'en sortant est une panne que personne ne
teste.

**Pourquoi appeler le script depuis le déploiement plutôt que de le documenter :**
une bascule d'environnement qui dépend d'une commande qu'il faut penser à taper
est une bascule qu'on oublie. L'échec n'arrête pas le déploiement — mieux vaut
une plateforme en ligne dont une action critique est refusée qu'une livraison
bloquée — mais il est signalé bruyamment, parce que le symptôme (403 à
l'approbation) n'évoque pas de lui-même sa cause.

## Ce que ce lot ne tient pas

**Le second facteur repose sur ce que le realm publie.** L'engine lit `acr` et
`amr` ; un fournisseur d'identité qui ne publierait ni l'un ni l'autre ferait
répondre 403 à toute approbation. C'est le comportement voulu — refuser plutôt
que supposer. La pose est désormais faite par le déploiement (§11), mais un
environnement dont le fournisseur d'identité n'est pas Keycloak demandera son
propre équivalent.

**La vérification d'un scellement n'est pas exposée.** L'empreinte et la
référence de transaction sont rendues par l'API et affichées ; recalculer
l'empreinte d'un plan et confronter sa signature reste, aujourd'hui, un geste
manuel. Un point d'API `verify` — comme celui des certificats de formation —
n'est pas fait.

## Conséquences

- Migrations `V110` à `V115`.
- Trois modules neufs en Clean Architecture (`product`, `controlplan`,
  `revisionrequests`), couverts par les règles ArchUnit au même titre que
  `dpoappointments` et `marketplace`.
- Deux entrées au catalogue : `product` (sans dépendance) et `controlplan`
  (dépend de `risk` et de `product` — sans PFMEA il n'a rien à citer, sans
  produit il n'a pas de sujet).
- `NcService` publie un `NcCreatedEvent` après commit ; l'écouteur du moteur
  avale ses propres pannes. Une proposition manquée se rattrape à la prochaine
  NC ; une NC perdue ne se resaisit pas.
- Les index partiels sont désormais **couverts par un test** : `PartialIndexesOnPostgresTest`
  démarre un PostgreSQL par Testcontainers, y rejoue les 113 migrations, et
  vérifie que chaque index mord — H2 les accepte sans jamais les appliquer. Le
  test se saute là où Docker n'est pas disponible, en le disant.
- `qualitos.security.step-up.enforced` vaut **true** par défaut. Un interrupteur
  de sécurité dont le défaut est « ouvert » finit par rester ouvert.
- L'approbation d'un control plan écrit trois colonnes de plus (`seal_sha256`,
  `seal_signature`, `anchor_tx_ref`) et ne peut plus aboutir si la chaîne est
  injoignable. C'est le comportement voulu, et il rend l'ancrage indisponible
  visible au moment où il compte plutôt que le jour de l'audit.
