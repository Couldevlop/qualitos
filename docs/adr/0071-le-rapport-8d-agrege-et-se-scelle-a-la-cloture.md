# ADR 0071 — Le rapport 8D agrège ce qui existe, et se scelle à la clôture

- **Statut** : Accepté
- **Date** : 2026-09-13
- **Owners** : @Couldevlop
- **Portée** : Module Non-conformités (rapport 8D : agrégation, saisies, scellement,
  rendu PDF, vérification publique)
- **Honore** : la fiche différée `docs/backlog/8d-rapport-cloture-nc.md`, supprimée par
  ce lot

## Contexte

À la clôture d'une non-conformité, le client automobile — et, de plus en plus, le
client tout court — demande un **rapport 8D** : huit disciplines, de la constitution
de l'équipe à la reconnaissance de son travail. C'est le format que l'IATF 16949
attend, et celui qu'un auditeur sait lire sans explication.

Le dossier existait déjà dans QualitOS, éparpillé : la non-conformité décrit le
problème, les Ishikawa et les 5 pourquoi rattachés tiennent la cause, la CAPA
escaladée porte les actions et leurs preuves, le PFMEA et les plans de surveillance
disent ce qui empêche la récidive. Il manquait le document, et trois disciplines que
personne ne sait renseigner à notre place.

Trois questions étaient ouvertes dans la fiche différée : document figé ou vue
recalculée, que faire des trois disciplines sans source, et sur quelles NC.

## Décisions

### 1. Le rapport AGRÈGE, il ne recopie pas

Cinq disciplines sur huit n'ont **aucune colonne** : D2 relit la non-conformité,
D4 les Ishikawa et les 5 pourquoi rattachés plus la cause racine saisie sur la NC,
D5 et D6 la CAPA escaladée avec ses actions et ses preuves, D7 le mode de défaillance
PFMEA désigné par la NC et les plans de surveillance du produit.

C'est le parti du dossier PPAP du cycle APQP (ADR 0068 §8), pour la même raison :
tenir une seconde liste, c'est garantir qu'elle divergera. Un 8D dont les actions
correctives auraient été recopiées depuis la CAPA afficherait, six mois plus tard,
des actions que la CAPA ne connaît plus.

L'agrégation passe par **un seul port** (`EightDSourcePort`) et non par six
dépendances directes : le cas d'usage a besoin de « l'état du dossier », pas de savoir
que l'Ishikawa se lit par `ncId` quand les 5 pourquoi se lisent par association JPA.
C'est ce qui le rend testable sans base — et un garde-fou ArchUnit l'impose.

**Alternative écartée : une table de rapport portant les huit disciplines.** Plus
simple à écrire, et c'est ce qu'on trouve chez les concurrents. Mais le rapport
serait devenu une saisie de plus, à tenir à jour à la main, et le lien avec le travail
réellement fait dans les autres modules se serait perdu — c'est-à-dire exactement ce
que §3.6 refuse.

### 2. Trois disciplines se saisissent, et le rapport se déclare **partiel**

D1 (équipe), D3 (endiguement) et D8 (reconnaissance) n'ont aucune source, et aucun
module n'en aura : ce sont des faits que seul un humain connaît. Elles ont donc trois
colonnes, et trois champs à l'écran.

Elles sont **facultatives**. Un rapport auquel manque la reconnaissance de l'équipe
s'émet quand même, et se déclare `partiel`, avec la liste des disciplines vides — en
tête du PDF, pas en note de bas de page.

**Alternative écartée : exiger les trois avant d'émettre.** C'était tentant pour la
qualité du document. Mais cela allonge le geste de clôture jusqu'à le faire
contourner, et surtout : un document aux cases remplies à la hâte **ment plus** qu'un
document qui dit ce qui lui manque. Une discipline sans contenu est donc affichée avec
la raison de son absence — y compris dans le PDF (« Non renseigne. » et le libellé de
source en rouge) — jamais comme un bloc vide et muet.

Le cas le plus éclairant est D7 : le Poka-Yoke, que la fiche différée citait comme
source, **ne se rattache aujourd'hui qu'à un projet DMAIC**, jamais à une
non-conformité. Le libellé le dit explicitement plutôt que de le taire : taire
l'absence laisserait croire qu'on a cherché sans rien trouver.

### 3. Un document FIGÉ, et le figé est un **instantané de texte**

À l'émission, le texte des huit disciplines est sérialisé dans `snapshot_json`, et le
rapport ne relit plus ses sources. Une vue recalculée changerait après coup — une
CAPA rouverte, un Ishikawa complété, un plan de surveillance révisé — et un 8D qui
change n'est plus un 8D : ce que le client a lu fait foi, pas ce que la base dit
aujourd'hui.

L'instantané stocke du **texte déjà mis en forme**, dates comprises, et non des
identifiants à résoudre. C'est la leçon de l'ADR 0062 poussée un cran plus loin : une
empreinte ne se calcule que sur des valeurs stockables, et le rendu doit être une
**fonction pure** de ce qu'on stocke.

### 4. L'empreinte porte sur le PDF, et le rendu est déterministe

L'émission rend le PDF, calcule son SHA-256, le signe (Ed25519 + ML-DSA-65, ADR 0011)
et l'ancre — en réutilisant le moteur existant (`HybridSignatureService` +
`BlockchainAnchorPort`), comme le control plan et l'export de dashboard. Le contexte
de signature est propre (`eightd-report`) : une signature de 8D ne doit pas pouvoir
être présentée comme une signature de control plan, alors même que les deux signent
un SHA-256 de 64 caractères.

Le rendu PDFBox est **déterministe**, et trois précautions y veillent : aucune lecture
d'horloge, métadonnées de document figées, et `/ID` du trailer **dérivé du contenu** —
PDFBox en tire sinon un nombre aléatoire à chaque enregistrement, seule différence
entre deux rendus du même rapport, mesurée au banc avant d'être corrigée.

Au téléchargement, le PDF est **re-rendu** depuis l'instantané et son empreinte
**comparée** à celle qui a été scellée. Si elles divergent — une évolution du rendu,
par exemple — le document n'est pas remis, et le refus le dit (409). Remettre un
fichier que la signature ne couvre plus serait pire que ne rien remettre.

**Alternative écartée : stocker les octets du PDF en stockage objet.** Le figé aurait
été total, sans contrainte de déterminisme. Mais le rapport serait devenu indisponible
quand le stockage objet est coupé (il l'est par défaut, cf. `StorageDisabledException`),
il aurait fallu l'inscrire au balayage des binaires orphelins (ADR 0056), et une
évolution du rendu serait passée inaperçue au lieu d'être signalée.

### 5. Émettre est réservé au pilotage qualité, et exige un second facteur

Lire le rapport — le PDF compris — est ouvert à tout authentifié : l'opérateur qui a
signalé le défaut et l'auditeur interne doivent pouvoir lire la suite qu'on a donnée à
l'écart, sans habilitation d'écriture.

Renseigner et émettre sont réservés à
`QUALITY_MANAGER / DIRECTOR_QUALITY / ADMIN_TENANT / SUPER_ADMIN`, la liste des autres
référentiels de méthode du module. L'autorisation est portée par `@PreAuthorize` sur la
méthode, donc évaluée **avant** la liaison du corps (ADR 0065) — un banc le vérifie
avec un corps illisible et un rôle insuffisant, et attend 403, pas 400.

L'émission passe en plus par `StepUpGuard` : elle produit une preuve opposable, et
§18.2 #5 ne laisse pas le choix. Appel explicite dans le corps de la méthode, comme
pour l'approbation d'un control plan, et non annotation maison qu'un intercepteur mal
configuré désactiverait en silence.

### 6. La clôture est la condition, et le service la tient

`POST /8d/issue` refuse (409) tant que la non-conformité n'est pas `CLOSED`, et refuse
une seconde émission. Un 8D émis sur un écart encore ouvert affirmerait que le problème
est réglé alors qu'il court ; deux émissions produiraient deux documents de même
référence et d'empreintes différentes, dont aucun ne serait opposable.

**Alternative écartée : émettre automatiquement à la clôture** (la fiche différée
évoquait l'accroche dans `NcService.close`). Un 8D pour chaque écart mineur serait une
charge inutile, et surtout un document émis sans ses trois saisies — donc partiel —
pour la totalité des NC. Le rapport se prépare au fil du traitement et s'émet **sur
demande**.

### 7. Le QR code mène à une vérification publique

Le PDF porte un QR code vers `GET /api/v1/nc/public/8d/{code}/verify`, ouvert en
`permitAll` comme les certificats de formation et les exports de dashboard : un QR
imprimé sur un document remis au client ne peut pas demander de s'authentifier. La
réponse ne rend que des faits d'intégrité, et un code inconnu répond `valid=false` et
non 404, pour que la route ne serve pas à énumérer. Un banc dédié charge la
`SecurityConfig` réelle : si la règle disparaissait d'un remaniement, tous les PDF déjà
émis deviendraient invérifiables sans que rien ne le signale.

## Conséquences

- Migration **V130** : `nc_eightd_reports`, une ligne par (tenant, NC), avec une
  contrainte qui impose que `ISSUED` porte les six champs de preuve — une empreinte
  sans signature, ou une signature sans ancrage, n'est pas une demi-preuve, c'est
  l'absence de preuve. La base le tient, pas seulement l'agrégat.
- Le module suit la découpe hexagonale (`domain` / `application` / `infrastructure` /
  `web`) avec deux règles ArchUnit : l'instantané ne dépend ni de PDFBox, ni de
  Jackson, ni de JPA ; le cas d'usage ne touche aucun des cinq modules qu'il agrège.
- Le nouveau geste est journalisé (`EIGHTD_REPORT_ISSUED`) au journal chaîné du tenant,
  acteur lu du jeton.
- Front : un écran `/nc/:id/8d`, ouvert depuis la fiche de la NC — pas d'entrée de
  menu, comme l'Ishikawa et les 5 pourquoi, puisque le rapport part d'un écart déjà
  constaté.
- ⚠ Dette assumée : **le Poka-Yoke n'alimente pas D7**, faute de lien entre un
  dispositif et une non-conformité. Le rapport le dit ; le jour où ce lien existera,
  seul l'adaptateur de sources changera.
- ⚠ Dette assumée : le texte de l'instantané est produit en **français** par le serveur
  et ne suit pas la langue de l'interface. C'est voulu pour un document émis — il doit
  dire dans dix ans ce qu'il disait le jour de la clôture — mais un tenant anglophone
  recevra un PDF français. La réponse, le jour où elle sera demandée, est de figer la
  langue **à l'émission** et de la stocker avec l'instantané, pas de traduire à la
  lecture (même frontière que l'ADR 0070).

## Tests d'invariant

- `PdfBoxEightDRenderAdapterTest` : deux rendus du même rapport donnent **exactement**
  les mêmes octets ; une discipline sans source est écrite dans le PDF.
- `EightDServiceTest` : l'émission avant clôture et la seconde émission sont refusées,
  rien n'est signé ni journalisé dans ces cas ; le contenu émis ne suit plus les
  sources qui changent après coup ; un rendu qui ne retombe pas sur l'empreinte scellée
  n'est pas remis ; un rapport d'un autre tenant reste introuvable.
- `EightDControllerTest` : lire est ouvert, écrire ne l'est pas, le refus précède la
  lecture du corps, et l'émission sans second facteur ne touche pas au service.
- `EightDPublicPathTest` : la vérification répond sans jeton.
- `HexagonalArchitectureTest` : les deux règles du §Conséquences.

## Références

- `CLAUDE.md` §3.6 (référentiel transverse), §4.2/§4.3, §11.3/§11.4, §16, §18.2
- ADR 0011 (signature hybride), 0012 (ancrage Phase A), 0056 (binaires orphelins),
  0062 (empreintes sur valeurs stockables), 0065 (autorisation avant le corps),
  0068 (le dossier PPAP agrège), 0070 (ce que le serveur fige ne se traduit pas)
- `docs/D8_Pump_Leakage_Analysis.pdf` — gabarit de rendu
