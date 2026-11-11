# Administrer son organisation — guide de l'administrateur du client

[← Retour à l'index](../README.md) · Routes : **`/admin/*`** · Menu : *Administration*

Cette page complète celle du rôle [Admin Tenant](admin-tenant.md) : elle décrit **écran par
écran** la section *Administration* du menu.

## La règle de base

> **Tout le paramétrage de l'application, à l'exception de la gestion des utilisateurs, est
> réservé à l'administrateur du client.** Un manager qualité pilote des dossiers ; il ne
> redéfinit pas les règles sous lesquelles ces dossiers sont jugés.

Deux conséquences pratiques :

- La section *Administration* n'apparaît qu'aux rôles d'administration de l'organisation. Le
  menu ne fait que masquer ce qui mènerait à un refus : **l'autorité reste le serveur**, qui
  revérifie chaque écriture.
- Les **règles chiffrées** que vous réglez ici (seuils de Cpk, poids des domaines d'impact,
  classification MOC…) sont **figées sur chaque dossier au moment où il franchit son étape**.
  Changer un seuil ne réécrit pas l'histoire : il vaut pour les dossiers suivants.

## Les écrans, un par un

| Écran | Route | À quoi il sert | Qui peut écrire |
|---|---|---|---|
| **Licence** | `/admin/licence` | Ce que couvre la licence de cette installation et jusqu'à quand : référence, date de validité, délai de grâce, comptes actifs sur le maximum autorisé, modules ouverts. Sur la plateforme de l'éditeur, l'écran l'indique — la licence ne s'y applique pas. | Lecture : administration du client |
| **Modules du tenant** | `/admin/modules` | Voir **de quoi votre organisation dispose** : les modules du socle sont toujours ouverts, les autres relèvent de votre abonnement. | **Lecture seule pour vous** : ouvrir ou fermer un module facturé est une décision de l'éditeur, pas un réglage du client |
| **Équipe & habilitations** | `/admin/team` | Créer le compte d'un membre (adresse e-mail, prénom, nom, rôles), rechercher un membre ou un rôle, **retirer** ou **rétablir** un accès. Un membre conserve toujours au moins un rôle. | Administration du client |
| **Rôles et droits** | `/admin/roles` | « Ce que chaque rôle peut faire, et qui le porte. » La matrice des droits, action par action ; glisser un membre sur un rôle le lui attribue. | `authz.manage` |
| **Paramètres MOC** | `/admin/moc` | Classification des changements, **poids des domaines d'impact**, rappel, et validateurs requis de la gestion des changements. | `authz.manage` |
| **Documentation** | `/admin/documents` | Revues périodiques, limites de fichiers, **arborescence** et processus de la documentation qualité (GED). | `authz.manage` |
| **Paramètres SPC** | `/admin/spc` | Les **seuils de Cpk** qui classent les études de capabilité (maîtrise, surveillance). | `authz.manage` |
| **Paramètres DMAIC** | `/admin/dmaic` | Le **questionnaire de la fiche de résolution**, les types de défaillance et les sévérités. | `authz.manage` |
| **Natures de NC** | `/admin/nc` | Sous chaque catégorie de non-conformité, les **natures d'écart** que votre activité rencontre réellement. | `authz.manage` |
| **Listes déroulantes** | `/admin/listes` | « Les mots que prennent les listes de l'application, dans votre maison. » Le réglage de toutes les listes plates de l'application en un seul endroit. | `authz.manage` |
| **Cabinets** | `/admin/cabinets` | « Qui, hors de votre organisation, peut entrer dans vos données — et jusqu'à quand. » Accorder, suivre et reprendre l'accès d'un cabinet partenaire. | Administration du client |
| **Fournisseur d'IA** | `/admin/ia` | « À qui partent les demandes faites à l'IA pour votre organisation. » | `authz.manage` |
| **Clés d'API** | `/admin/api-keys` | Émettre une clé d'API (portées et date d'expiration facultatives), la révéler une fois, la **faire tourner**, la révoquer. | Rôles d'administration du client |
| **Webhooks** | `/admin/webhooks` | Déclarer les abonnements sortants et suivre chaque **livraison** : dernière tentative, prochain renvoi, identifiant d'évènement. | Rôles d'administration du client |
| **Quotas d'API** | `/admin/quotas` | Les quotas et limites d'API de votre organisation, avec la **consommation sur la fenêtre en cours**. | Rôles d'administration du client |
| **Journal d'audit** | `/admin/audit-log` | Le **registre inaltérable** des actions faites dans votre organisation, filtrable par type de ressource, identifiant ou type d'action. | Lecture : rôles d'administration |
| **Connecteurs** | `/connectors` | Les connecteurs tiers (ERP, EHR/FHIR, outils de communication) : connexions, secrets, synchronisations, et leur état — une intégration peut être « désactivée sur erreurs ». | Rôles d'administration du client |

Deux écrans d'administration n'ont pas d'entrée dans ce menu, et c'est voulu : on les ouvre
depuis le module qu'ils règlent.

| Écran | Route | Où le trouver |
|---|---|---|
| **Ateliers de la matrice de compétences** | `/admin/competences` | Bouton « Ateliers » depuis la [matrice de compétences](../modules/efficacite-capa-et-competences.md) |
| **Circuits de validation** | `/admin/circuits` | « Qui approuve, et dans quel ordre. » Accessible par son adresse ; aucun lien de l'application n'y mène aujourd'hui |

## Trois écrans à comprendre avant de les toucher

### Rôles et droits (`/admin/roles`)

C'est l'écran le plus structurant de la section. Les droits se règlent **par organisation et
action par action** : ce n'est pas un rôle figé livré avec le produit, mais une matrice que vous
ajustez. Le droit `authz.manage` ouvre lui-même cette matrice : donnez-le avec parcimonie, et à
plus d'une personne — sinon un départ vous laisse sans administrateur.

Glisser un membre sur un rôle le lui attribue ; un membre doit toujours conserver au moins un
rôle.

### Listes déroulantes (`/admin/listes`)

Un seul écran règle toutes les listes plates de l'application — catégories d'écart, types
d'audit, familles de fournisseurs, incidents EHS, zones d'audit 5S, secteurs de parties
intéressées, et les autres.

Deux comportements existent, et **l'écran annonce lequel s'applique avant que vous n'essayiez** :

- **Liste ouverte** : elle vous appartient. Vous ajoutez, retirez, renommez, réordonnez,
  désactivez. C'est le cas quand l'application stocke du texte libre.
- **Liste à renommer** : les valeurs sont celles de la plateforme, parce que des chemins de
  traitement en dépendent. Vous pouvez les **renommer** et les **réordonner** — adopter votre
  vocabulaire — mais pas en ajouter ni en retirer. Ajouter une valeur qu'aucun traitement ne
  sait traiter reviendrait à enregistrer une donnée dont l'application ne ferait rien.

Un bouton de réinitialisation rend à une liste ses libellés d'origine.

### Fournisseur d'IA (`/admin/ia`)

Vous épinglez le fournisseur de modèle qui traitera les demandes de votre organisation. Deux
points à retenir :

- **Il n'y a aucun repli silencieux.** Si la politique n'est pas lisible ou si le fournisseur
  épinglé n'est pas configuré, l'appel échoue et le dit. La souveraineté de vos données ne se
  dégrade jamais sans que vous le sachiez.
- Chaque changement de politique est **journalisé** dans un registre chaîné.

## Ce qui ne relève pas de vous

Certaines décisions appartiennent à l'éditeur de la plateforme, et l'application vous le refuse
explicitement :

- **Ouvrir ou fermer un module facturé** : vous en gardez la lecture, pour savoir de quoi vous
  disposez.
- **Le catalogue des clients et des tarifs**, et la piste d'audit de la plateforme.
- **Proposer** une mission d'un cabinet partenaire : l'éditeur propose, **vous accordez**.
  L'accès que vous accordez porte sur le cabinet, est daté des deux bouts, et se referme à sa
  date — aucune tâche de fond ne décide à votre place.

## Bonnes pratiques

- **Réglez avant d'ouvrir le produit à vos équipes** : seuils, natures d'écart, listes et
  circuits. Un dossier ouvert fige les règles en vigueur ; changer après coup laisse deux
  générations de dossiers dans le même registre.
- **Au moins deux administrateurs**, toujours.
- **Retirer l'accès plutôt que supprimer** : un compte désactivé libère une place de licence et
  conserve l'attribution de ce qu'il a fait.
- **Relisez le journal d'audit périodiquement.** Il est inaltérable, mais personne ne le lit à
  votre place.
- **Datez les accès externes** : un accès de cabinet partenaire sans fin prévue est un accès
  permanent.
