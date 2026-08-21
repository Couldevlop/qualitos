# Module Produit, PFMEA et Control Plan — la boucle qui tient les documents à jour

[← Retour à l'index](../README.md) · Route : **`/products`** · Menu : *Opérations › Produits*

## À quoi sert ce module

Trois documents s'enchaînent, et le premier manquait.

Le **produit** dit de quoi on parle : sa référence, sa nomenclature, sa gamme. Le **PFMEA**
dit ce qui peut mal tourner à chaque opération de cette gamme, et à quel point. Le **control
plan** dit ce qu'on vérifie réellement au poste pour que cela n'arrive pas.

Sans produit, le PFMEA analyse un objet que la plateforme ne connaît pas. Sans control plan,
l'analyse de risque reste une étude que rien ne relie à la production. Et sans boucle de
retour, les deux vieillissent en silence jusqu'à la revue annuelle.

C'est cette boucle que le module ajoute : **chaque non-conformité et chaque CAPA close sur
efficacité vérifiée déposent une proposition de révision**, chiffrée et justifiée, qu'un
humain accepte ou refuse.

## Parcours pas à pas

### 1. Créer le produit

`/products` → **Nouveau produit**. La **référence** sert de clé humaine et ne se modifiera
plus : elle est citée par le PFMEA, le control plan et les non-conformités.

### 2. Saisir la nomenclature et la gamme

Sur la fiche produit, les onglets *Nomenclature* et *Gamme*.

La **gamme** mérite le soin qu'on lui donne : c'est le mot commun entre le PFMEA et le
control plan. Sans opérations nommées, les deux documents parlent d'un « poste » en texte
libre et ne se recoupent jamais.

### 3. Rattacher le PFMEA

Le PFMEA se crée dans le module [FMEA](../README.md) puis se rattache au produit. Un produit
ne porte **qu'un seul PFMEA en vigueur** : ses révisions passées et son brouillon en cours ne
comptent pas — c'est le document applicable qui est unique.

L'onglet *PFMEA* de la fiche produit trie les lignes par **priorité d'action** puis par RPN.
Ce n'est pas un détail d'affichage : le RPN multiplie les trois notes et donne le même 120
pour une défaillance grave et pour une défaillance fréquente et bénigne. La priorité d'action
lit les trois notes séparément.

### 4. En déduire le control plan

Onglet *Control Plan* → **Nouveau brouillon**, en choisissant sa phase (prototype, pré-série,
série). Un produit porte légitimement un plan en vigueur **par phase**.

Chaque ligne peut citer la **ligne de PFMEA qui la justifie**. Celles qui n'en citent aucune
portent la mention « sans justification » : un contrôle sans raison d'être coûte du temps au
poste sans réduire aucun risque, et c'est exactement ce qu'un auditeur cherche.

Un plan approuvé ne se modifie plus. Pour le faire évoluer : **Ouvrir une révision** — les
lignes sont recopiées, de sorte que corriger une ligne ne fasse pas perdre les quarante
autres. L'approbation est réservée à la direction qualité.

À l'approbation, le plan affiche un encart **« Document scellé et ancré »** : une empreinte
de 64 caractères et une référence de transaction. L'empreinte couvre le plan **et ses
lignes** — modifier une ligne, une tolérance ou une justification produirait une empreinte
différente. C'est ce qu'on donne à un auditeur qui demande à vérifier lui-même, plutôt que
de lui demander de nous croire.

Deux choses à savoir sur cet encart. Il n'apparaît **pas sur un brouillon** : un document
qui n'est pas applicable n'a rien à prouver. Et les plans approuvés **avant la mise en
service de cette fonction** n'en portent pas : les sceller après coup certifierait un
contenu que personne n'a vu approuver. Leur approbation reste tracée au journal.

Si l'approbation échoue en disant que l'ancrage est indisponible, ce n'est pas un défaut
d'autorisation : la plateforme refuse de rendre opposable un document qu'elle ne saurait pas
prouver. Le geste se rejoue tel quel une fois le service rétabli.

### 5. Déclarer une non-conformité en la rattachant

Dans le formulaire de non-conformité, choisir le **produit concerné** fait apparaître la
question du **mode de défaillance**. Le bouton *Proposer* recherche, dans le PFMEA en vigueur,
les modes dont les termes recoupent le texte du défaut — et affiche **les termes qui ont
motivé chaque suggestion**, pour qu'elle soit contestable.

« **Aucun mode ne correspond** » est un choix, pas un champ laissé vide. C'est lui qui
déclenchera la proposition d'ajouter une ligne au PFMEA : un défaut survenu sans avoir été
analysé est l'écart le plus intéressant à montrer à un auditeur.

### 6. Traiter les propositions

L'onglet *Révisions proposées* de la fiche produit — et la colonne « À réviser » de la liste,
qui dit d'un coup d'œil quels produits ont dérivé.

Chaque proposition affiche sa justification chiffrée (« 3 NC en 12 mois sur ce mode de
défaillance — occurrence 4 → 6 »). **Accepter** ouvre une révision en brouillon et y applique
le changement ; le document en vigueur ne bouge pas. **Refuser** exige un motif écrit : ne pas
bouger est aussi une décision qualité, et l'auditeur voudra la lire.

## Ce que la boucle propose, et pourquoi

| Fait | Proposition | Raison |
|---|---|---|
| NC rattachée à un mode de défaillance, historique plus lourd que la cote | Relever l'occurrence | Le terrain contredit l'analyse |
| NC sans mode de défaillance | Créer une ligne de PFMEA | Un défaut que l'analyse n'avait pas prévu |
| CAPA close, action corrective | Baisser l'occurrence | La cause a été supprimée |
| CAPA close, action qui ajoute un contrôle | Baisser la note de détection | Mieux détecter, c'est **baisser** la note |
| CAPA close, action non temporaire | Ajouter une ligne au control plan | Pour que l'action ne se perde pas |
| CAPA close, mesure d'endiguement | Rien | Un endiguement est temporaire par définition |

Le comptage des non-conformités sur douze mois glissants est une **approximation assumée** de
la table AIAG d'occurrence, qui se lit en défauts par million d'opportunités : la plateforme
ne connaît pas le volume produit. La justification affichée le dit, et vous restez libre de
refuser.

**Une non-conformité ne fait jamais baisser une cote.** Un défaut survenu ne peut pas servir
d'argument pour minorer un risque.

## Liens avec les autres modules

- [Non-conformités](non-conformites.md) : c'est là que la boucle commence.
- [CAPA](capa.md) : la fiche d'un dossier clos montre ce que sa clôture a proposé de réviser.
- FMEA : le PFMEA lui-même s'édite dans le module risques, la fiche produit le donne à lire.
- [Audits](audits.md) : le dossier produit — PFMEA en vigueur, control plan approuvé,
  historique des propositions et des refus motivés — est ce qu'un auditeur IATF demandera.

## Bonnes pratiques

- **Nommez les opérations avant de coter.** Une gamme vide rend le recoupement PFMEA /
  control plan impossible.
- **Ne laissez pas traîner un brouillon.** Un plan en brouillon n'est pas applicable : c'est
  le plan approuvé qui est affiché au poste.
- **Motivez les refus utilement.** « Non » ne dit rien ; « cotation revue le 12/08 en revue de
  risque » dit tout, et se relit deux ans plus tard.
- **Regardez l'onglet « NC liées ».** Le bloc des défauts que l'analyse n'explique pas est la
  liste de ce qu'il reste à comprendre.

## Rôles

| Action | Rôle requis |
|---|---|
| Lire produits, PFMEA, control plan, propositions | Tout utilisateur authentifié |
| Créer et modifier un produit, sa nomenclature, sa gamme | Manager qualité et au-dessus |
| Éditer un brouillon de control plan | Manager qualité et au-dessus |
| Refuser une proposition de révision | Manager qualité et au-dessus |
| **Accepter une proposition de révision** | Manager qualité et au-dessus, **+ code à usage unique** |
| **Approuver un control plan** | Directeur qualité, Admin tenant, Super admin, **+ code à usage unique** |

## Le code à usage unique, et quand il est demandé

Deux gestes engagent l'organisation : **approuver un control plan** (le document
devient opposable) et **accepter une proposition de révision** (elle écrit dans un
document approuvé). Les deux exigent que votre session porte la trace d'un second
facteur — pas seulement le bon rôle.

Concrètement : vous vous connectez normalement, avec votre mot de passe. Au moment
où vous approuvez, si votre session n'a pas encore présenté de code, l'écran vous
propose **« Se réauthentifier »** ; vous saisissez votre code, et l'approbation
repart. Le code reste valable une heure : approuver trois plans à la suite ne le
redemande pas trois fois.

**Refuser ne demande pas de code** : un refus ne modifie aucun document, il
consigne une décision. Lui imposer une friction supplémentaire découragerait
d'écrire les motifs, qui sont précisément ce que l'auditeur vient lire.

Si l'écran répond que le second facteur est indisponible, c'est que le fournisseur
d'identité de votre environnement n'a pas encore reçu la configuration
correspondante — voir `infra/keycloak/apply-step-up.sh` côté exploitation.
