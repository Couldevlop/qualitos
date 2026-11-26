# 0142 — Le rapport 8D se remet à un client, et son dessin se versionne

- **Statut** : accepté
- **Date** : 2026-11-26
- **Décideurs** : direction qualité, architecte principal
- **Concerne** : `nonconformity/eightd` (moteur qualité), écran du rapport 8D (SPA), annuaire (`api-core`)
- **Complète** : ADR 0139 (le rapport montre ce que la plateforme sait), ADR 0141 (le rapport se relit avant de se figer)

## Contexte

La liste de recette du 8D décrit, discipline par discipline, ce que le document
doit porter. Le rapport généré s'en écartait sur sept points, et la relecture du
PDF réel en a révélé six autres que la liste ne nomme pas, parce qu'ils ne se
voient qu'une fois le document imprimé :

- l'**équipe** et les **responsables d'action** s'affichaient par leur adresse de
  courriel — une adresse personnelle dans un document qui sort de la maison, là
  où le lecteur attend un nom ;
- le **D2** alignait sept phrases (« Référence : … », « Gravité : … ») que l'œil
  devait lire par leur début pour y trouver une valeur ;
- chaque action ouvrant son propre dossier CAPA, le **D5** dessinait un tableau
  d'une seule ligne par dossier, chacun titré du même texte que sa ligne ;
- le **D6** écrivait « Aucune pièce jointe » sous chaque action, et se lisait
  comme un relevé de manques ;
- les **arêtes de l'Ishikawa** portaient les noms du modèle — `ENVIRONMENT`,
  `MANPOWER`, `MEASUREMENTS` — sous un écran qui dit « Milieu »,
  « Main-d'œuvre », « Mesures » ;
- le **sous-titre** promettait « signé ML-DSA + ancré blockchain » une ligne
  au-dessus de « BROUILLON […] ni signé, ni ancré » ;
- le **pied de page** imprimait `Tenant: fd3c738c-e111-…`, identifiant interne
  sans valeur pour son lecteur ;
- la **référence** s'écrivait trois fois sur le même écran, dont une collée au
  titre (« Rapport 8DInt-NC-2026-0005ue ») ;
- plusieurs phrases fixes étaient restées **sans accents** (« non emis », « ni
  signe, ni ancre ») ;
- le document n'était **pas paginé**, alors qu'il sert de preuve et circule
  sur papier ;
- la colonne de l'intitulé d'action prenait **15 %** de la laisse, parts héritées
  du temps où la deuxième colonne était la nature de l'action : l'intitulé se
  repliait sur trois lignes pendant que trois dates flottaient dans du blanc.

## La contrainte qui commande tout

Un rapport émis n'est pas stocké : il est **re-rendu depuis son instantané** à
chaque téléchargement, et son empreinte recalculée est comparée au sceau. Un
écart fait refuser la remise.

Il s'ensuit que les changements de **contenu** sont sans danger — un instantané
ancien ne porte pas la fiche, la liste est vide, le dessin ne change pas —, mais
que **tout changement de dessin casse les rapports déjà émis**, sans qu'aucune
donnée n'ait bougé, et le fait en silence : le document cesse simplement d'être
délivrable au déploiement suivant.

## Décision

**1. Le dessin du document se versionne.** L'instantané porte un champ `layout`,
additif et acceptant l'absence. `null` désigne le dessin d'origine, que le moteur
de rendu conserve intact pour toujours ; tout rapport neuf porte le nom du dessin
en vigueur. Chaque trait remanié est posé derrière une garde sur ce champ, et
deux bancs interrogent les deux côtés — celui du rapport ancien tombe au premier
trait posé hors d'une garde. Une refonte ultérieure prend un nouveau nom ; on
n'en modifie jamais un existant.

**2. Une personne se montre par son nom.** L'équipe du D1 et le responsable de
chaque action sont résolus dans l'annuaire. Le nom figé à la saisie ne sert que
s'il en est un — une valeur contenant `@` est une adresse, pas un nom — ou si
l'annuaire se tait.

**3. La fonction, c'est le rôle.** Aucun champ « fonction » n'existe : l'invitation
d'un utilisateur ne porte que prénom, nom et rôles. La colonne « Rôle et fonction »
écrit donc le rôle plate-forme en toutes lettres (« Pilote, Responsable qualité »),
seule donnée de cette nature enregistrée à la création. Conséquence assumée :
`DirectoryDto.Entry` expose cette fonction, donc le rôle de chacun devient lisible
par tout membre du client ; la lecture reste limitée à une personne nommée, et
n'est jamais payée sur une page de recherche.

**4. Une discipline peut porter une fiche.** `Section` gagne une liste de couples
libellé/valeur, rendue en deux colonnes alignées par le PDF comme par l'écran.
Elle **s'ajoute** aux lignes de texte au lieu de les remplacer : une discipline
porte l'une ou l'autre selon l'âge du rapport.

**5. Le document ne promet pas ce qu'il n'a pas.** Un brouillon ne mentionne ni
signature ni ancrage. Le pied ne porte aucun identifiant interne. Les pages sont
numérotées.

## Conséquences

- Les rapports déjà émis restent délivrables, à l'octet près.
- Toute refonte future du dessin passe par un nouveau nom de `layout` et une
  garde ; un trait posé sans garde fait tomber le banc du dessin d'origine.
- Le verdict d'efficacité (ADR 0108, ADR 0131) quitte le D6 du document remis au
  client — la liste de recette demande, sous cette discipline, l'intitulé de
  l'action et le nom de ses pièces, et rien d'autre. Il reste lisible sur le
  dossier CAPA et sur le tableau d'efficacité.
- Le jour d'une date figée se lit en UTC et non au fuseau du serveur : deux
  machines scellaient sinon deux dates.
