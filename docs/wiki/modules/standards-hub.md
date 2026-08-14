# Module Standards Hub — Référentiel des normes & certification

[← Retour à l'index](../README.md) · Route : **`/standards`** · Menu : *Normes & certification › Standards Hub*

## À quoi sert ce module

Le **Standards Hub** est le référentiel des normes (ISO 9001, ISO 27001, IATF 16949, FDA…) et le
moteur qui transforme une norme en **dossier de certification actionnable**. Pour une norme que
votre organisation décide de viser (une **adoption**), le module gère les exigences, les preuves,
l'alignement, l'audit blanc, la roadmap et la génération de documents.

## Ce que vous y trouvez

- **Catalogue des normes** : parcourir les normes disponibles et leur fiche.
- **Adoptions** : déclarer qu'une norme est visée par votre organisation, puis la piloter.
- **Mes preuves** : rattacher des preuves aux exigences (documents, audits, formations…).
- **Score d'alignement** : voir le taux de couverture par norme / par clause.
- **Audit blanc** : simuler un audit pour identifier les écarts avant l'audit officiel.
- **Roadmap de certification** : suivre les étapes chronologiques jusqu'au certificat.
- **Dossier de certification** : générer le dossier complet (preuves rattachées).
- **Modèles de documents et de processus** : templates prêts à l'emploi, avec une option de
  **brouillon généré par IA** pré-rempli à partir du contexte de l'organisation.
- **Révisions** : suivre les évolutions de la norme.

## Parcours pas à pas

1. **Ouvrir** `/standards` et **parcourir le catalogue**.
2. **Adopter** la norme visée (créer une adoption).
3. **Rattacher des preuves** aux exigences au fil de l'eau.
4. **Consulter le score d'alignement** pour repérer les clauses faibles.
5. **Lancer un audit blanc** pour obtenir la liste des écarts à combler.
6. **Suivre la roadmap** étape par étape et mettre à jour l'avancement.
7. **Générer le dossier de certification** quand vous êtes prêt.
8. Le moment venu, **enregistrer la certification** obtenue.

## Auditer votre propre procédure (référentiel interne)

Le catalogue ne contient pas que des normes livrées. Vous pouvez y créer **votre propre
référentiel d'audit** à partir d'une procédure de votre GED, et auditer contre lui exactement
comme contre une ISO.

### Créer le référentiel

1. Dans **Catalogue**, cliquer sur **« Créer depuis une procédure »**.
2. Choisir la procédure source. **Seules les procédures APPROUVÉES apparaissent** : une procédure
   dont aucune version n'est publiée reste un brouillon, et auditer contre un brouillon ne prouve
   rien. Si la liste est vide, publiez d'abord une version dans la GED.
3. Le référentiel reprend le **code**, le **titre** et le **numéro de version** de la procédure.
   Ce numéro est **figé** : la procédure continuera d'évoluer, mais un audit doit rester
   rattachable à la version contre laquelle il a été mené.

Le référentiel naît **vide**. C'est voulu : ses exigences sont celles de votre organisation, et
personne d'autre ne peut les deviner sans risquer d'en inventer.

> Un seul référentiel par procédure. La seconde tentative est refusée — deux référentiels sur le
> même texte donneraient deux scores d'alignement divergents.

### Saisir les exigences

Le badge **« Procédure interne »** distingue vos référentiels des normes livrées, et le filtre
**Afficher** permet de n'afficher que les uns ou les autres.

Ouvrez la fiche (via son adoption) puis l'onglet **Exigences** : les commandes d'ajout, de
modification et de suppression n'apparaissent que sur **vos** référentiels. Une norme livrée
reste en lecture seule — son contenu vient des mises à jour de la plateforme.

La structure suit trois niveaux : **Section → Clause → Exigence**. Un code n'a besoin d'être
unique que **parmi ses voisins immédiats** : deux sections peuvent numéroter leur première clause
de la même façon.

Chaque exigence porte son texte, son **obligation** (MUST / SHOULD / MAY, qui pondère le score de
conformité), la **preuve attendue** et le **risque en cas d'absence**.

> Supprimer une section emporte ses clauses, et une clause ses exigences. L'écran le dit avant
> d'exécuter.

### Auditer contre ce référentiel

Dans un plan d'audit **encore au stade de la préparation** et dont la checklist est **vide** :
bouton **« Générer depuis un référentiel »**. Chaque exigence devient une question, avec sa
référence de clause, sa preuve attendue et son poids.

Les questions sont **copiées**, pas liées : le référentiel pourra évoluer ensuite sans réécrire
un audit déjà mené. C'est ce qui permet de relire, des années plus tard, le rapport tel qu'il a
été produit.

La génération est refusée si la checklist contient déjà des questions (deux jeux mêlés, et plus
personne ne sait lequel fait foi) ou si l'audit a déjà démarré.

### Supprimer un référentiel

Possible tant qu'**aucun projet de conformité ne le suit**. Dès qu'une adoption existe, elle
porte des preuves et un score d'alignement : le référentiel ne peut plus disparaître sous elle.
Retirez d'abord l'adoption.

Les audits déjà menés, eux, ne perdent rien : ils gardent leurs questions et leurs réponses.

## Liens avec les autres modules

- Les preuves proviennent des [audits](audits.md), [documents](../README.md), formations et KPIs.
- Les écarts d'audit blanc se traitent via [CAPA](capa.md).
- Plusieurs normes partagent des clauses communes : une même preuve peut couvrir plusieurs
  référentiels (systèmes de management intégrés).

## Bonnes pratiques

- **Rattachez les preuves en continu** : un alignement maintenu au fil de l'eau évite le rush avant
  l'audit.
- **Audit blanc avant audit réel** : c'est le meilleur moyen d'éviter les non-conformités majeures.
- **Mutualisez** : exploitez les clauses communes entre normes pour réduire l'effort documentaire.
