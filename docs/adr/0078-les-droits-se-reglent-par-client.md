# ADR 0078 — Les droits se règlent par client, action par action

- **Statut** : Accepté
- **Date** : 2026-10-08
- **Owners** : @Couldevlop
- **Portée** : Module `authz` (api-quality-engine), page `/admin/roles` (web),
  directive `*qosCan` ; modules CAPA, NC, documents, registre des risques
- **Remplace en partie** : ADR 0020 (matrice d'autorisation par rôle) — pour
  les modules listés ci-dessus
- **S'inscrit dans** : CLAUDE.md §16 (rôles), §10.4 (paramétrage par tenant),
  §18.2 (tenant et acteur issus du jeton), ADR 0065 (autorisation avant la
  lecture du corps)

## Contexte

Les droits étaient des listes de rôles écrites dans le code : 131
`@PreAuthorize` côté serveur, seize constantes recopiées côté écran. Aucun client
ne pouvait retirer ou accorder une action sans une livraison. CAPA, NC et
documents n'avaient même aucune règle : tout utilisateur connecté pouvait clore
une NC ou approuver un document. L'écran « Équipe & habilitations » écrivait
des rôles dans une table que les services ne lisaient pas.

Le client veut paramétrer lui-même : autoriser ou non chaque action de chaque
membre, simplement (glisser-déposer).

## Décisions

### 1. Keycloak authentifie ; QualitOS autorise

Keycloak dit qui est l'utilisateur, de quel client il est, et quels rôles de la
plateforme porte son compte. Ce que ces rôles permettent, et les rôles en plus,
vivent dans la base de QualitOS, réglables par client.

### 2. Un catalogue d'actions dans le code, des réglages dans la base

`Permission` énumère les actions (`capa.create`, `nc.close`…) avec les rôles qui
les reçoivent PAR DÉFAUT, selon le tableau du §16 : l'utilisateur déclare une
NC, ajoute des photos et fait avancer ses actions ; l'auditeur vérifie
l'efficacité ; le pilotage qualité (administrateur, directeur, manager) ouvre,
traite et clôt ; seuls directeur, manager et administrateur approuvent et
publient. Le super administrateur (l'éditeur) a tout.

La base (V136) ne garde que ce que le client a CHANGÉ : un rôle système réglé,
un rôle sur mesure, un rôle attribué à un membre. Un client sans réglage n'a
aucune ligne et reçoit les droits livrés — y compris ceux d'une action ajoutée
plus tard au catalogue.

**Changement de comportement assumé** : ouvrir une CAPA, approuver ou publier
un document ne sont plus ouverts à l'utilisateur terrain. Un client qui veut
l'ancien comportement coche ces actions pour le rôle « Utilisateur ».

### 3. Les droits d'un membre = rôles du jeton ∪ rôles attribués

Un rôle porté par le compte Keycloak ne se retire pas depuis l'écran (il
s'administrera avec la création des comptes, lot suivant) ; les rôles attribués
dans l'application s'y ajoutent. Les attributions sont rangées par code de
rôle et par sujet du jeton.

### 4. Vérification avant la lecture du corps

`@RequiresPermission(Permission.X)` sur un point d'entrée ; `PermissionInterceptor`
refuse en 403 avant que le corps ne soit lu (même raison que l'ADR 0065). Sans
service de droits — les tranches `@WebMvcTest` —, il applique les droits livrés
aux rôles du jeton : jamais « tout permis ».

### 5. Un invariant contre le verrouillage

L'administrateur du client garde toujours `authz.manage` (domaine, et ligne
incohérente relue = défauts). Le super administrateur ne se règle pas depuis un
client (domaine, CHECK en base).

### 6. Cache court, assumé

Rôles d'un client et rôles d'un membre sont gardés 30 s en mémoire, oubliés sur
le nœud qui les modifie. Sur un autre nœud, un retrait de droit prend effet au
plus tard 30 s après.

### 7. Traçabilité

Création, réglage, suppression d'un rôle, changement des rôles d'un membre :
inscrits au journal chaîné, avec des codes et des identifiants seulement —
aucun texte libre (ADR 0076).

### 8. L'écran n'affiche que ce qui servira

`AuthzService` lit `/api/v1/authz/me` ; `*qosCan="'capa.create'"` n'affiche un
bouton que si l'action est accordée (rien tant que les droits ne sont pas lus,
rien si la lecture échoue). Le serveur reste seul juge.

## Conséquences

- Page `/admin/roles` : matrice actions × rôles (interrupteurs, enregistrement
  groupé), rôles sur mesure, rétablissement des droits livrés, équipe qu'on
  glisse sur un rôle (ou menu au clavier).
- Les autres modules (APQP, cercles, coût de la qualité, control plans…) gardent
  leurs listes de rôles jusqu'à leur bascule, module par module.
- Reste à faire, lots suivants : création des comptes et invitation (Keycloak
  Admin API), portée des droits (ses fiches, son site, tout le client), circuits
  de validation paramétrables, navigation filtrée par actions.

## Vérification

- `AuthzDomainTest`, `AuthorizationServiceTest` (cache, cloisonnement, défauts),
  `AuthzControllerTest`, `PermissionInterceptorTest`,
  `AuthzPersistenceIntegrationTest` (mapping JPA réel, journal d'audit),
  `SecurityConfigAuthorizationTest` (l'utilisateur déclare une NC, n'ouvre pas
  de CAPA).
- Web : `authz.service.spec` (service, directive, libellés),
  `roles-matrix.component.spec`.
