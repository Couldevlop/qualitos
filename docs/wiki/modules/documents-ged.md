# Module GED — Documentation qualité

[← Retour à l'index](../README.md) · Route : **`/documents`** · Menu : *Opérations › GED*
Administration : **`/admin/documents`** · Menu : *Administration › Documentation*

## À quoi sert ce module

La **GED** (gestion électronique des documents) tient le référentiel documentaire du système
qualité : politiques, procédures, modes opératoires, enregistrements, formulaires, manuels et
documents externes. Chaque document y vit par **versions**, et une version ne devient applicable
qu'après avoir été **signée** par les personnes désignées pour la valider.

Les types proposés sont : Politique, Procédure, Mode opératoire, Enregistrement, Formulaire,
Manuel, Document externe, Autre.

## Parcours pas à pas

### Déposer un document

1. **Ouvrir** `/documents`, puis **Nouveau document**.
2. Renseigner son **type**, son **dossier** dans l'arborescence et le **processus** auquel il se
   rattache (les deux listes appartiennent à votre organisation — voir *Administration* plus bas).
3. **Joindre le fichier** : il est exigé à l'écran dès la création.
4. Le document est créé en **Brouillon** avec sa première version.

### Soumettre une version à la signature

1. Ouvrir la fiche du document, puis **Soumettre**.
2. **Désigner de un à trois signataires**, dans l'ordre où ils devront décider. Chaque signataire
   est un utilisateur de l'annuaire, choisi par son nom — jamais un identifiant technique.
3. Le serveur refuse une liste vide, plus de trois personnes, un doublon, ou l'auteur de la
   version lui-même.
4. Une version **sans fichier ne part pas en signature**.

### Signer, ou rejeter

- On signe **dans l'ordre 1 → 2 → 3** : seul le signataire dont c'est le tour peut décider.
- La fiche affiche une **carte par signataire** (« Signataire 1 », « Signataire 2 »,
  « Signataire 3 ») avec son nom, son état — *En attente*, *Approuvé*, *Rejeté* —, sa date et son
  commentaire.
- **Rejeter exige un motif** : la version repart en brouillon avec sa raison, et la soumission
  suivante redésigne ses signataires et repart du rang 1.
- La **dernière approbation** fait passer la version à *Approuvée*.
- C'est la **désignation qui donne le droit de signer** : un signataire sans rôle qualité peut
  signer la version pour laquelle il a été désigné.

### Retrouver ce qui vous attend

La liste propose cinq vues : **Tous les documents**, **Mes documents**, **À valider par moi**,
**Revues à échéance**, **Archives (obsolètes)**.

« À valider par moi » ne montre une version **qu'à la personne dont c'est le tour**, et jamais ses
propres versions.

## Comment lire le résultat

Deux statuts cohabitent, et ils ne disent pas la même chose :

| Statut de la **version** | Ce qu'il signifie |
| --- | --- |
| Brouillon | en cours d'écriture, pas encore soumise |
| En revue | soumise, en attente de ses signataires |
| Approuvée | tous les signataires ont approuvé |
| Actuelle | version publiée, celle qui s'applique |
| Obsolète | remplacée par une version plus récente |

| Statut affiché du **document** | Ce qu'il signifie |
| --- | --- |
| Brouillon | aucune version publiée |
| Approuvé | une version est en vigueur |
| En révision | une nouvelle version est en cours |
| Revue dépassée | la date de prochaine revue est passée |
| Archivé | le document ne s'applique plus |

Les cartes de signature de la dernière version signée **restent affichées en lecture seule** après
publication : elles sont la trace de qui a signé, quand, et avec quel commentaire.

## Ce que l'administrateur du client règle

L'écran `/admin/documents` est réservé à l'administrateur de l'organisation. Il fixe :

- **Fichiers** : taille maximale par fichier, quota de stockage, durée de conservation des
  fichiers des versions obsolètes.
- **Revue périodique** : périodicité en mois et préavis en jours.
- **Arborescence** : les dossiers (quatre niveaux au plus). Un dossier qui contient des
  sous-dossiers ou des documents ne se supprime pas.
- **Processus** : les processus de la cartographie auxquels un document se rattache. Un processus
  rattaché à des documents ne se supprime pas.
- **Stockage** : l'espace réellement occupé et la part du quota consommée.

Deux économies sont automatiques et n'ont rien à régler : **un fichier n'est stocké qu'une fois**
par organisation (deux dépôts du même contenu partagent le même objet, et une nouvelle version
hérite du fichier de la précédente jusqu'à ce que vous le remplaciez), et **la compression n'est
appliquée que si elle gagne** — les formats déjà compressés (docx, xlsx, pptx, JPEG, PNG, WebP)
sont stockés tels quels.

Enfin, si une durée de conservation est fixée, une tâche quotidienne **retire le fichier** des
versions obsolètes trop anciennes — la fiche l'annonce par « Retiré (conservation échue) ». La
version, son **empreinte** et son ancrage restent : un auditeur peut toujours constater ce qui a
été approuvé, même si les octets ne sont plus là.

## Le cas des revues ouvertes avant la signature désignée

Une version déjà en revue sous un ancien **circuit par rôles** va à son terme : elle se décide par
le droit d'approbation et son étape de circuit. Les nouvelles soumissions, elles, passent toutes
par les signataires désignés. Un circuit peut par ailleurs être **suspendu** par l'administrateur,
sans perdre ses étapes ; il s'applique alors en approbation simple.

## Bonnes pratiques

- **Désignez les signataires qui ont l'autorité métier**, pas ceux qui ont le rôle logiciel : la
  plateforme reconnaît la désignation.
- **Motivez un rejet comme on motive un écart** : la version repart au rang 1, et le motif est la
  seule chose que le rédacteur aura pour corriger.
- **Tenez les revues périodiques** : la vue « Revues à échéance » et le statut « Revue dépassée »
  sont ce que l'auditeur regarde en premier.
- **Rattachez chaque document à son processus** : c'est ce rattachement qui permet au
  [Standards Hub](standards-hub.md) de compter le document comme preuve d'une clause.
