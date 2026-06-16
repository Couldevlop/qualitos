# Rôle — Super Admin (plateforme)

[← Retour à l'index](../README.md)

## À quoi sert ce rôle

Le **Super Admin** exploite la plateforme QualitOS dans son ensemble (niveau multi-tenant).
Il ne pilote pas la qualité d'une organisation au quotidien : il **administre les organisations
clientes (tenants)**, décide quels modules et quels packs sectoriels sont activés pour chacune,
et supervise le fonctionnement global.

C'est le rôle le plus élevé. Il s'utilise avec parcimonie et **MFA obligatoire**.

## Permissions clés (CLAUDE.md §16)

- Créer / désactiver des **tenants** (organisations clientes).
- Activer / désactiver les **modules** et les **Industry Packs** par tenant.
- Gérer la **facturation** et les options d'abonnement.
- Accéder aux **logs globaux** de la plateforme.

## Parcours typiques

### Activer un tenant et son périmètre fonctionnel

1. Créer l'organisation cliente.
2. Activer les modules dont elle a besoin (un tenant ne voit que ses modules actifs ; les écrans
   non activés ne sont pas chargés).
3. Activer le ou les **packs sectoriels** correspondant à son domaine — voir
   [Packs sectoriels](../modules/industry-packs.md).
4. Désigner un **Admin Tenant** côté client, qui prendra ensuite la main sur les utilisateurs.

### Superviser

- Suivre l'usage des modules et la facturation par tenant.
- Consulter les journaux globaux en cas d'incident.

## Bonnes pratiques

- **Principe du moindre privilège** : n'activez que les modules réellement utilisés par un tenant.
- **Sépare exploitation et métier** : ne pilotez pas de cycles qualité avec ce compte ; déléguez
  à l'Admin Tenant et aux managers.
- **Traçabilité** : toute action d'administration sensible est journalisée.

## Liens utiles

- [Admin Tenant](admin-tenant.md) — le relais côté organisation cliente.
- [Packs sectoriels](../modules/industry-packs.md)
- [Standards Hub](../modules/standards-hub.md)
