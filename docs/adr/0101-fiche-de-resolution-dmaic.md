# ADR 0101 — La fiche de résolution DMAIC, reprise du classeur client

- **Statut** : Accepté
- **Date** : 2026-10-24
- **Owners** : @Couldevlop
- **Portée** : api-quality-engine (`dmaic.resolution`, migration V149), écran
  `/dmaic/:id` (onglets de la fiche), écran `/admin/dmaic`
- **Complète** : ADR 0078 (droits par client), ADR 0086 (isolation par client
  en base), ADR 0092 (une personne est un utilisateur de l'annuaire)

## Contexte

Le projet DMAIC de la plateforme portait une charte (problème, objectif), des
spécifications, des mesures, la capabilité et des Poka-Yoke. Le classeur que
les équipes utilisent réellement, `DMAIC_FR_Vierge.xlsx` (dossier
`docs/DMAIC`), va bien plus loin et c'est lui que l'on retranscrit :

- un **en-tête de réclamation** : client ou projet, n° de réclamation,
  propriétaire du problème, membres de l'équipe, dates de création et de
  fermeture ; une **sévérité** sur trois niveaux et un **type de défaillance**
  pris dans une liste de 56 codes (V01 à V122) ;
- un **questionnaire par phase**, avec pour chaque question une forme de
  réponse : texte libre, ou l'une des trois listes fermées du classeur
  (Oui/Non, Oui/Non/N.A., Oui/Non/Inconnu). La phase « Définir » est rangée en
  rubriques Quoi, Qui, Pourquoi, Où, Quand, Combien ; la phase « Analyser » est
  coupée en deux, le **confinement** d'abord, les **causes** ensuite ;
- des **synthèses** : mesures conservatoires, cause racine, solutions
  retenues, plan de surveillance ;
- une analyse **5 pourquoi** à trois branches (pourquoi le défaut est apparu,
  pourquoi il a échappé au client, pourquoi le système l'a permis), chaque
  chaîne marquée ou non comme cause racine, avec son action ;
- un **plan d'actions** (date, action, responsable, statut Ouvert / En cours /
  En revue / Clos, dates prévue et réelle, avancement, notes) ;
- **six photos** avec leur remarque ;
- un **diagramme d'Ishikawa** ;
- le **rapport d'action corrective client**, qui reprend les cinq étapes pour
  le client.

## Décision

1. **Une fiche par projet**, sous-module en couches `dmaic.resolution`
   (domaine pur, ports, adaptateurs, web). Le projet DMAIC existant n'est pas
   modifié : l'énoncé du problème reste le sien, la fiche le reprend.
2. **Le contenu est un document JSON** (`dmaic_resolution_sheets.content`),
   lu et écrit d'un bloc. Sa forme suit le paramétrage du client, qui ajoute ou
   retire des questions sans migration.
3. **Le questionnaire, les types de défaillance et les sévérités sont un
   paramétrage du client**, tenu par l'administrateur (`authz.manage`). Sans
   réglage, la plateforme propose les valeurs du classeur, fautes de frappe
   corrigées. Une question est identifiée par un **code stable** : changer son
   libellé ne détache pas les réponses.
4. **La fiche se vérifie contre le paramétrage au moment où on l'écrit.** Une
   réponse hors liste, une sévérité ou un type absents du catalogue sont
   refusés (422, avec le champ). Les réponses aux questions que le catalogue ne
   pose plus restent en base sans être rendues : on ne réécrit pas l'histoire
   d'une réclamation, et remettre la question les fait réapparaître.
5. **Les listes fermées enregistrent un code** (`YES`, `NO`, `NA`, `UNKNOWN`),
   que l'écran traduit dans la langue de l'utilisateur.
6. **Les personnes viennent de l'annuaire** (propriétaire, équipe, responsable
   d'action) : compte et nom recopié, jamais un nom saisi.
7. **Écrire la fiche demande l'action `dmaic.manage`**, accordée par défaut au
   pilotage (administrateur du client, directeur, manager) et réglable dans la
   matrice des droits. La lire est ouvert à tout authentifié du module.
8. **Les photos vivent dans le stockage objet**, six emplacements, gardes
   habituelles (liste blanche JPEG/PNG/GIF/WEBP, signature binaire, 10 Mo,
   extension tirée du type). Un propriétaire d'objets les déclare au balayeur
   d'orphelins. Stockage éteint : la fiche reste lisible sans ses photos, le
   versement répond 503.
9. **Le rapport client est un PDF** produit par le serveur, dans l'ordre de la
   feuille du classeur ; une rubrique vide y est écrite « Non renseigné ».
10. **Les erreurs du sous-module ont leur propre gestionnaire** RFC 7807, limité
    à ses contrôleurs, au lieu d'alourdir le gestionnaire global.

## Conséquences

- Le classeur se remplit à l'écran, sans fichier qui circule, avec les
  personnes de l'annuaire et une trace d'audit à chaque écriture (sans texte
  libre ni nom dans la charge utile).
- Un client adapte le questionnaire à son métier : c'est ce qui porte la fiche
  au-delà de l'usage industriel du classeur d'origine.
- Les libellés par défaut sont en français, comme le classeur ; un client
  d'une autre langue les traduit dans son paramétrage. Les codes V restent en
  anglais, comme dans la liste d'origine.
- Le rapport client n'est pas encore signé ni ancré : c'est un export de
  travail. Le sceller (ML-DSA, ancrage) reprendra le mécanisme du rapport 8D.

## Vérification

- Tests du domaine : défauts du classeur (48 questions, 56 types, 3
  sévérités), nettoyage et refus du paramétrage, refus de la fiche, réponses
  conservées après retrait d'une question.
- Tests du service sur des ports en mémoire : isolation par client, fiche
  vierge sans écriture, photos (gardes, remplacement, stockage éteint), rapport.
- Tests des adaptateurs sur un vrai mapping JPA ; test du rendu PDF.
- Tests web : partage des droits, corps mal formés refusés avant le service,
  suppression d'une photo à travers la vraie chaîne de filtres.

## Références

- `docs/DMAIC/DMAIC_FR_Vierge.xlsx` (feuilles Explication, DMAIC, Photos,
  Client Claim Rapport, CAPA, 5Why, Fishbone, Validation).
- CLAUDE.md §3.4 (DMAIC), §5.4 (personnalisation par client), §18.2.
