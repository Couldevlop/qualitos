# ADR 0091 — Un annuaire en lecture restreinte pour tous les membres du client

- **Statut** : Accepté
- **Date** : 2026-10-14
- **Owners** : @Couldevlop
- **Portée** : `api-core` (paquet `directory`, `AppUser`, migration V8,
  `SecurityConfig`), ingress du chart, proxy du mode léger, `apps/web`
  (`core/directory`, `shared/user-picker`)
- **S'inscrit dans** : CLAUDE.md §16 (rôles), §18.2.2 (client lu dans le jeton),
  §11.1 A01 (contrôle d'accès)

## Contexte

`/api/v1/users` n'est lisible que par les administrateurs : il sert à
administrer les membres (rôles, invitation, compte de connexion, activation).
Or un manager qualité doit désigner le responsable d'un audit, d'une CAPA ou
d'un changement. Faute d'accès, les écrans en étaient réduits à une saisie
libre (deux orthographes = deux personnes, personne à notifier) ou à appeler
une API qui leur répondait 403.

Ouvrir `/api/v1/users` à tous aurait exposé à chaque membre les rôles, l'état
des comptes et les identifiants de connexion de tous les autres. Décision
produit validée : une lecture **restreinte** de l'annuaire pour tout
utilisateur authentifié du client.

## Décisions

### 1. Une surface dédiée, distincte de la gestion

`GET /api/v1/directory/users?q=&page=&size=` et
`GET /api/v1/directory/users/{id}`, servis par api-core. Les droits de
`/api/v1/users` ne changent pas. Toute autre méthode sur `/api/v1/directory/**`
est refusée par la configuration de sécurité (`denyAll`), pas seulement faute
de contrôleur : élargir cette surface doit se voir dans `SecurityConfig`.

### 2. Un contrat pauvre, et pourquoi l'adresse en fait partie

Réponse : `{ id, displayName, email, active }`.

- **Jamais** : rôles, client, identifiant de compte de connexion, statut
  d'invitation, dates de création ou de connexion.
- **`email`** : nécessaire. Un nom ne désigne pas une personne de façon
  unique — deux « Marie Martin » sont courantes dans un groupe hospitalier ou
  industriel —, et aucun autre champ restreint ne les départage. C'est
  l'adresse professionnelle, déjà connue des collègues ; c'est aussi le seul
  libellé des membres enregistrés avant cette décision (point 3).
- **`active`** : la recherche ne rend que des membres actifs, mais la lecture
  par identifiant rend aussi un ancien membre, pour afficher le responsable
  d'un dossier passé au lieu d'un identifiant ou d'un vide. L'indicateur
  permet de le signaler (« ancien membre »).

La réponse ne se met pas en cache partagé (`Cache-Control: no-store`).

### 3. Un nom affiché enregistré, une clé de recherche calculée

`app_users` ne gardait que l'adresse ; le prénom et le nom n'allaient qu'au
fournisseur d'identité. La V8 ajoute :

- `display_name` : « prénom nom » saisi à l'invitation et à la création d'un
  client. `NULL` pour les membres antérieurs ou créés sans nom : l'annuaire
  montre alors leur adresse.
- `search_key` : nom et adresse sans accents, en minuscules, recalculés par
  l'entité à chaque écriture (`@PrePersist`/`@PreUpdate`). Remplie avec
  l'adresse pour les lignes existantes, déclencheur `updated_at` suspendu le
  temps du remplissage.

La normalisation est faite par l'application et non par l'extension
`unaccent` : créer une extension demande des droits que le rôle des migrations
n'a pas toujours chez un client (ADR 0083, 0086), et la même fonction sert au
stockage et au terme cherché, donc ils ne peuvent pas diverger. Pas de
`pg_trgm` non plus : la recherche « contient » porte sur les membres actifs
d'un seul client (index partiel `(tenant_id, …) WHERE active`), quelques
centaines à quelques milliers de lignes.

### 4. Recherche bornée et validée

- `q` : facultatif, 100 caractères au plus après rognage, sans caractère de
  contrôle ; normalisé puis échappé (`%`, `_` pris à la lettre, échappement
  `!`). Sans terme : premiers noms de l'annuaire.
- `page` 0 à 999, `size` 1 à 50 (20 par défaut). Hors bornes : 400.
- Les refus portent un message fixe, jamais la valeur reçue.
- Tri : nom affiché (ou adresse), puis identifiant pour une pagination
  stable.
- Lecture par projection (`DirectoryRow`) : les rôles de l'entité ne sont pas
  chargés, ni exposés par accident.

### 5. Isolation et absence d'oracle

Le client vient du jeton (`TenantContext`) et est passé **explicitement** à
chaque requête, en plus du filtre Hibernate et de la RLS — une lecture par
identifiant échappe au filtre Hibernate. Un jeton sans client, ou un client
illisible : 401. Un identifiant d'un autre client répond le même 404, au même
message neutre, qu'un identifiant inexistant.

### 6. Débit

api-core n'a pas de limiteur de débit applicatif (le garde-fou par client
existant, ADR 0017, protège le chemin d'IA du moteur qualité), et l'ingress
n'en pose pas. L'abus est contenu par les bornes du point 4 : 50 personnes au
plus par appel, et seulement celles du client de l'appelant, qui pourrait de
toute façon les lire page par page. Un limiteur global à api-core reste à
décider pour l'ensemble de ses surfaces, pas pour celle-ci seule.

### 7. Routage

`/api/v1/directory` est nommé dans l'ingress du chart et dans le proxy du mode
léger. Sans cela, le préfixe tombait dans la règle fourre-tout `/api` et
partait au moteur qualité, qui répond 404.

### 8. La brique côté écran

- `DirectoryService` (`core/directory`) : `search(q, size, page)` et
  `get(id)`, ce dernier gardé 60 s par identifiant (500 entrées au plus,
  alimenté aussi par les recherches, un échec n'est pas gardé). Mode
  démonstration sans serveur : quelques personnes fictives.
- `qos-user-picker` (`shared/user-picker`, `UserPickerModule`) : champ de
  formulaire (`ControlValueAccessor`) dont la valeur est l'identifiant ;
  autocomplétion Material, frappe temporisée, pas de saisie libre, noms et
  adresses affichés, jamais d'identifiant — y compris pour une personne sortie
  de l'annuaire. Les modules audits, CAPA et changements l'importent ; ils ne
  sont pas modifiés par cette décision.

## Conséquences

- ✅ Un manager, un auditeur, un utilisateur peuvent désigner un responsable
  sans droit d'administration.
- ✅ La gestion des membres reste réservée aux administrateurs.
- ⚠ Tout membre du client voit les noms et adresses des membres actifs, y
  compris un auditeur externe du client (§16) : c'est le périmètre décidé. Le
  restreindre par rôle se ferait dans `SecurityConfig`, sans changer le
  contrat.
- ⚠ Les membres enregistrés avant la V8 apparaissent sous leur adresse tant
  qu'aucun nom ne leur est donné ; la synchronisation des noms depuis le
  fournisseur d'identité n'est pas faite ici.
- ⚠ Pas de limiteur de débit dédié (point 6).

## Tests d'invariant

- `DirectoryControllerTest` : un utilisateur sans rôle d'administration lit
  l'annuaire ; seuls les champs restreints sortent ; bornes de pagination et
  validation du terme (400 sans reflet) ; écritures refusées ; 401 sans jeton
  ou sans client ; 404 neutre.
- `DirectoryRepositoryTest` (base H2) : membres inactifs exclus, autre client
  invisible en recherche comme par identifiant, recherche insensible aux
  accents et à la casse, jokers pris à la lettre, tri, pagination, clé suivant
  un renommage.
- `AppUserDirectoryOnPostgresTest` : V8 sur un vrai PostgreSQL (valeur par
  défaut, échappement du LIKE, index partiel) — sauté sans Docker.
- `directory.service.spec.ts`, `user-picker.component.spec.ts`.

## Références

- CLAUDE.md §16, §18.2.2, §11.1 A01 ; ADR 0017, 0079, 0083, 0086.
