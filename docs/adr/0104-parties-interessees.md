# ADR 0104 — Le registre des parties intéressées : criticité au maximum, pertinence justifiée, et une IA qui propose sans écrire

- **Statut** : Accepté
- **Date** : 2026-11-04
- **Owners** : @Couldevlop
- **Portée** : api-quality-engine (`quality.stakeholders` — `Stakeholder`,
  `StakeholderRequirement`, `StakeholderService`, `StakeholderController`,
  `AiStakeholderAssistant`, `CapaStakeholderGateway`,
  `Permission.STAKEHOLDER_MANAGE`), web (`/stakeholders` — registre, fiche en
  tiroir, matrice d'évaluation, assistant IA, et l'entrée de navigation du
  Pilotage)
- **Complète** : ADR 0088 (LA fenêtre de création CAPA), ADR 0091 (une personne
  se choisit dans l'annuaire), ADR 0077 (tableau de bord SMI — même lecture
  transverse du système), ADR 0102 (routage IA par client)

## Contexte

La norme ISO 9001 demande au §4.2 de **déterminer** les parties intéressées
pertinentes et leurs exigences, puis de **surveiller** cette détermination. La
plateforme couvrait les risques (§6.1), les processus, les audits, les écarts —
mais pas l'amont : *qui* attend quoi de l'organisme. Faute de registre, la
réponse vivait dans un tableur, hors du système, et aucune exigence client ou
réglementaire ne pouvait être rattachée à un risque, à un écart ou à un
document.

Trois questions se posaient, chacune avec une réponse facile et fausse.

**1. Comment agréger deux cotations en une criticité ?** Une partie intéressée
pèse sur la qualité, et elle pèse au titre de la réglementation. La moyenne est
le réflexe — et elle noie précisément le cas qui compte : une autorité de
tutelle à impact réglementaire élevé mais à impact qualité faible ressortirait
« moyenne », rangée au milieu du tableau, alors que son non-respect ferme
l'établissement.

**2. Faut-il justifier la pertinence ?** Écarter une partie intéressée est une
décision de périmètre. La laisser s'écarter d'un simple clic revient à produire
un registre que personne ne peut défendre un an plus tard, et c'est exactement
ce qu'un auditeur vient vérifier : le périmètre a-t-il été *déterminé*, ou
*subi* ?

**3. Que fait l'IA ?** Le texte d'un contrat se prête parfaitement à
l'extraction d'exigences, et la description d'un secteur à la suggestion de
parties intéressées. La tentation est d'inscrire directement le résultat : un
registre rempli en trois secondes. Il serait rempli de lignes que personne
n'assume, et un modèle qui se trompe de clause laisserait une exigence
imaginaire opposable à l'organisme.

## Décision

**1. La criticité est le MAXIMUM des deux impacts, et elle n'est pas stockée.**
`Stakeholder.criticality()` rend `ImpactLevel.worst(qualityImpact,
regulatoryImpact)`. Les deux cotations ne se compensent pas : la plus exigeante
commande. Elle est **calculée à la lecture** et jamais persistée, pour qu'un
impact modifié ne laisse derrière lui aucune criticité périmée. L'écran
l'affiche en lecture seule — la matrice d'évaluation ne propose que les deux
notes, et dit explicitement qu'une ligne recotée verra sa criticité recalculée
à l'enregistrement.

**2. Trancher la pertinence exige une justification, dans les deux sens.**
`Stakeholder.analyse` refuse `RELEVANT` comme `NOT_RELEVANT` sans
`relevanceJustification`. `TO_DETERMINE` reste un état plein et sans
justification : « pas encore tranché » est une information honnête, et le
registre la compte dans ses tuiles. Les fiches écrites avant cette règle ne sont
pas réécrites (`restoreAnalysis` repose l'état lu en base sans rejouer les
règles de saisie) : elles se corrigent à leur prochaine modification.

**3. Ouvrir une CAPA sur une exigence la repasse « à réévaluer ».**
`StakeholderRequirement.withCapa` force `TO_REASSESS`. Ouvrir une action, c'est
constater un écart : une exigence ne peut pas rester affichée « maîtrisée »
pendant qu'une action corrige ce qui la met en défaut. Le registre compte ces
exigences, et la revue de direction les lit.

**4. L'IA propose, elle n'écrit pas.** Les trois appels d'assistance —
suggestions de parties intéressées, extraction d'exigences d'un document,
détection de changements entre deux versions — sont en lecture : ils rendent des
**propositions** et ne touchent pas au registre. Inscrire une suggestion,
ajouter une exigence extraite et marquer des exigences à réévaluer sont trois
appels distincts, soumis à `STAKEHOLDER_MANAGE`, déclenchés un par un par un
humain. Chaque proposition est accompagnée de ce qui la rend vérifiable : la
justification pour une suggestion, la **citation du passage** pour une exigence
extraite. Sans la citation, rien ne distingue une exigence lue dans le contrat
d'une exigence inventée.

**5. Une seule adresse, trois vues.** Le registre, la matrice et l'assistant
parlent des mêmes fiches : trois onglets sur `/stakeholders`, et la fiche dans
un tiroir par-dessus la liste. Trois routes auraient rechargé la liste trois
fois et laissé trois états divergents à l'écran. La cotation se fait **en lot** :
on parcourt le registre, on ajuste plusieurs lignes, on enregistre une fois — et
seules les lignes réellement modifiées partent.

## Alternatives écartées

- **Criticité = moyenne pondérée des deux impacts.** Paramétrable par client,
  elle ferait de la comparaison entre sites une discussion sur les poids, et
  masquerait le cas de l'autorité de tutelle décrit plus haut.
- **Criticité saisie à la main.** Elle se périme au premier impact modifié, et
  rien dans l'écran ne dirait qu'elle est devenue fausse.
- **Pertinence sans justification, avec un commentaire facultatif.** Un champ
  facultatif reste vide ; la décision serait introuvable à l'audit.
- **Un assistant qui inscrit directement ce qu'il trouve.** Gain de quelques
  secondes contre un registre non assumé : le rapport est mauvais, et la
  réglementation sur l'IA (AI Act, CLAUDE.md §12.3) demande de toute façon la
  supervision humaine sur un effet décisionnel.
- **Une route par vue (`/stakeholders/matrice`, `/stakeholders/assistant`).**
  Trois chargements de la même liste, trois états à tenir d'accord.

## Conséquences

- ✅ Les exigences des parties intéressées entrent dans le référentiel commun :
  une exigence se rattache à un risque, un écart, un audit ou un document, et
  peut ouvrir sa CAPA.
- ✅ La criticité ne peut pas être périmée ni manipulée : elle n'existe qu'en
  lecture, dérivée des deux cotations.
- ✅ Le registre est défendable à l'audit : chaque décision de pertinence porte
  son motif, chaque exigence extraite porte sa citation.
- ✅ La lecture reste ouverte à tout authentifié : chacun peut savoir à quoi
  l'organisme s'est engagé. Seule l'écriture demande `STAKEHOLDER_MANAGE`.
- ⚠ La cotation en lot abandonne les notes en attente quand le registre est
  rechargé — un filtre changé, une sauvegarde faite par un collègue. C'est
  délibéré : réappliquer des notes posées sur des valeurs périmées écraserait
  silencieusement le travail d'un autre.
- ⚠ Le côté interne / externe d'une suggestion est **proposé** d'après sa
  catégorie et reste modifiable avant inscription : une filiale et un
  sous-traitant ne se rangent pas du même côté selon l'organisme.
- ⚠ Les textes d'une exigence extraite sont bornés à la longueur des champs du
  serveur avant l'envoi. Une citation très longue est donc tronquée ; la fiche
  garde la référence du passage pour y remonter.

## Tests d'invariant

- `HexagonalArchitectureTest` : le domaine `quality.stakeholders.domain` ne
  dépend ni de Spring, ni de JPA, ni de Jackson — la criticité, la justification
  de pertinence et les règles d'exigence se testent sans base ; et l'application
  ne touche les autres modules que par un port.
- Domaine : la criticité d'une fiche cotée (élevé, faible) vaut élevé ;
  `analyse` refuse `RELEVANT` et `NOT_RELEVANT` sans justification et accepte
  `TO_DETERMINE` sans ; `withCapa` rend une exigence `TO_REASSESS`.
- Web : les trois appels d'assistance ne modifient rien ; inscrire, ajouter et
  marquer exigent `STAKEHOLDER_MANAGE`.
- Front (`stakeholder-matrix.component.spec.ts`) : seules les lignes recotées
  sont envoyées ; une note ramenée à sa valeur d'origine quitte le lot ; en
  lecture seule, aucun sélecteur et aucun appel.
- Front (`stakeholder-assistant.component.spec.ts`) : après chacun des trois
  appels, aucune écriture n'a eu lieu tant que personne n'a cliqué ; une liste
  vide n'est pas traitée comme un échec ; une suggestion déjà au registre n'est
  pas proposée à l'inscription.

## Références

- ISO 9001:2015 §4.2 (compréhension des besoins et attentes des parties
  intéressées), §9.3 (revue de direction), §10.2 (non-conformité et action
  corrective).
- `CLAUDE.md` §3.6 (référentiel transverse partagé), §12.3 (l'IA suggère,
  l'humain décide), §16 (modèle de rôles).
- ADR 0088 (LA fenêtre de création CAPA), ADR 0091 (sélecteur d'annuaire),
  ADR 0102 (routage IA par client).
