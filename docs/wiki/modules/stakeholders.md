# Module Parties prenantes — Registre des parties intéressées

[← Retour à l'index](../README.md) · Route : **`/stakeholders`** · Menu : *Pilotage › Parties prenantes*

## À quoi sert ce module

L'ISO 9001 demande au §4.2 de **déterminer** les parties intéressées pertinentes et leurs
exigences, puis de **surveiller** cette détermination. C'est l'amont du système qualité :
*qui* attend quoi de l'organisme — clients, autorités de tutelle, salariés, fournisseurs,
riverains, actionnaires.

Le registre remplace le tableur que cette question occupe habituellement, et il le fait pour
une raison précise : une exigence inscrite ici se **rattache** à un risque, à une
non-conformité, à un audit ou à un document. Elle entre dans le référentiel commun, et peut
ouvrir sa propre [CAPA](capa.md).

La lecture est ouverte à toute personne connectée : chacun peut savoir à quoi l'organisme
s'est engagé. Écrire demande l'habilitation correspondante, réglée par l'administrateur dans
*Administration › Rôles et droits*.

## Parcours pas à pas

### Lire le registre

1. **Ouvrir** `/stakeholders` (menu *Pilotage › Parties prenantes*).
2. **Lire les six tuiles** de tête : nombre de parties prenantes, internes / externes,
   criticité élevée, revues en retard, pertinence à trancher, exigences à réévaluer. Ce sont
   les six questions qu'un auditeur pose.
3. **Filtrer** la liste : recherche par nom, interne / externe, niveau de risque. *Effacer
   les filtres* revient à la vue complète.
4. **Cliquer une ligne** : la fiche s'ouvre dans un tiroir par-dessus la liste, sans la
   quitter.

### Remplir une fiche

La fiche est organisée en quatre sections, dans l'ordre où on la remplit :

1. **Identification** — nom ou raison sociale, catégorie, interne ou externe, description,
   coordonnées, sites concernés.
2. **Analyse** — besoins et attentes, exigences applicables, **pertinence pour le système
   qualité** et sa justification, risques et opportunités associés, impact qualité et impact
   réglementaire.
3. **Pilotage** — mesures de maîtrise, indicateurs de suivi, documents de référence, actions
   associées, prochaine réévaluation. Le bouton **Marquer comme revue** date la revue.
4. **Exigences** — la liste des exigences portées par cette partie prenante, chacune avec
   son texte, son origine (contrat, texte, clause), la nature de l'obligation, le risque si
   elle n'est pas tenue, la mesure de maîtrise, l'indicateur et la preuve attendue.

> **Trancher la pertinence exige un motif, dans les deux sens.** Déclarer une partie
> prenante *pertinente* ou *non pertinente* sans justification est refusé. L'état
> *à déterminer* reste un état plein, sans justification : « pas encore tranché » est une
> information honnête, et le registre la compte.

### Coter en lot dans la matrice d'évaluation

1. Onglet **Matrice d'évaluation**.
2. Pour chaque ligne, choisir l'**impact qualité** et l'**impact réglementaire**. La
   criticité, elle, ne se saisit pas : elle est affichée en lecture seule et annoncée comme
   *recalculée à l'enregistrement*.
3. Parcourir plusieurs lignes, puis **Enregistrer la cotation** une seule fois. Seules les
   lignes réellement modifiées partent ; une note ramenée à sa valeur d'origine quitte le lot.
4. *Annuler les changements* abandonne les notes en attente.

> Les notes en attente sont abandonnées si le registre est rechargé — un filtre changé, un
> enregistrement fait par un collègue. C'est volontaire : réappliquer des notes posées sur
> des valeurs périmées écraserait le travail d'un autre.

### Se faire assister (l'IA propose, vous décidez)

Onglet **Assistant IA**, trois services, tous **en lecture** — aucun n'écrit au registre :

1. **Proposer des parties prenantes** à partir du secteur d'activité et des caractéristiques
   de l'organisme. Chaque proposition porte sa justification ; celles déjà au registre sont
   signalées *Déjà au registre* et ne sont pas proposées. Vous pouvez **Inscrire**,
   **Corriger** avant d'inscrire, ou **Rejeter**. Le côté interne / externe est une
   proposition, modifiable : une filiale et un sous-traitant ne se rangent pas du même côté
   selon l'organisme.
2. **Relever les exigences** d'un document (contrat, texte réglementaire) : vous désignez la
   partie prenante qui les porte, et chaque exigence proposée vient avec la **citation du
   passage** dont elle est tirée. Sans cette citation, rien ne distinguerait une exigence lue
   d'une exigence inventée. Vous les ajoutez une par une.
3. **Détecter les changements** entre deux versions d'un document, pour marquer les exigences
   concernées *à réévaluer*.

### Rattacher une exigence au reste du système

- Depuis une exigence, **ouvrir un risque** au [registre des risques](risques-opportunites.md) :
  l'exigence devient la cause reprise, et vous renseignez la nature du risque, le processus
  où il se matérialise, la gravité et la probabilité.
- Depuis une exigence, **ouvrir une CAPA**. Attention : ce geste repasse automatiquement
  l'exigence à l'état **à réévaluer**. Une exigence ne peut pas rester affichée « maîtrisée »
  pendant qu'une action corrige ce qui la met en défaut.
- Le bouton **Exporter** produit un fichier CSV du registre tel qu'il est filtré à l'écran.

## Comment lire le résultat

- **La criticité est le maximum des deux impacts, jamais leur moyenne.** Une autorité de
  tutelle à impact réglementaire élevé et impact qualité faible ressort *élevée*, et non
  *moyenne* : son non-respect ferme l'établissement. La plus exigeante des deux cotations
  commande.
- **La criticité n'est pas stockée** : elle est recalculée à chaque lecture depuis les deux
  impacts. Elle ne peut donc pas être périmée, ni corrigée à la main.
- **Les quatre états d'une exigence** : *à analyser*, *applicable*, *maîtrisée*,
  *à réévaluer*. Le dernier est le seul qui s'obtient par un événement extérieur — un avenant,
  une révision de norme, une CAPA ouverte — et non par un travail accompli.
- **Une revue en retard** se lit directement dans la liste (*Réévaluation en retard*) et dans
  la tuile correspondante.

## Bonnes pratiques

- **Tranchez la pertinence tôt**, avec une phrase défendable. Un auditeur vient vérifier que
  le périmètre a été *déterminé*, pas *subi*.
- **Cotez en une passe** : la matrice est faite pour parcourir le registre d'un bout à
  l'autre et enregistrer une fois, pas pour ouvrir vingt fiches.
- **Ne laissez pas une exigence « maîtrisée » sans preuve attendue** : c'est le champ qui
  rend la maîtrise vérifiable l'année suivante.
- **Fixez une prochaine réévaluation** sur chaque fiche pertinente : sans elle, la
  surveillance demandée par le §4.2 n'a pas de déclencheur.
- **Relisez les propositions de l'assistant avant de les inscrire** : une exigence
  opposable à l'organisme se décide, elle ne se reçoit pas. La citation du passage est là
  pour cette relecture.
