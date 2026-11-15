# ADR 0120 — La gravité d'un écart se lit dans la norme, pas dans une échelle maison

- **Statut** : Accepté
- **Date** : 2026-11-14
- **Owners** : @Couldevlop
- **Portée** : `apps/api-quality-engine` (`quality/standards` : `FindingGrade`
  (nouveau), `StandardsService`, `CertificationBlancService`,
  `auditblanc/domain/MockAuditClause`, `MockAuditCriticality`) et `apps/web`
  (`features/standards` : fiche de norme, dialogue d'édition de l'arbre ;
  `features/nc` : analyse de cause du 8D). Aucun changement de schéma : la
  colonne `risk_if_missing` reste, avec un autre emploi.
- **Prolonge** : **ADR 0115** (le vocabulaire de la certification remplace
  « critique ») — l'ADR 0115 avait corrigé les **mots** ; celui-ci corrige le
  **critère**.
- **S'inscrit dans** : CLAUDE.md §8 (Standards Hub), §8.7 (moteur
  d'alignement), §22.12 (tout indicateur a une définition formelle)

## Contexte

L'ADR 0115 avait retiré le mot « critique » de l'audit blanc : aucun organisme
certificateur ne l'écrit, et un client qui le lisait préparait une réponse à un
reproche qu'il n'entendrait pas. Les trois termes affichés sont depuis les bons
— non-conformité majeure, non-conformité mineure, observation.

Le **critère**, lui, n'avait pas bougé. Il lisait un champ `risk_if_missing`
saisi sur chaque exigence du catalogue, à quatre niveaux — `LOW`, `MEDIUM`,
`HIGH`, `CRITICAL` — et décidait : exigence obligatoire à risque élevé ou
critique → majeure ; obligatoire autrement → mineure. Trois défauts, et le
premier emporte les autres.

1. **Aucune norme ne publie cette échelle.** Elle a été inventée, puis saisie à
   la main sur les exigences du catalogue, et elle rendait un verdict normatif.
2. **La gravité n'est pas une propriété de la clause.** Elle dépend de ce que
   l'auditeur trouve en face d'elle le jour de l'audit. Un client détenant la
   moitié des preuves d'une clause y lisait « majeure » ; un autre, sans aucune
   preuve sur une clause étiquetée « risque faible », y lisait « mineure » —
   c'est l'erreur coûteuse, celle qui laisse partir en audit sans voir le
   blocage.
3. **La règle existait en trois copies** : `StandardsService.severityOf`,
   `CertificationBlancService.ncType` et `MockAuditClause.criticality`. L'onglet
   d'audit blanc et le rapport de certification à blanc pouvaient donc classer
   différemment le même écart.

À l'écran, deux étiquettes ajoutaient au malentendu : la ligne d'exigence
affichait le code brut `MUST` — traduit dans aucune langue — et, à côté,
`CRITICAL` sur fond rouge. Le tableau des écarts, lui, nommait les trois degrés
sans jamais dire ce qui les sépare.

## Ce que disent les textes

- **ISO/IEC 17021-1:2015, 3.12 et 3.13.** Une non-conformité **majeure** met en
  cause l'aptitude du système de management à atteindre les résultats escomptés ;
  une **mineure** ne la met pas en cause. Deux degrés, pas trois ni quatre.
- **ISO 19011:2018, 6.4.8.** Les constats *peuvent* être gradués selon le
  contexte et les risques, qualitativement ou quantitativement. C'est une
  permission, et elle ne fournit aucun barème : le barème est à justifier. La
  même norme, en 6.3, demande un **programme** d'audit établi par les risques.
- **Directives ISO/IEC, Partie 2.** « Doit » (*shall*) énonce une exigence,
  « devrait » (*should*) une recommandation, « peut » (*may*) une permission.
  Seule la première peut être manquée.
- **ISO 9001:2015, 10.2.1.** Elle ne gradue pas : elle demande une action
  « appropriée aux effets » du manquement. De la proportionnalité, pas des
  niveaux.

## Décision

**Un seul value object classe les écarts**, `FindingGrade`, et il ne se fonde
que sur deux faits vérifiables :

1. **le verbe de la norme** sur l'exigence — ce qui est recommandé ne peut pas
   être manqué : c'est une **observation** ;
2. **l'état des preuves de la clause** — aucune preuve dans toute la clause : la
   disposition demandée est absente, l'aptitude du système est en cause,
   **majeure** ; des preuves ailleurs dans la clause : manquement isolé,
   **mineure**.

La **clause** est la maille, pour trois raisons : c'est l'unité que la norme
donne à une disposition, c'est celle sur laquelle un certificateur conclut, et
elle porte sans seuil arbitraire le critère d'accumulation des organismes
accrédités — des manquements mineurs assez nombreux sur une même disposition
valent une majeure : quand ils portent sur *toutes* les exigences de la clause,
la clause est sans preuve, et la règle rend déjà « majeure ».

**`risk_if_missing` ne classe plus rien.** Il garde son seul emploi fondé :
ordonner le programme d'audit (ISO 19011, 6.3). Il s'appelle désormais « ordre
de vérification en audit » à l'écran, ses valeurs se lisent « à vérifier en
premier / à vérifier / à vérifier en dernier », et son étiquette n'est plus
rouge — un rouge d'alarme disait le contraire du libellé, et c'est le rouge que
l'œil retenait. La colonne reste en base : la supprimer coûterait une migration
sur les 57 normes du catalogue pour une donnée qui redevient utile dès qu'elle
est appelée par son nom.

**Les trois degrés sont définis là où on les lit.** Une légende au-dessus du
tableau des écarts donne, pour chacun, le critère et sa source. Un client qui
prépare un audit a besoin du critère, pas du mot seul.

**Le verbe de la norme se lit en clair** : « Exigée », « Recommandée »,
« Facultative », avec au survol ce que le verbe engage — sur la fiche comme dans
le dialogue d'édition de l'arbre, où l'éditeur du catalogue choisissait un code
sans lire ce qu'il engageait.

## Conséquence sur le 8D : un seul D pour deux outils

Question voisine, traitée ici parce qu'elle relève de la même exigence — suivre
la méthode plutôt que l'arranger. L'Ishikawa et les 5 Pourquoi sont deux outils
distincts ; ils n'ont pourtant **pas de discipline propre** dans le 8D. La
méthode de Ford place en **D4** la recherche *et* la vérification de la cause
racine, et les deux outils y travaillent — l'Ishikawa énumère les causes
possibles, les 5 Pourquoi creusent une branche jusqu'à la cause racine et la
vérifient.

Ils gardent donc leur D commun, mais **chacun sa case** dans le D4, dans l'ordre
de la méthode (l'éventail, puis la branche creusée), avec son titre et sa figure
— et rien de plus : la figure porte déjà ce qu'une phrase redirait.

## Conséquences

- Un écart reçoit **un seul** classement dans toute la plateforme, et il est
  justifiable devant un auditeur.
- Une clause obligatoire sans aucune preuve devient **majeure**, quel que soit
  le risque étiqueté : des dossiers jugés « certifiables sous réserve »
  passeront « non certifiables ». C'est l'effet voulu — un verdict trop clément
  avant un audit coûte plus cher qu'un verdict sévère.
- Inversement, une clause démontrée en partie ne produit plus de majeure : le
  catalogue n'a plus à porter d'étiquette de gravité juste.
- Le document émis (PDF signé, ancré) n'est pas touché : il porte le texte figé
  à l'émission.

## Alternatives écartées

- **Garder l'échelle à quatre niveaux en la documentant.** ISO 19011 l'autorise.
  Écartée : documenter une échelle ne la fonde pas, et elle resterait une
  prédiction de gravité posée avant d'avoir regardé la moindre preuve.
- **Supprimer la colonne `risk_if_missing`.** Écartée : une migration sur les 57
  normes du catalogue pour une donnée qui redevient légitime sous son vrai nom.
- **Un seuil d'accumulation chiffré** (« trois mineures sur une clause valent une
  majeure »). Écartée : le chiffre serait inventé, et le cas qu'il visait est
  déjà couvert — toutes les exigences manquées, c'est une clause sans preuve.

## Vérification

- `FindingGradeTest` : les deux degrés sur leurs définitions, l'observation qui
  n'est pas une non-conformité, l'ordre de traitement, et l'absence d'un
  quatrième degré.
- `MockAuditClauseTest` : à état de preuve égal, les quatre niveaux de risque
  rendent le **même** constat — c'est la régression gardée.
- `StandardsServiceTest` : clause sans preuve → majeures ; même clause, une
  preuve posée → mineure.
- `CertificationBlancServiceTest` : la certification à blanc compte et décide,
  elle ne reclasse plus.
- `standards-detail.component.spec.ts` : le verbe en clair, l'ordre de
  vérification nommé pour ce qu'il est, la légende des trois degrés.
- `nc-detail.cause-analysis.spec.ts` : l'Ishikawa puis les 5 Pourquoi, chacun
  dans sa case, titre et figure.
