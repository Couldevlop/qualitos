# ADR 0106 — Les cabinets partenaires : l'éditeur propose, le client accorde, et la date ferme

- **Statut** : Accepté
- **Date** : 2026-11-05
- **Portée** : `api-core` (domaine `partner`), console de l'éditeur, paramétrage d'un client, console d'un cabinet
- **Remplace / complète** : complète l'ADR 0079 (clients de la plateforme) et l'ADR 0082 (`@SaasOnly`)

## Contexte

Un consultant externe — cabinet de conseil, auditeur tiers, organisme de
formation — intervient chez plusieurs clients de la plateforme. Jusqu'ici la
seule façon de le faire entrer était de lui créer un compte **dans** le client,
donc un utilisateur du client, indistinguable d'un salarié : son départ se
gérait à la main, son accès n'avait pas de terme, et rien ne disait qu'un tiers
lisait le dossier qualité.

Trois parties sont en présence, et elles ne veulent pas la même chose :

- l'**éditeur** veut tenir la liste des cabinets avec lesquels il travaille et
  les proposer à ses clients ;
- le **client** veut décider qui entre chez lui, pour quoi faire, et jusqu'à
  quand ;
- le **cabinet** veut savoir chez qui son accès est ouvert aujourd'hui.

## Décision

### 1. Trois surfaces, trois autorisations, trois chemins

| Surface | Chemin | Rôle | Ce qu'on y fait |
| --- | --- | --- | --- |
| Console de l'éditeur | `/api/v1/partner-firms` | `SUPER_ADMIN`, `@SaasOnly` | déclarer un cabinet, inscrire ses consultants, **proposer** un client |
| Paramétrage d'un client | `/api/v1/partner-access` | `ADMIN`, `ADMIN_TENANT` | **accorder**, refuser, reprendre |
| Console d'un cabinet | `/api/v1/partner/portfolio` | `CONSULTANT` | lire son portefeuille, se rattacher |

Les chemins sont séparés **parce que les autorisations le sont**. Un chemin
unique aurait obligé à distinguer les droits à l'intérieur du service, là où ils
se lisent aujourd'hui dans l'URL et dans `SecurityConfig`.

`/partner/portfolio` n'est **pas** `@SaasOnly` : un client qui héberge lui-même
fait aussi intervenir des cabinets.

### 2. L'éditeur n'ouvre aucun accès

Proposer crée un accord au statut `PROPOSED` et rien d'autre. Seul
l'administrateur du client le fait passer à `GRANTED`, et lui seul le reprend.
C'est la même frontière que partout ailleurs dans la console : l'éditeur définit
le périmètre, le client est souverain dedans.

### 3. Un accord accordé est daté des deux bouts

La contrainte vit dans la **base**, pas seulement dans le service :

```sql
CONSTRAINT chk_partner_engagements_granted_dates
  CHECK (status <> 'GRANTED' OR (starts_on IS NOT NULL AND ends_on IS NOT NULL))
```

Un accès « jusqu'à nouvel ordre » n'est jamais repris : personne ne se souvient
de le faire. L'écran propose trois mois et laisse corriger — une valeur par
défaut raisonnable plutôt qu'absente.

### 4. Un seul accord vivant par couple client/cabinet

```sql
CREATE UNIQUE INDEX uq_partner_engagements_vivant
  ON partner_engagements (tenant_id, firm_id)
  WHERE status IN ('PROPOSED', 'GRANTED');
```

Index **partiel** : l'historique n'est pas contraint, sans quoi un client
n'aurait jamais le droit de refaire appel à un cabinet qu'il a remercié. Et la
contrainte est dans la base parce que le service ne peut pas l'assurer seul :
deux requêtes simultanées passent toutes les deux son contrôle avant que l'une
n'écrive.

### 5. L'accès se vérifie à chaque usage, sur trois conditions vivantes

`PartnerAccessService.requireAccess` rejoue, à chaque accès :

1. le cabinet est actif,
2. le consultant est actif,
3. l'accord est `GRANTED` **et** le jour est dans ses dates.

Aucune tâche de fond ne ferme un accès : **la date le ferme**. Un accord arrivé à
son terme disparaît du portefeuille sans qu'un traitement nocturne ait eu à
passer — donc sans qu'une panne de ce traitement puisse laisser un accès ouvert.

Désactiver un cabinet ne touche pas ses accords : le cabinet n'ouvre plus rien,
mais la trace des décisions que des clients ont prises reste — c'est celle qu'un
auditeur vient lire.

### 6. L'accord porte sur le cabinet, l'usage est nominatif

Un client contracte avec un cabinet, pas avec une personne : si le consultant
change en cours de mission, l'accès n'a pas à être renégocié. Qui s'est
réellement servi de l'accès est journalisé nominativement (`partner.access.used`).

### 7. Un consultant n'est l'utilisateur de personne

Son jeton **ne porte pas** de `tenant_id`. Il est donc tenu hors des écrans
métier par la **structure** — une troisième coquille Angular (`/partner`) et
`businessRoutesGuard` — et non en masquant des entrées de menu : l'adresse
`/nc/interne` reste tapable, un signet la rouvre, et un écran métier ouvert sous
son compte n'afficherait les données de personne.

Le rôle `consultant` figure dans `PlatformRoles.KNOWN` mais **jamais** dans
`ASSIGNABLE` : un administrateur de client ne doit pas pouvoir fabriquer un
consultant.

### 8. Le rattachement se fait par le courriel du jeton

`POST /partner/portfolio/link` lit le courriel **dans le jeton** et cherche une
fiche qui l'attend. Il ne crée rien : une personne dont aucune fiche ne porte le
courriel reste sans portefeuille, et c'est à son cabinet de l'inscrire. La route
ne doit pas pouvoir servir à s'inviter dans un cabinet.

## Conséquences

- Un tiers qui lit un dossier qualité est désormais **visible comme tiers**, avec
  un objet de mission et un terme.
- Le départ d'un consultant ne demande plus de ménage dans les comptes du client.
- `partner_firms` et `partner_consultants` ne portent pas de `tenant_id` et donc
  pas de RLS : ce sont des données de **plateforme**, comme `module_prices`
  (V5). `partner_engagements`, lui, appartient à un client et porte sa RLS.
- **Reste à faire** : l'entrée effective d'un consultant dans l'espace d'un
  client. Le serveur sait déjà l'autoriser (`requireAccess`), mais un jeton sans
  `tenant_id` ne peut pas porter la requête : il faudra une session déléguée,
  courte et journalisée, qui mérite son propre ADR. Tant qu'elle n'existe pas,
  le portefeuille dit chez qui l'accès est ouvert, sans l'ouvrir.

## Alternatives écartées

- **Un compte dans le client.** C'est l'état précédent. Rien ne distingue le
  tiers du salarié, et l'accès n'a pas de terme.
- **Un accord par consultant.** Chaque remplacement en cours de mission
  redeviendrait une négociation, pour une traçabilité qu'on obtient par la
  journalisation nominative de l'usage.
- **Une tâche de fond qui ferme les accès échus.** Une panne de cette tâche
  laisse des accès ouverts, et rien ne le signale. Vérifier les dates à l'usage
  n'a pas ce mode de défaillance.
- **Masquer les entrées de menu pour un consultant.** Ne ferme rien : l'adresse
  reste tapable.

## Tests d'invariant

- `PartnerConstraintsOnPostgresTest` (7 bancs, `@Tag("migration")`) : la base
  refuse deux accords vivants sur le même couple, un accord accordé sans dates,
  une fin antérieure au début, une portée inconnue, un compte servant deux
  cabinets, un courriel inscrit deux fois.
- `PartnerAccessServiceTest` (25 bancs) : les trois conditions vivantes, les
  dates, la portée, l'accord d'un autre client rendu `404` et non `403`.
- `PartnerFirmServiceTest` (18 bancs) : proposer n'ouvre rien, désactiver ne
  touche pas les accords, un consultant appartient au cabinet de son chemin.
- `platform.guard.spec.ts` : un consultant est renvoyé vers `/partner`, et seuls
  les deux rôles sans client sont consultés.
- `partner.service.spec.ts` : aucun identifiant de client ne voyage, sauf la
  proposition de l'éditeur, dont c'est l'objet.

## Références

- `CLAUDE.md` §16 (modèle de rôles), §18.2 #2 (`tenant_id` depuis le JWT)
- ADR 0079 (clients de la plateforme), ADR 0082 (`@SaasOnly`), ADR 0086 (RLS)
- `docs/specs/2026-11-05-console-super-admin.md`, lot 2
