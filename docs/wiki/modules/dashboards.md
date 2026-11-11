# Module Tableaux de bord — Vue exécutive, dashboards personnalisés et mode TV

[← Retour à l'index](../README.md) · Routes : **`/dashboard`**, **`/dashboard-builder`**, **`/tv`** · Menu : *Pilotage › Tableau de bord*, *Pilotage › Mes dashboards*, *Pilotage › Mode TV / Salle qualité*

## À quoi sert ce module

Trois écrans, trois usages qu'il ne faut pas confondre :

| Écran | Route | Pour quoi |
|---|---|---|
| **Tableau de bord exécutif** | `/dashboard` | la vue 360° livrée avec le produit : performance, conformité, risques |
| **Mes dashboards** | `/dashboard-builder` | composer soi-même un tableau de bord, le partager, l'exporter signé |
| **Mode TV / Salle qualité** | `/tv` | un écran mural qui tourne tout seul, sans souris |

Le premier ne se modifie pas : c'est une vue éditorialisée, identique pour tous. Le second
est à vous. Le troisième n'est pas un tableau de bord de plus, c'est une **façon de
l'afficher**.

## Parcours pas à pas — le tableau de bord exécutif

1. **Ouvrir** `/dashboard` (menu *Pilotage › Tableau de bord*). L'en-tête rappelle la date
   des données affichées.
2. **Lire la bande d'indicateurs stratégiques**, puis les blocs : tendance qualité globale
   sur 12 mois (réalisé contre cible), **Pareto des défauts**, heatmap de conformité par
   norme et par section, top risques et alertes, prédictions, alignement normatif.
3. **Cliquer une catégorie du Pareto** : le **filtre croisé** s'applique à toute la page. Un
   bandeau annonce « Filtre croisé actif », avec un bouton **Effacer**. Recliquer la même
   catégorie retire le filtre.
4. **Descendre d'un niveau** (*drill-down*) depuis une catégorie pour voir ses sous-causes ;
   « Toutes les catégories » remonte.
5. **Annoter** un graphique : le panneau *Annotations* reçoit un commentaire ancré sur la
   catégorie sélectionnée. L'annotation est horodatée, attribuée à son auteur et conservée
   côté serveur — elle est donc visible par vos collègues du même périmètre, et non perdue
   en fermant l'onglet. Seul son auteur, ou un administrateur de l'organisation, peut la
   supprimer.
6. **Exporter** la bande d'indicateurs en CSV, ou ouvrir **Nouveau rapport**.

### Revenir à une date passée

1. Dans l'en-tête, le sélecteur **« État au »** (voyage dans le temps).
2. Choisir une date : l'écran affiche, pour chaque indicateur actif, **sa dernière mesure
   antérieure ou égale** à cette date. Un bandeau rappelle en permanence que vous lisez un
   état historique.
3. Si aucune mesure n'existe à cette date, l'écran le dit et affiche *n/d* — rien n'est
   reconstitué ni approché.
4. Le bouton **Présent** revient à l'état courant.

> Les dates futures sont ramenées à maintenant : le voyage dans le temps regarde le passé.
> Pour l'avenir, c'est la [prévision KPI](forecast.md) qui répond.

## Parcours pas à pas — composer son tableau de bord

1. **Ouvrir** `/dashboard-builder` (menu *Pilotage › Mes dashboards*). La liste montre vos
   tableaux de bord, ceux qui sont partagés, et le nombre de widgets de chacun.
2. **Nouveau tableau de bord**, puis le nommer.
3. **Glisser un widget** depuis la palette vers la grille. Neuf types sont disponibles :
   indicateur (KPI), courbe, barres, camembert, jauge, carte de contrôle, tableau, heatmap et
   bloc narratif.
4. **Déplacer** un widget par sa poignée, le **redimensionner** par son coin, le
   **configurer** (KPI du catalogue, libellé, unité, seuil de bascule, texte pour un bloc
   narratif) ou le **supprimer**.
5. **Partagé / Privé** décide qui le voit : vous seul, ou votre organisation.
6. **Enregistrer**. La mise en page est conservée côté serveur, dans le périmètre de votre
   organisation.
7. **Exporter en PDF signé** : le document est signé (signature hybride Ed25519 + ML-DSA-65)
   et son empreinte est ancrée. Le PDF porte un **code de vérification** : n'importe qui,
   sans compte ni jeton, peut contrôler qu'un exemplaire n'a pas été retouché. La réponse de
   vérification ne livre que des faits d'intégrité, jamais le contenu du tableau de bord.

## Parcours pas à pas — le mode TV

1. **Ouvrir** `/tv` (menu *Pilotage › Mode TV / Salle qualité*) sur l'écran mural.
2. **Choisir l'intervalle** de rotation. L'écran enchaîne ensuite les vues seul.
3. L'heure du dernier relevé est affichée en permanence : un écran figé se repère
   immédiatement.
4. Les cartes de prévision affichent leur **niveau de confiance** à côté de la valeur.

## Comment lire le résultat

- **Un filtre croisé n'est pas un filtre de données globales** : il atténue les catégories
  non retenues pour vous laisser comparer. Tant que le bandeau est affiché, ce que vous lisez
  est restreint.
- **L'état historique n'est pas une reconstitution** : c'est la mesure réellement enregistrée
  à cette période. Un indicateur créé après la date demandée n'a donc pas de valeur, et
  l'écran l'affiche *n/d* plutôt que zéro.
- **Un PDF exporté est opposable, pas décoratif** : la signature et l'ancrage portent sur
  l'empreinte des octets rendus. Un PDF modifié après coup échoue à la vérification.
- **Une annotation est un acte tracé** : elle porte le nom de son auteur et sa date.

## Bonnes pratiques

- **Huit à douze indicateurs au plus** sur une vue de direction. Au-delà, on ne décide plus,
  on consulte.
- **Annotez les décisions, pas les constats** : « commande passée le 12, effet attendu en
  mars » vaut mieux que « pic anormal ».
- **Partagez un dashboard quand il est stable.** Un tableau de bord partagé puis remanié
  chaque semaine fait douter de tous les chiffres qu'il porte.
- **Sur l'écran mural, moins de widgets et de gros caractères** : le mode TV se lit à trois
  mètres, debout, sans pouvoir cliquer.
- **Reliez chaque indicateur affiché à sa définition** : un KPI du
  [catalogue](kpis.md) porte sa formule, sa cible et sa source. Un chiffre sans
  définition se discute sans fin.
- **Pour un document à diffuser hors de l'application**, préférez l'export PDF signé à une
  capture d'écran : seul le premier se vérifie.
