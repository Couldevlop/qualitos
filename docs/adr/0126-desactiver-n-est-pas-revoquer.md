# ADR 0126 — Désactiver n'est pas révoquer

- **Statut** : accepté
- **Date** : 2026-11-16
- **Portée** : comptes utilisateurs (port d'identité, service, adaptateur Keycloak)

## Contexte

Un administrateur de client pouvait déjà retirer l'accès d'un membre, de deux
façons : le bouton de la console (`update` avec `active=false`) et la
désactivation dédiée (`deactivate`). Les deux faisaient la même chose :

1. `enabled=false` chez le fournisseur d'identité ;
2. `active=false` en base.

**Et c'est tout.** Aucune des deux ne fermait les sessions ouvertes.

Or ces deux écritures ne font que refuser les **nouveaux** jetons. Le jeton
d'accès que la personne a déjà dans son navigateur reste valable jusqu'à son
échéance : elle continue de lire, d'écrire et de signer pendant tout ce temps,
avec un compte que l'administrateur croit fermé et que la console affiche comme
inactif.

C'est précisément l'écart entre **désactiver** et **révoquer**. Et il se
manifeste au pire moment : on retire un accès en urgence les jours où l'on ne
peut pas attendre — un départ conflictuel, un compte soupçonné d'être compromis.
Un écran qui affiche « inactif » pendant que la personne travaille encore est
pire qu'un écran qui ne promet rien : il arrête l'enquête.

## Décision

**Retirer l'accès ferme les sessions, sur-le-champ.**

### Un verbe de plus au port d'identité

`IdentityProvider.logout(accountId)` ferme toutes les sessions du compte.
L'adaptateur Keycloak passe par `POST /users/{id}/logout`, qui invalide les
sessions et les jetons de rafraîchissement.

Les jetons d'accès déjà émis restent techniquement valides jusqu'à leur échéance
— 15 minutes, borne posée par `CLAUDE.md` §11.1 A07. C'est la borne que la
plateforme accepte ; ce qui change, c'est que la session **ne se renouvelle
plus**, là où elle se renouvelait indéfiniment.

### Les DEUX chemins, pas un seul

`deactivate(id)` **et** `update(id, active=false)`. Le second est celui que la
console emprunte : n'instrumenter que le premier aurait laissé la moitié du
produit sans révocation, et la moitié qui sert.

### Un échec de fermeture ne défait pas la désactivation

Le compte est déjà fermé en base et chez le fournisseur quand on ferme les
sessions. Relever l'exception annulerait la transaction et **rouvrirait un accès
qu'on voulait couper** : on aurait échoué à faire *plus*, et on finirait par
faire *moins*.

L'échec est donc journalisé en erreur — ce qui laisse une trace à reprendre — et
la session expire de toute façon d'elle-même. C'est le compromis le moins
mauvais : la seule alternative serait de refuser la désactivation tant que le
fournisseur ne répond pas, c'est-à-dire de faire dépendre une mesure d'urgence
de la disponibilité d'un service tiers.

## Conséquences

- Retirer un accès le retire vraiment, par les deux chemins.
- L'opération est **idempotente** : fermer les sessions d'un compte qui n'en a
  aucune réussit sans rien faire.
- ⚠ Fenêtre résiduelle de 15 minutes au plus sur un jeton d'accès déjà émis.
  La supprimer demanderait une liste de révocation consultée à chaque requête —
  un aller-retour de plus sur le chemin chaud, pour une fenêtre que la durée de
  vie des jetons borne déjà. Dette assumée, consignée ici.

## Vérification

- `UserServiceTest` (22) : la fermeture est demandée sur les **deux** chemins ;
  et quand le fournisseur ne répond pas, le compte reste désactivé — un banc qui
  échouerait si l'on « améliorait » le code en relevant l'exception.
- `KeycloakIdentityProviderTest` (10) : l'appel part bien sur
  `POST /users/{id}/logout`, et son échec remonte en `IdentityProviderException`
  plutôt que de passer inaperçu.
