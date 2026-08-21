# Six modules maquettés, et ce que le code en porte déjà

- **Date du point** : 2026-08-21
- **Source** : `docs/nouveaux_modules_qms_maquettes.html` (19/08/2026)
- **Version examinée** : `main-fb1423f44007`, déployée en préproduction

## En une phrase

Aucun des six modules maquettés n'existe. Mais le lot Produit / PFMEA / Control
Plan livré le 21/08 pose des briques que trois d'entre eux réclament, et l'un —
l'efficacité CAPA — est à moitié construit sans avoir été nommé.

## L'état, module par module

| Module maquetté | Couverture | Ce qui existe et sert |
| --- | --- | --- |
| **Efficacité CAPA** | ~50 % | `CapaCase.effectivenessVerified` et sa date ; `NcHistoryPort.countForProductAndFailureMode(tenant, produit, mode, depuis)` compte les non-conformités sur fenêtre glissante — c'est le calcul même de la « récurrence 6 mois » |
| **Boîte à idées** | ~60 % en données | `circle.CircleProposal`, dont le cycle `PROPOSED → UNDER_REVIEW → APPROVED → REJECTED → IMPLEMENTED → MEASURED` recouvre les colonnes de la maquette. Manquent le **vote** et une saisie hors cercle |
| **Suivi ESG** | ~25 %, épars | `Supplier.score` et `SupplierAuditRecord.score` pour la note fournisseur ; `ehs` porte un type d'incident `ENVIRONMENTAL`. Rien sur le CO₂, les déchets valorisés, ni la pondération d'un score ESG |
| **Coût de la qualité** | ~10 % | Le catalogue KPI accepte n'importe quelle définition (code, unité, cible, seuils) : le COQ y tiendrait sans schéma neuf. Mais rien ne le décompose en prévention / évaluation / défaillance interne / externe, et le terme n'apparaît que dans un commentaire du pack automobile |
| **Continuité d'activité** | ~5 % | ISO 22301 figure au catalogue du Standards Hub, et `nis2measures` traite de continuité côté cyber. Aucun registre d'incident de continuité, aucun plan |
| **Revue de contrat** | 0 % | Rien : ni exigence client, ni capacité interne, ni écart |

## Ce que le lot du 21/08 apporte à cette feuille de route

Trois acquis, réutilisables tels quels :

1. **Le patron « la machine propose, l'humain tranche »** (`revisionrequests`) :
   une source de faits dépose une proposition chiffrée et justifiée, un humain
   accepte ou refuse, et le refus exige un motif. La revue de contrat et la
   boîte à idées ont exactement cette forme.
2. **Le comptage d'événements sur fenêtre glissante**, écrit et testé. C'est le
   cœur de l'efficacité CAPA.
3. **Le scellement d'un document approuvé** — empreinte, signature hybride,
   ancrage. Un plan de continuité et une revue de contrat sont, eux aussi, des
   documents opposables.

## Deux questions de cadrage, tranchées

**La maquette annonce cinq modules et son ruban en montre six.** L'efficacité
CAPA s'est ajoutée aux cinq du titre. Elle est traitée comme un **onglet du
module CAPA existant**, non comme un module : c'est une lecture des dossiers
clos, pas un objet nouveau, et en faire un module dupliquerait le référentiel.

**La boîte à idées recouvre les propositions de cercle de qualité.** Un seul
objet, enrichi du vote et d'une saisie hors cercle, plutôt que deux écrans pour
la même chose — « l'agrégation est dans la donnée, pas dans l'UI »
(CLAUDE.md §3.6).

## Ordre proposé

| Rang | Module | Pourquoi ici |
| --- | --- | --- |
| 1 | **Efficacité CAPA** | L'essentiel des données existe ; il manque une définition formelle et une lecture |
| 2 | Boîte à idées | Enrichit un modèle en place plutôt que d'en créer un |
| 3 | Coût de la qualité | Modèle simple, mais exige une convention comptable partagée avec le métier |
| 4 | Suivi ESG | Dépend de sources externes (mesures, facteurs d'émission) qu'il faut d'abord décider |
| 5 | Continuité d'activité | Modèle métier neuf, adossé à ISO 22301 |
| 6 | Revue de contrat | Tout est à faire, et c'est le seul qui exige un modèle métier entièrement neuf |

## Ce que ce point ne dit pas

Les pourcentages ci-dessus mesurent la **présence de briques**, pas l'avancement
d'un module. Un module à « 60 % de données » peut demander autant de travail
qu'un module à zéro : l'écran, les règles, les tests et la documentation restent
entiers. Ils servent à ordonner, pas à estimer.
