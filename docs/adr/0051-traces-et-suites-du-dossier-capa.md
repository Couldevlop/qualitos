# ADR 0051 — Ce qu'un dossier CAPA laisse derrière lui, et ce qu'il déclenche

- **Statut** : Accepté
- **Date** : 2026-08-09
- **Owners** : @Couldevlop

## Contexte

Trois manques de la même famille, constatés en relisant le module CAPA (§4.2) après la
livraison des preuves jointes (ADR 0050) :

1. Le dossier inscrivait au journal le versement et le retrait d'une **preuve**, mais
   rien de ce qui **décide de son sort**. Un dossier d'audit qui dit qui a joint une
   pièce sans dire qui l'a clos raconte l'accessoire et tait l'essentiel (§11.5).
2. Le contrat des webhooks (§13.2) déclarait `capa.case.opened`, `capa.case.resolved`,
   `capa.case.closed` et `capa.effectiveness.verified` — et **aucun code ne les
   publiait**. Un tenant pouvait s'y abonner et n'en recevoir jamais un seul.
3. La détection d'anomalies (ADR 0022) s'arrêtait à l'écran : on savait qu'un point
   était anormal, et rien ne s'ensuivait. Le schéma SPC→CAPA (ADR 0016) existait
   pourtant, éprouvé.

## Décision

1. **Un seul collaborateur pour le service métier.** `CapaService` déclare ce qui vient
   d'arriver au dossier à `CapaLifecycleJournal` ; il ne connaît ni le journal d'audit ni
   les abonnés. La liste de ce qui s'inscrit et de ce qui se publie tient dans une
   énumération (`CapaTransition`), où ajouter une transition sans lui donner de trace
   devient un oubli visible.
2. **Toutes les transitions ne sortent pas.** Ouverture, résolution, clôture et
   efficacité non démontrée partent aux abonnés ; démarrage, modification, rejet et
   suppression restent au journal. Publier tout ce qui bouge obligerait l'abonné à
   filtrer ce qu'on n'aurait pas dû envoyer.
3. **Deux temporalités.** Le journal s'écrit DANS la transaction — la chaîne
   d'empreintes doit être annulée avec le changement qu'elle décrit. L'annonce aux
   abonnés part APRÈS validation (`@TransactionalEventListener(AFTER_COMMIT)`) : une
   requête HTTP ne se rattrape pas, et prévenir un ERP d'une clôture que la base finit
   par annuler serait irréparable.
4. **L'efficacité non démontrée est un événement à part entière**
   (`capa.case.effectiveness-rejected`) : elle dit que l'action corrective n'a pas
   produit son effet, et le dossier repart en traitement.
5. **La charge utile dit où en est le dossier, sans emporter sa description.** Libre,
   souvent longue, elle raconte un incident : elle n'a à grossir ni les lignes du journal
   ni les envois vers un tiers.
6. **Une anomalie peut ouvrir une CAPA** (`CapaSourceType.ANOMALY`), à condition que
   l'appelant dise **sur quoi** porte l'analyse. Sans ce sujet, rien n'est ouvert : une
   action corrective qui ne dit pas sur quoi elle porte n'est pas exploitable. Le sujet
   sert aussi de clé anti-doublon (`sourceRef = "anomaly:<sujet>"`).
7. **La criticité vient de la PART d'observations anormales**, pas de leur nombre : dix
   points sur dix mille est un aléa, dix sur cinquante est une dérive. Le score brut ne
   s'y prête pas — il n'a pas la même échelle d'une méthode à l'autre.

## Justification

Le journal et les webhooks décrivent le même fait à deux publics ; les faire passer par
le même point d'entrée garantit qu'ils ne divergeront pas. Séparer leurs temporalités
n'est pas une subtilité : c'est la seule façon d'avoir à la fois une chaîne d'audit
cohérente avec la base et des tiers qui ne reçoivent que des faits acquis.

Pour l'anomalie, exiger un sujet est un refus assumé. Ouvrir un dossier que personne ne
saura rattacher à un équipement produirait du bruit dans le registre CAPA, exactement ce
que l'anti-doublon cherche à éviter par ailleurs.

## Alternatives écartées

- **Appeler le journal d'audit et les webhooks directement depuis `CapaService`** : deux
  dépendances d'infrastructure dans le service métier et deux appels à répéter dans
  chacune des sept transitions. Le premier oubli serait invisible.
- **Publier les webhooks dans la transaction** : plus simple, mais un tiers prévenu d'un
  fait annulé n'a aucun moyen de le savoir. Cela tiendrait en outre une transaction
  ouverte pendant un aller-retour réseau.
- **Déduire le sujet d'anomalie côté serveur** (empreinte de la matrice, horodatage) :
  stable pour l'anti-doublon, mais illisible dans un dossier d'audit — et un auditeur
  qui lit « anomaly:8f3c1a… » n'apprend rien.
- **Graduer la criticité sur le score brut** : les scores d'Isolation Forest et de la
  reconstruction ACP n'ont pas la même échelle ; un seuil unique aurait un sens
  différent selon la méthode choisie.

## Conséquences

- ✅ Le registre d'audit répond enfin à « qui a clos ce dossier, et quand ».
- ✅ Les quatre événements CAPA du contrat de webhooks deviennent réels ; un abonné
  reçoit ce à quoi il a souscrit.
- ✅ La boucle détection → action corrective se referme (ADR 0022 soldé sur ce point).
- ⚠ Le journal grossit : sept transitions par dossier au lieu d'aucune. La table est
  déjà indexée par tenant, ressource et action ; la rotation reste à définir.
- ⚠ Un abonné injoignable n'interrompt pas la transition métier — l'échec est tracé par
  le suivi de livraison existant (statut, relances, file de rebut), pas par une erreur
  rendue à l'utilisateur.
- ⚠ `CapaSourceType.ANOMALY` s'ajoute à une colonne `VARCHAR(30)` sans contrainte
  d'énumération en base : aucune migration nécessaire, mais les valeurs ne sont vérifiées
  que côté application.

## Tests d'invariant

- `CapaServiceTest` : chaque transition est consignée, un refus ne laisse **aucune**
  trace, et la suppression est consignée **avant** l'effacement.
- `CapaLifecycleJournalTest` : acteur issu de l'identité authentifiée, attribution au
  système faute d'identité exploitable, description absente de la charge utile,
  échappement d'un titre qui casserait la ligne, transitions internes non publiées.
- `CapaWebhookRelayTest` : tenant reposé pendant la publication et restauré après, y
  compris en cas d'échec ; un abonné injoignable ne remonte pas au métier.
- `AnomalyServiceTest` : pas de CAPA sans anomalie, sans demande ou sans sujet ; pas de
  second dossier sur le même sujet ; criticité graduée ; sujet assaini.

## Références

- CLAUDE.md §4.2 (CAPA), §11.5 (audit & traçabilité), §13.2 (webhooks), §12.1 (IA).
- ADR 0016 (SPC→CAPA), ADR 0022 (anomalies non supervisées), ADR 0050 (preuves jointes).
- ISO 9001:2015 §10.2 — non-conformité et action corrective.
