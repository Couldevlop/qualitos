# Module Marketplace — Packs normatifs publiés par des partenaires

[← Retour à l'index](../README.md) · Routes : **`/marketplace`**, **`/marketplace/submit`**,
**`/marketplace/moderation`** · Menu : *Référentiels › Marketplace*

## À quoi sert ce module

Le marketplace est le catalogue des **packs normatifs et sectoriels** que des **cabinets
partenaires** proposent, et que votre organisation peut **installer** : référentiels, modèles
de documents et processus prêts à l'emploi pour un besoin précis — par exemple un pack
ISO 13485 pensé pour une jeune entreprise de dispositifs médicaux.

L'idée est simple : tout ce qui est propre à un métier n'a pas à être développé dans la
plateforme. Un cabinet qui connaît son secteur le met en pack, l'éditeur l'instruit, vous
l'installez.

Trois écrans, trois rôles :

| Écran | Pour qui | Route |
| --- | --- | --- |
| **Catalogue** | tout utilisateur ; l'installation est réservée à l'administrateur du client | `/marketplace` |
| **Proposer un pack** | un partenaire | `/marketplace/submit` |
| **Modération** | l'éditeur de la plateforme | `/marketplace/moderation` |

## Parcours pas à pas — installer un pack

1. **Ouvrir** `/marketplace`. Le catalogue n'affiche que les packs **publiés** : un pack en
   cours d'instruction ou rejeté n'y figure pas, et son adresse directe ne donne rien.
2. **Rechercher** un pack par son nom.
3. **Installer**. Le pack est alors marqué **Installé** dans le catalogue.
4. **Noter** le pack — la note est ce qui guide les organisations suivantes.
5. **Désinstaller** si le pack ne convient pas.

Deux garanties à connaître :

- **Une seule installation active** d'un même pack par organisation, tenue par la base de
  données et non par l'écran.
- **L'historique est conservé.** Désinstaller ne supprime rien : la trace de ce qui a été
  installé, quand, et par qui reste disponible — c'est ce qu'un audit vient regarder.

## Parcours pas à pas — proposer un pack (partenaire)

Depuis le catalogue, **Proposer un pack** ouvre le formulaire de soumission :

- le **nom** et la **version** (format `X.Y` ou `X.Y.Z` — l'écran refuse autre chose) ;
- l'**éditeur / partenaire** et le **secteur** visé ;
- la **description** ;
- les **normes couvertes**, séparées par des virgules ;
- le **prix** (en centimes) et la **devise** ;
- une **signature partenaire** (empreinte de 16 à 128 caractères).

Le **manifeste** du pack est contrôlé **avant tout enregistrement**. Le contrôle refuse un
manifeste qui ne serait pas un objet JSON, qui dépasserait 64 Kio, auquel il manquerait le nom
ou la version, dont un identifiant de norme ne serait pas un code simple, ou — surtout — qui
référencerait un fichier par un chemin suspect (remontée de répertoire, chemin absolu, adresse
distante). Le contrôle **ne télécharge jamais** le contenu distant : un pack ne peut pas servir
à faire appeler une adresse de son choix par la plateforme.

## Parcours pas à pas — instruire un pack (éditeur)

`/marketplace/moderation` est la **file d'instruction**, et son sous-titre dit la règle :
*« Aucun pack n'est publié sans validation humaine. »*

1. Lire la soumission et son **manifeste**.
2. **Prendre en revue** — le pack passe en instruction, ce qui dit aux autres modérateurs qu'il
   est pris en charge.
3. Puis **Publier**, ou **Rejeter** avec un motif.

Le cycle de vie est : **soumis → en revue → publié** *ou* **rejeté**, et **déprécié** en fin de
vie. Un pack **ne peut pas atteindre « publié » sans être passé par l'instruction** : ce n'est
pas une consigne d'usage, c'est la règle que le code applique — une transition sautée est
refusée.

Cette file est réservée à l'**éditeur de la plateforme**. Un partenaire soumet, il n'instruit
pas ; un administrateur de client installe, il ne publie pas.

## Comment lire l'articulation avec les packs sectoriels

Ne confondez pas les deux notions :

- un **[pack sectoriel](industry-packs.md)** (*Industry Pack*, route `/industry-packs`) fait
  partie de la plateforme : il s'active, et il apporte KPI, modèles et normes pré-configurés ;
- un **pack du marketplace** est un produit tiers que l'on **installe**.

Les deux mécanismes restent distincts, et c'est volontaire : l'installation d'un pack du
marketplace recopie le **code** du pack, ce qui permet de le rapprocher d'un pack sectoriel
homonyme sans que l'un dépende de l'autre.

## Bonnes pratiques

- **Vérifiez la version avant d'installer.** Un pack normatif suit la révision de sa norme ;
  une version en retard documente un référentiel qui n'est plus celui de l'auditeur.
- **Notez les packs que vous utilisez.** C'est le seul signal dont dispose l'organisation
  suivante.
- **Désinstallez ce qui ne sert pas** plutôt que de le laisser actif : un référentiel installé
  et ignoré finit par être cité dans un audit comme un engagement non tenu.
- **Partenaires : soignez le manifeste.** C'est lui qui est instruit en premier, et c'est lui
  qui fait refuser une soumission avant même qu'un humain la lise.
