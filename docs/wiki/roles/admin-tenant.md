# Rôle — Admin Tenant

[← Retour à l'index](../README.md)

## À quoi sert ce rôle

L'**Admin Tenant** administre **une organisation** (un tenant) dans QualitOS. Il ne gère pas la
plateforme entière (c'est le rôle du [Super Admin](super-admin.md)), mais il est responsable de
la configuration de son organisation : utilisateurs, rôles, paramètres, intégrations.

## Permissions clés (CLAUDE.md §16)

- Gérer les **utilisateurs** et leurs **rôles** au sein du tenant.
- Configurer les **paramètres** de l'organisation.
- Configurer les **intégrations** (LDAP / SSO).
- Consulter les **modules actifs** du tenant (l'activation/désactivation reste un acte Super Admin).

## Parcours typiques

### Mettre en route une nouvelle organisation

1. Créer les comptes utilisateurs et leur attribuer un rôle adapté
   ([Directeur Qualité](directeur-qualite.md), [Manager Qualité](manager-qualite.md),
   [Auditeur](auditeur.md), [Utilisateur](utilisateur.md)).
2. Brancher l'annuaire d'entreprise (LDAP / Active Directory / SSO) si l'organisation en dispose,
   pour que les utilisateurs se connectent avec leurs identifiants habituels.
3. Vérifier que les modules nécessaires sont actifs ; sinon, en faire la demande au Super Admin.

### Gérer le cycle de vie des accès

- Ajouter / désactiver des utilisateurs au fil des arrivées et départs.
- Réviser périodiquement les rôles attribués.

## Bonnes pratiques

- **Moindre privilège** : attribuez le rôle le plus restreint suffisant à chaque personne.
- **SSO de préférence** : centraliser l'authentification réduit les risques liés aux mots de passe.
- **Revue d'accès régulière** : retirez les accès devenus inutiles.

## Liens utiles

- [Super Admin](super-admin.md)
- [Directeur Qualité](directeur-qualite.md) · [Manager Qualité](manager-qualite.md)
