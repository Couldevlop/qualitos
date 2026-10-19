# ADR 0094 — La GED stocke chaque fichier une fois, compressé s'il y gagne

- **Statut** : Accepté
- **Date** : 2026-10-19
- **Owners** : @Couldevlop
- **Portée** : `api-quality-engine` (`docs/*`, `nonconformity/storage/ObjectStorage`),
  migration `V143`, `apps/web` (`features/documents`, `features/admin/pages/document-settings`),
  chart Helm (`ingress-document-files.yaml`, CSP des valeurs), `apps/web/nginx.conf`
- **S'inscrit dans** : CLAUDE.md §4.1 (Document Control), §11.3 (empreintes ancrées),
  §18.2.2 (client lu dans le jeton), ADR 0080 (circuits de validation),
  ADR 0086 (RLS), ADR 0092 (une personne est un utilisateur de l'annuaire)

## Contexte

Le menu « Documents » n'était qu'une liste plate de fiches : pas de fichier
joint, pas d'arborescence, pas de vue de travail. La maquette « Documentation
(GED) » décrit l'écran attendu : des vues rapides (tous, mes documents, à
valider par moi, revues à échéance, archives) et une arborescence à gauche, la
liste filtrable au centre, la fiche du document choisi à droite avec l'aperçu,
le circuit et l'historique des versions.

Joindre le fichier lui-même change l'échelle. Une organisation de quelques
milliers de personnes accumule des dizaines de milliers de fichiers, chacun
recopié de version en version, souvent déposé deux fois (le même modèle dans
deux dossiers), et rien n'est jamais supprimé. Sans stratégie, le stockage
croît sans borne et le serveur relaie des centaines de mégaoctets par jour.

## Décisions

### 1. Un fichier est stocké une fois par client (déduplication par contenu)

Chaque fichier déposé est identifié par le SHA-256 de ses octets d'origine.
`document_blobs` porte une contrainte unique `(tenant_id, sha256)` : un second
dépôt du même contenu, dans n'importe quel document ou version, réutilise
l'objet existant. Une nouvelle version hérite du fichier de la précédente
(même ligne de `document_blobs`) jusqu'à ce qu'on le remplace. La
déduplication ne traverse jamais deux clients : la RLS (ADR 0086) s'applique à
`document_blobs` comme aux autres tables.

La clé d'objet est `tenants/{client}/documents/blobs/{sha256}-{8 aléatoires}` :
le suffixe évite qu'une suppression différée d'un objet orphelin efface un
dépôt simultané du même contenu.

### 2. Compressé seulement si le gain le justifie

Les formats déjà compressés (docx, xlsx, pptx, JPEG, PNG, WebP) sont stockés
tels quels. Les autres (PDF, formats bureautiques anciens, texte, CSV, XML)
sont compressés en gzip, et la version compressée n'est gardée que si elle
gagne au moins 10 %. L'objet porte alors `Content-Encoding: gzip` : le
navigateur décompresse lui-même, le serveur ne relit jamais le fichier.

L'empreinte reste celle des octets d'origine : c'est elle qu'on ancre et
qu'un auditeur recalcule sur le fichier téléchargé.

### 3. Le serveur ne relaie pas les fichiers

La lecture passe par un lien présigné valable dix minutes, avec le nom
d'origine dans `response-content-disposition` (téléchargement ou affichage).
L'aperçu (PDF, images, texte) lit ce lien depuis le navigateur et l'affiche à
partir d'un `blob:` local, sous le type déclaré au dépôt. Le stockage reste
servi inerte (ingress du stockage objet : `sandbox`, `nosniff`, `no-referrer`).

### 4. Les limites sont celles du client, sous un plafond de plateforme

`document_settings` (une ligne par client, réglée par l'administrateur du
client) fixe la taille maximale d'un fichier (25 Mo par défaut, plafond de
plateforme 50 Mo), un quota facultatif (mesuré sur l'espace réellement
occupé, après déduplication et compression), la périodicité des revues
(24 mois) et leur préavis (30 jours). Le type est vérifié sur une liste
fermée et à la signature binaire du contenu ; un dépôt refusé l'est avec un
400 ou un 413 explicite.

### 5. Les versions obsolètes perdent leur fichier, pas leur empreinte

Si l'administrateur fixe une durée de conservation, une tâche quotidienne
retire le fichier des versions obsolètes depuis plus longtemps : la version,
son empreinte et son ancrage restent, l'objet n'est supprimé que si plus
aucune version ne le référence. La suppression de l'objet n'a lieu qu'après
la validation de la transaction, et le balayage des orphelins connaît les
objets de la GED.

### 6. Le dépôt a sa propre route

Les dépôts passent par `POST /api/v1/document-files/{document}/versions/{version}`,
servi par un ingress à part : lui seul accepte des corps de 52 Mo, et le
pare-feu n'y lit pas le corps (un binaire n'a pas de champ à inspecter ; le
type est vérifié par l'application). L'ingress principal garde sa limite de
12 Mo et toutes ses règles.

### 7. Arborescence, processus et statut affiché

- L'arborescence (4 niveaux au plus) et la liste des processus sont propres à
  chaque client, créées au premier accès à partir de la trame de la maquette,
  puis réglées par l'administrateur. Un dossier qui contient des sous-dossiers
  ou des documents, un processus rattaché à des documents, ne se suppriment pas.
- Le document porte un cycle de vie (`DRAFT`, `IN_FORCE`, `IN_REVISION`) et une
  date de prochaine revue, fixée à la publication. Le statut affiché ajoute
  « Revue dépassée » (date passée) et « Archivé ».
- « À valider par moi » liste les versions en revue dont l'étape de circuit en
  cours revient à l'un des rôles de l'utilisateur et sur lesquelles il n'a pas
  encore décidé ; sans circuit, celles qu'il a le droit d'approuver. Jamais ses
  propres versions.

## Justification

- La déduplication par contenu est le levier le plus fort pour une GED : les
  versions successives qui ne changent pas de fichier, les modèles copiés
  d'un dossier à l'autre ne coûtent plus rien.
- Recompresser du docx ou du JPEG coûte du calcul pour rien ; le seuil de
  10 % évite de payer la décompression pour un gain marginal.
- Les liens présignés retirent les fichiers du chemin de l'API : ni mémoire ni
  bande passante côté serveur, et un lien expiré ne vaut plus rien.
- Garder l'empreinte après la purge préserve la preuve (§11.3) sans garder
  indéfiniment des octets que plus personne ne consulte.

## Conséquences

- Remplacer le fichier d'un brouillon libère l'ancien objet s'il n'est plus
  référencé ; un fichier partagé par plusieurs versions survit tant qu'une
  version le porte.
- L'aperçu ne couvre que PDF, images et texte ; les formats bureautiques se
  téléchargent (pas de conversion côté serveur).
- La CSP de l'application autorise `blob:` en `img-src`, `frame-src` et
  `object-src`, pour l'aperçu seulement : aucune origine extérieure n'y entre.
- Une installation dont le contrôleur refuse les annotations « snippet »
  (ADR 0083) garde la limite de corps de 52 Mo mais pas l'exclusion du
  pare-feu : les fichiers de plus de 13 Mo environ y seront refusés par le
  pare-feu tant qu'il n'est pas réglé.

## Tests d'invariant

- `DocumentFilesTest` : liste fermée des types, contrôle de signature, choix de
  la compression et seuil de gain, empreinte sur les octets d'origine.
- `DocumentFileServiceTest` : déduplication, quota, remplacement et libération,
  suppression après validation, purge des obsolètes.
- `DocumentCatalogServiceTest` : profondeur maximale, refus des cycles (y
  compris un dossier placé sous lui-même), suppressions refusées.
- `GedIntegrationTest` : de bout en bout sur la base de test, création de la
  trame, filtres, « À valider par moi », cycle de vie, déduplication et purge.

## Références

- Maquette `docs/Documentation (GED).pptx`
- CLAUDE.md §4.1, §11.3, §18.2
- ADR 0080, 0083, 0086, 0092
