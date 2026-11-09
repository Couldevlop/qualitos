# ADR 0111 — Un seul mécanisme pour les listes déroulantes du client

- **Statut** : Accepté
- **Date** : 2026-11-09
- **Owners** : @Couldevlop
- **Portée** : api-quality-engine (`settings.catalog` : `CatalogId`, `CatalogMode`,
  `CatalogEntry`, `TenantCatalogService`, `TenantCatalogController`,
  `TenantCatalogEntryRow`), migration `V164`, apps/web
  (`core/catalogs/CatalogService`, écran `/admin/listes`)
- **Complète** : ADR 0101 (paramètres DMAIC) et ADR 0103 (natures d'écart) — ni
  l'une ni l'autre n'est révisée. Elles restent les deux réglages qui ont leur
  propre table, parce que leur donnée est structurée (questionnaire, échelle,
  rattachement à une catégorie) ; les listes plates passent désormais ici.

## Contexte

La promesse est « 100 % adaptable au domaine » (§5), et la règle 18.2 #9 est
sans détour : aucune liste sectorielle codée en dur. Le relevé du
2026-11-09 a trouvé douze listes déroulantes encore écrites dans les écrans —
catégories d'écart, types d'audit, familles de fournisseurs, incidents EHS,
projets APQP, natures de CAPA, types de modification, branches d'Ishikawa,
fréquences de KPI, mécanismes de détrompeur, zones d'audit 5S, secteurs de
parties intéressées.

Chacune pouvait recevoir le traitement déjà appliqué six fois : une table, une
entité, un service, un contrôleur, un écran. **Douze fois ce traitement coûterait
douze fois**, pour douze écrans que l'administrateur devrait apprendre
séparément.

Un second problème, plus sérieux, se cachait derrière le premier. Toutes ces
listes ne sont pas de même nature. Les zones d'audit 5S sont du texte libre côté
serveur : elles appartiennent entièrement au client. Les types d'audit, eux, sont
une énumération Java dont les valeurs ouvrent et ferment des étapes du programme.
Ouvrir les deux « de la même façon » laisserait un client ajouter `AUDIT_CROISE`
à une liste qu'aucun chemin de code ne sait traiter — et la plateforme
accepterait une donnée dont elle ne ferait rien.

Un troisième, invisible à la lecture du code : les écrans traduisent aujourd'hui
leurs libellés en six langues. Servir le libellé du serveur à la place ferait
lire « Interne » à un utilisateur arabophone.

## Décision

**Une table, une API, un écran.** `tenant_catalog_entries
(tenant_id, catalogue, code, label, position, active)` porte les douze listes et
celles à venir ; `GET/PUT /api/v1/settings/catalogs/{liste}` et `/reset` les
servent ; `/admin/listes` les règle. Ajouter une liste au paramétrage, c'est
ajouter une constante à `CatalogId` — ni table, ni contrôleur, ni écran de plus.

**Deux modes, imposés par le code et non par prudence.**

- `OPEN` — la liste appartient au client : il ajoute, retire, renomme,
  réordonne, désactive. C'est le cas quand le serveur stocke du texte (zones 5S,
  secteurs de parties intéressées).
- `RENAME` — les codes sont ceux de la plateforme : il renomme et réordonne,
  rien d'autre. L'ensemble des codes reçus doit être exactement celui de la
  plateforme, et aucun ne peut être désactivé. C'est le cas de toute liste
  adossée à une énumération du code.

La distinction est **dite dans l'écran avant d'être essayée** : sur une liste
`RENAME`, le bouton d'ajout n'existe pas et le code est montré, pas saisi.

**Aucune régression, aucune reprise de données.** Un client qui n'a rien réglé
n'a aucune ligne en base, et l'API rend les valeurs de la plateforme avec
`defaults: true`. **Les écrans de saisie gardent alors leurs propres libellés
traduits** : `CatalogService.labels()` rend `null` dans ce cas précis, et un
échec réseau rend aussi `null` — un formulaire doit rester utilisable quand le
paramétrage est injoignable. Dès que le client enregistre, ce sont ses mots qui
s'affichent, **dans sa langue** : traduire les mots d'un client serait les
trahir.

**Le code ne meurt jamais.** Retirer une entrée ne reclasse pas l'historique :
les enregistrements gardent leur code, affiché tel quel — ce qui se voit, et se
corrige. Une entrée peut être *désactivée* plutôt que retirée : elle disparaît
des nouvelles saisies et reste disponible pour relire le passé. C'était déjà la
règle d'ADR 0103 ; elle devient celle de tous les catalogues.

**Réservé à l'administrateur du client.** Lire est ouvert à tout authentifié —
sans la liste, pas de saisie, et l'API publique comme la file hors-ligne passent
par le même chemin. Changer exige `authz.manage`, comme tout paramétrage hors
gestion des utilisateurs.

**Comment les mots du client atteignent les écrans.** Les libellés sont rendus
par des fonctions pures appelées depuis des gabarits, des tableaux et des
exports : `auditTypeLabel(code)`, `ncCategoryLabel(code)`. Les rendre
asynchrones demanderait de réécrire une quarantaine d'écrans pour ce qui tient
dans une table de correspondance. Le réglage du client est donc **tenu en
mémoire** (`core/catalogs/catalog-overrides.ts`), rempli **une fois** à
l'ouverture de la session par un appel unique — `GET /settings/catalogs/custom`,
qui ne rend que les listes effectivement réglées — et **oublié à la
déconnexion** : un client ne lit jamais les mots d'un autre. Vide, tout retombe
sur les libellés de l'écran, traduits en six langues : exactement l'état d'avant
ce mécanisme. L'appel est rejoué après chaque enregistrement, pour que la
nouvelle liste vaille immédiatement, et son échec n'est pas une panne.

Deux fonctions suffisent aux écrans : `catalogOptions(liste, repli)` quand
l'écran porte des couples code/libellé, `catalogCodes(liste, repli)` quand il
porte un tableau de codes. Toutes deux prennent l'ordre et les mots du client,
réduisent la liste aux codes que l'écran propose — un diagramme en 6M ne se voit
pas offrir *Management* parce que le client l'a renommé — et n'y ramènent jamais
une entrée que le client a mise hors service.

## Conséquences

- Les douze listes relevées sont réglables, et la treizième coûtera une
  constante.
- Une zone d'audit 5S devient une donnée de référence au lieu d'un texte libre :
  la tendance par zone — la carte de chaleur promise au §3.2 — a enfin une clé
  stable à agréger.
- Les listes adossées au code n'accueillent pas de valeur nouvelle. Pour en
  ouvrir une vraiment, il faut remplacer son énumération par une donnée, ce qui
  est un chantier par module et un ADR à part. Le refus est explicite plutôt que
  silencieux : l'administrateur lit pourquoi, au lieu de voir sa valeur
  disparaître à l'enregistrement.
- Deux écritures des libellés coexistent : celles de la plateforme (six langues,
  dans les écrans) et celles du client (sa langue, en base). C'est le prix de la
  personnalisation, et il est payé une fois.
