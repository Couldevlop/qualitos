# Demande de dérogation client (concession) — description de besoin

Description de besoin **arbitrée**. Le document décrit ce que les deux pièces
fournies contiennent réellement, le processus tel qu'il a été énoncé, les briques
de la plateforme qui servent déjà, et les arbitrages rendus. Aucun code, aucune
migration, aucun ADR n'en découle encore.

**La maquette fait loi pour tout ce que le §0 ne tranche pas.** C'est la règle
donnée le 21 novembre — « pour les autres champs regarde la maquette » — et elle
ferme la section 4 : une question qui y reste écrite sans arbitrage explicite se
résout en lisant la maquette, dont la section 1 du présent document relève
champ par champ le contenu. La section 4 est conservée parce qu'elle garde la
trace du *pourquoi* de chaque choix, non parce qu'elle attendrait encore.

---

## 0. Arbitrages rendus

Quatre points attendaient une réponse. Ils sont tranchés. La source de chacun est
indiquée, parce que c'est elle qui fait foi : la **maquette** pour le vocabulaire
et les états, la **consigne** pour le reste.

**Le nom retenu est « dérogation ».** La maquette l'écrit dans son titre —
« Demande de dérogation (concession) client » — et dans son en-tête,
« QualitOS · Dérogations clients ». « Concession » y figure comme glose entre
parenthèses, et c'est ainsi que la présentation l'emploie en raccourci. La section
s'appelle donc **« Dérogations »**, et son bouton **« Ajouter une dérogation »** :
le « Add concession » de la présentation est une note d'auteur, pas un libellé
d'écran — la plateforme est en français et ses libellés sont figés (ADR 0052).

**Le brouillon figure au registre.** La maquette connaît un état `draft`
« Brouillon », et rien ne l'exclut de la liste. Les quatre statuts de la
présentation ne sont donc pas un second axe à raccorder : ils sont la **suite du
même cycle de vie**, dont la maquette porte le début.

| Étape | Libellé de la maquette | Statut au registre | Qui doit agir |
| --- | --- | --- | --- |
| Saisie en cours | `draft` « Brouillon » | Brouillon | l'initiateur |
| Validation manager demandée | `pending` « Signature demandée », badge « En attente du manager qualité » | En attente interne | le manager |
| Le manager n'accepte pas | `draft` (retour au brouillon), bouton « Renvoyer pour correction » | **À modifier** | l'initiateur |
| Le manager a signé | `signed` « Signé », badge « Approuvée par la qualité » | **Accepté** | l'initiateur |
| Envoyée au client | *(aucun)* | En attente client | le client |
| Retour du client | Décision : « Acceptée » / « Acceptée sous conditions » / « Refusée » | Acceptée / Refusée | l'initiateur, qui porte la décision |

**La boucle interne.** Le manager ne refuse pas : s'il n'accepte pas, le dossier
passe **« À modifier »** et revient à l'initiateur, qui corrige et le renvoie au
manager — **autant de fois qu'il le faut, jusqu'à la signature**. Il n'y a donc
qu'une seule issue favorable, **« Accepté »**, et elle est encore **interne** :
rien n'est parti au client à ce stade. La sortie de boucle par le bas existe déjà
dans la maquette, c'est le bouton **« Retirer la demande »** de l'initiateur ;
le manager, lui, n'a aucun moyen de clore un dossier.

Ce qui reste ouvert sur ce cycle :

- **« Accepté » et « Acceptée » se ressemblent à une lettre près**, et ne disent
  pas la même chose : le premier est la signature du manager, en interne ; le
  second la réponse du client. Dans une colonne de registre, les deux se lisent
  côte à côte. Le cycle est validé tel quel, mais le **libellé du statut interne
  reste à choisir** — proposition en Q29.
- La présentation ne sait toujours pas où ranger **« Acceptée sous conditions »**
  (Q28), et la boucle ne dit pas si elle **garde la trace de ses tours** (Q3) ni
  si « À modifier » **porte le motif** de ce qui doit changer (Q2) — sans lui,
  l'initiateur ne sait pas quoi corriger.

**La colonne « défaut » montre l'écart.** La maquette n'a aucun champ portant ce
nom ; ce qu'elle a, c'est la section 3, intitulée **« Écart »**, et son tableau à
trois colonnes — « Spécification / exigence client », « Valeur réelle / écart
demandé », « Qté non conforme ». La colonne du registre affiche donc l'écart, pris
sur la **première ligne** de ce tableau ; un dossier peut en porter plusieurs, et
les autres se lisent dans la fiche. Aucun champ n'est créé pour cette colonne.

**L'envoi au client est un courriel de la plateforme, dérogation en pièce
jointe.** Le bouton « Validation client » ne télécharge pas un fichier à poster
soi-même : **QualitOS envoie le courriel**, et la dérogation y est **jointe en
PDF**, produit depuis les données de la fiche et **bien structuré** — pas une
impression de navigateur. La diapositive 3 de la présentation décrit l'ancien
geste (« ouvre l'impression. On choisit "Enregistrer au format PDF" […] prêt à
envoyer par mail ») : il est remplacé, et c'est un **seul** geste, pas deux.

Trois conséquences, dont une lourde :

- **Le PDF se fabrique côté serveur**, comme le font déjà le rapport 8D, le
  rapport de NC fournisseur et le rapport de réclamation (PDFBox, cf. §3). Le
  rendu de la dérogation doit **garder ses accents** : c'est acquis depuis
  l'ADR 0134, et les deux rendus de réclamation et de NC fournisseur le faisaient
  déjà. Un document qui part chez un client n'écrit pas « Dérogation accordée »
  en « Derogation accordee ».
- **Ce courriel-ci sort de l'organisation**, contrairement à celui du manager. Son
  expéditeur doit donc être le client au sens de la plateforme — c'est exactement
  ce que l'ADR 0127 règle, et ce que le moteur qualité ne sait pas faire
  aujourd'hui. **Q17 n'est plus différable** : elle bloque cet envoi, pas celui du
  manager.
- La **réponse** revient par courriel, hors plateforme, sous la forme du PDF signé
  par le client (diapositive 4). Rien ne la rapatrie automatiquement : l'initiateur
  porte la décision à la main en section 6.

**Le courrier reproduit la fiche, et sa langue se choisit à l'envoi.** Le
document joint est **conforme à l'interface d'édition de la dérogation** : il
reprend la fiche telle qu'elle se présente à l'écran, ses six sections dans leur
ordre, et rien d'autre. Ce n'est donc ni un extrait, ni une mise en page
inventée pour l'occasion — ce que le client lit est ce que l'initiateur a rempli.

La langue se **choisit au moment de l'envoi**, entre **français et anglais**. Deux
seulement, là où la plateforme en sert six : c'est le besoin, pas une limite
technique. Le choix se pose à côté de l'adresse du client et du commentaire, au
moment de « Validation client ».

Une conséquence à tenir : le PDF n'étant pas scellé, il peut être réémis. La
langue choisie doit donc être **retenue sur le dossier**, faute de quoi un second
envoi partirait dans une autre langue sans que personne l'ait décidé. C'est
l'esprit de l'ADR 0129, qui fige la langue du rapport 8D à l'émission plutôt que
de traduire à la relecture.

**Le courriel au client est un simple courriel avec pièce jointe.** Arbitré le
22 novembre : « le mail qu'on envoie au client c'est juste un simple mail avec
pièce-jointe ». Cela porte sur le **message**, et sur lui seul : corps bref en
texte brut, la dérogation en pièce jointe PDF, et rien d'autre — ni gabarit
élaboré, ni sceau, ni QR de vérification, ni accusé de lecture.

**Cela ne révoque pas l'expéditeur par client**, qui est une consigne explicite
du 21 novembre : « Dans l'interface de chaque admin tenant, on n'a la
possibilité de paramétrer son adresse mail qui sortira le mail mais en copie
caché le mail de l'initiateur. » L'adresse d'envoi reste donc celle que règle
l'administrateur du client (ADR 0127, écran `admin/pages/mail-settings`), et
l'initiateur reste en copie cachée. Ce que Q17 garde d'ouvert est donc le seul
**chemin** par lequel le moteur atteint ces réglages — un détail de mise en
œuvre, non une décision de produit.

À reprendre quoi qu'il arrive de `SmtpAuditReminderMailer` : le **corps en texte
brut à dessein** (un titre saisi par un utilisateur n'a pas à être échappé en
HTML) et `singleLine()` sur le sujet (injection d'en-tête, OWASP A03).

**Le statut interne s'appelle « Approuvé qualité ».** Le choix du libellé a été
délégué — « si tu veux tu peux proposer autre statut pour la validation
interne » — et c'est celui-là, pour trois raisons : c'est le mot de la maquette
elle-même, dont le badge dit « Approuvée par la qualité » ; il nomme **qui** a
tranché, ce qu'aucun autre statut du registre ne fait ; et il ne se confond pas
avec l'« Acceptée » du client, dont il ne différait que d'une lettre. Le tableau
du cycle de vie ci-dessus se lit donc avec « Approuvé qualité » là où il écrit
« Accepté ». Les libellés étant figés (ADR 0052), celui-ci l'est désormais.

**Aucun rattachement normatif.** Le dossier ne se raccroche à aucune clause :
ni ISO 9001 §8.7, ni IATF 16949, ni AS9100D, bien que les trois soient au
catalogue du Standards Hub. Le vocabulaire des pièces est celui de la qualité
automobile et aéronautique, mais rien n'est déclaré, et rien ne le sera.

**Le renvoi est motivé, la boucle est tracée, la signature est simple.** Trois
points du cycle interne sont arrêtés :

- **« À modifier » porte le motif.** Le manager dit ce qui doit changer, sinon
  l'initiateur ne sait pas quoi corriger et la boucle tourne à vide. Le champ est
  obligatoire au renvoi, comme le rejet motivé de la GED (ADR 0099).
- **La boucle garde ses tours, et ils sortent dans la piste d'audit.** Chaque
  demande de validation, chaque renvoi motivé, chaque signature est un événement
  journalisé — pas seulement un état qui écrase le précédent. La brique existe :
  `AuditEventService.record(...)` (`.../quality/auditlog/`), chaînée par empreinte
  et vérifiable (`verifyChain`), que les modules alimentent par un adaptateur
  `AuditLog<X>EventPublisher` posé dans leur propre `infrastructure`. C'est aussi
  ce qui permet de répondre à « combien d'allers-retours avant signature », que la
  seule colonne de statut ne saurait dire.
- **La signature du manager reste simple**, telle que la maquette la fait : nom
  saisi et tracé à la souris ou au doigt. **Ni second facteur, ni signataire au
  sens de l'ADR 0099, ni sceau.** Le `StepUpGuard` ne s'applique pas, et le
  dossier ne passe pas par le régime des actions critiques du §18.2 #5. La chaîne
  est donc **déclarative de bout en bout** — c'est cohérent avec le PDF non
  scellé ci-dessous, et la seule pièce probante du dossier reste le PDF que le
  client renvoie signé de son côté.

**Le PDF n'est pas scellé, et le commentaire est optionnel.** Le document joint
au courriel est un PDF **structuré mais non scellé** : ni signature
cryptographique, ni ancrage, ni QR de vérification — contrairement au rapport 8D.
La traçabilité repose donc sur le registre et sur le PDF que le client renvoie
signé de son côté, non sur une empreinte que la plateforme pourrait revérifier. Le
champ **commentaire** ajouté au moment de « Validation client » est
**optionnel** ; saisi là, à côté de l'adresse du client, il accompagne le message
— c'est la lecture retenue, faute d'autre place où il puisse servir.

**L'expéditeur est réglé par l'administrateur du client, et l'initiateur est en
copie cachée.** Chaque organisation paramètre, dans son écran d'administration,
**l'adresse d'où sortent ses courriels** ; le courriel envoyé au client porte en
**copie cachée l'adresse de l'initiateur**, qui garde ainsi une trace de ce qui
est parti sans que le client voie son adresse.

Ce paramétrage **existe déjà**, et il n'y a rien à créer :

- l'écran est `apps/web/src/app/features/admin/pages/mail-settings/`, réservé à
  l'administration du client comme tout paramétrage ;
- le modèle porte exactement ce qu'il faut — `fromAddress`, `fromName`,
  `replyTo`, le serveur sortant et sa sécurité, un interrupteur `enabled`, et un
  **envoi d'essai** daté par `verifiedAt` ;
- `configured: false` signifie que l'organisation **n'a pas encore réglé** sa
  messagerie.

**Arbitrage du 24 novembre 2026 — aucun repli.** Sans réglage, l'envoi au client
**ne part pas**, et le refus nomme le paramétrage à poser ; le dossier reste
« Approuvé qualité », donc renvoyable dès que la messagerie est réglée. La
messagerie se règle dans les paramètres de l'organisation, qui y désigne une
**adresse générique** — celle de son service qualité plutôt que celle d'une
personne.

C'est ce qui distingue ce courriel de celui d'une **facture**, laquelle se replie
bien sur la messagerie de la plateforme (`TenantAwareInvoiceMailer`) : une facture
émane de QualitOS, une dérogation émane du fournisseur de son destinataire.
Partir d'une adresse de plateforme ferait arriver chez le client un courrier qui
l'engage sous un nom qui n'est pas celui de son fournisseur.

> Une version antérieure de ce paragraphe annonçait l'inverse — un repli sur la
> messagerie de la plateforme, sous une adresse générique, l'initiateur restant
> en copie cachée. La lecture était fautive : « adresse générique » désignait une
> adresse générique **du client**, non de la plateforme. La trace est conservée
> plutôt qu'effacée, le javadoc de `pourEnvoi` portant encore la formule qui a
> induit l'erreur.

La copie cachée, elle, se pose **au message** et non au paramétrage : rien à
ajouter à l'écran.

Ce qui manque n'est donc ni l'écran ni la décision, c'est le **chemin** : ces
réglages vivent dans `api-core` et le moteur qualité ne sait pas les lire (§3.5).
C'est tout ce que Q17 recouvre encore.

**Le courriel au manager est interne.** Il part de l'initiateur vers **son
manager**, et toujours **avant** tout envoi au client : la frontière décrite en
§2.5 passe là. Le destinataire appartient donc à l'organisation, ce qui change la
portée de Q17 sans la fermer — un courriel interne n'a pas à partir au nom du
client, comme devrait le faire celui qui s'adresse au client lui-même.

---

## 1. Ce que les deux fichiers contiennent réellement

### 1.1 Sources

| Pièce | Nature | Ce qu'elle apporte |
| --- | --- | --- |
| `docs/derogation/QualitOS – Demande de dérogation client.html` | maquette HTML autonome (12,9 ko), formulaire + simulateur de rôles | la totalité des champs, des libellés et des trois états |
| `docs/derogation/Concessions (1).pptx` | 4 diapositives, aucune note de commentaire remplie, une image (copie d'écran de la section 6) | l'emplacement dans la navigation, le comportement attendu, et deux corrections sur la maquette |

### 1.2 Le formulaire de la maquette

Titre de la page : **« Demande de dérogation (concession) client »**.
Sous-titre : **« À soumettre au client *avant* expédition de pièces non conformes
aux spécifications »**.
En-tête de l'application : **« QualitOS · Dérogations clients »**.

Chaque fiche porte une **référence** de la forme `DER-<année>-<3 chiffres>`
(générée aléatoirement dans la maquette) et un **badge d'état** (voir 1.4).

#### Section 1 — « Identification »

Huit champs, tous en saisie libre sauf mention, aucun marqué obligatoire dans le
balisage ; le contrôle réel à la soumission est décrit en 1.5.

| Libellé exact | Type | Indication de saisie | Obligatoire à la soumission |
| --- | --- | --- | --- |
| Client | texte | « Nom du client » | **oui** |
| Contact qualité client | texte | « Nom, e-mail » | non |
| N° de commande client | texte | — | non |
| Réf. pièce / désignation | texte | — | **oui** |
| Indice | texte | — | non |
| N° de lot / série | texte | — | non |
| Quantité concernée | nombre (min 0) | — | **oui** |
| Date de la demande | date | pré-remplie au jour courant | non |

#### Section 2 — « Nature de la demande »

Quatre groupes de boutons radio, plus un champ libre. Les libellés d'option sont
ceux affichés ; la valeur portée par le balisage est donnée entre parenthèses
quand elle diffère.

| Groupe | Options (libellé affiché) | Défaut |
| --- | --- | --- |
| expédition | « Pièces à expédier » (`À expédier`) · « Pièces déjà expédiées » (`Déjà expédiées`) | Pièces à expédier |
| origine | « Lié au produit » (`Produit`) · « Lié au process » (`Process`) | Lié au produit |
| occurrence | « Première occurrence » (`Première fois`) · « Répétition » | Première occurrence |
| durée | « Temporaire » · « Permanente » | Temporaire |

Champ libre associé : **« Durée / lot concerné : »**, exemple d'aide
« ex. lot unique, jusqu'au 31/12 ».

#### Section 3 — « Écart »

Un tableau à lignes ajoutables (deux lignes vides à l'ouverture), trois colonnes :

| Colonne | Type |
| --- | --- |
| Spécification / exigence client | texte |
| Valeur réelle / écart demandé | texte |
| Qté non conforme | nombre (min 0) |

Bouton sous le tableau : **« + Ajouter une ligne »**. Aucun bouton de suppression
de ligne dans la maquette.

Puis un champ long, obligatoire :
**« Justification : pourquoi les pièces peuvent être utilisées (forme, ajustement,
fonction) »**.

#### Section 4 — « Cause, confinement et actions »

| Libellé exact | Type | Obligatoire à la soumission |
| --- | --- | --- |
| Cause racine | texte long | **oui** |
| Confinement (stock, en cours, déjà livré) et traçabilité | texte long | non |
| Action corrective / préventive | texte long | **oui** |
| Réf. 8D / CAPA et date prévue | texte, aide « CAPA-2026-xxx · échéance » | non |
| Pièces jointes | texte, aide « mesures, photos, rapport 8D… » | non |

Encart sous la section, à reprendre tel quel : **« Les lots livrés sous dérogation
doivent être identifiés clairement et accompagnés d'une copie de la demande
signée. »**

#### Section 5 — « Approbation interne »

Son contenu dépend de l'état et du rôle (voir 1.4).
Au brouillon : une liste déroulante **« Manager qualité à solliciter »**, peuplée
dans la maquette de deux entrées en dur (« Karim Benali – Manager qualité »,
« Sophie Martin – Manager qualité adjointe »), suivie de la phrase **« Le manager
reçoit une notification QualitOS et signe avant tout envoi au client. »**

Une fois signée : **« Manager qualité »** (nom), mention **« Signé le <date et
heure> »**, et **« Signature »** (image du tracé).

#### Section 6 — « Décision du client »

| Libellé exact | Type | Valeurs |
| --- | --- | --- |
| Décision | liste | « En attente » (vide) · « Acceptée » · « Acceptée sous conditions » · « Refusée » |
| Nom, date, signature client | texte | — |

La diapositive 4 amende cette section : « Pas besoin du nom et de la signature du
client, la décision et la date suffisent, cette partie sera seulement pour la
traça interne, sinon la réponse signée du client sera le PDF signé
électroniquement de sa part et retourné par mail. » La copie d'écran jointe à la
diapositive montre exactement les deux champs actuels, c'est-à-dire ce qu'il faut
corriger.

Cette section reste saisissable **à tous les états** dans la maquette : le
verrouillage du formulaire l'épargne explicitement, ainsi que la section 5.

### 1.3 Les deux rôles de la maquette

Un sélecteur « Vue : » en en-tête, présenté par la diapositive 3 comme un artifice
de démonstration :

- **« Responsable qualité fournisseur (demandeur) »**
- **« Manager qualité (signataire) »**

### 1.4 Les états du dossier et les boutons

Trois états, affichés en fil d'étapes et en badge :

| État | Libellé d'étape | Libellé du badge |
| --- | --- | --- |
| `draft` | « Brouillon » | « Brouillon » |
| `pending` | « Signature demandée » | « En attente du manager qualité » |
| `signed` | « Signé » | « Approuvée par la qualité » |

Boutons, par état et par rôle — libellés **exacts** de la maquette :

| État / rôle | Boutons |
| --- | --- |
| Brouillon (demandeur) | « Enregistrer le brouillon » · **« Demander la signature »** |
| Signature demandée (demandeur) | « Retirer la demande » |
| Signature demandée (manager) | « Effacer » · **« Renvoyer pour correction »** · **« Approuver et signer »** |
| Signé (tous) | « Rouvrir (annule la signature) » · **« Enregistrer en PDF / envoyer au client »** |

Dans l'attente, le demandeur lit : « Demande envoyée à **<manager>**. En attente de
signature. »

### 1.5 Les règles déjà écrites dans la maquette

- **Contrôle de complétude à la demande de signature** : six champs sont exigés —
  client, réf. pièce, quantité, justification, cause, action corrective. Le
  message affiché est : « Champs obligatoires manquants : client, réf. pièce,
  quantité, justification, cause, action corrective. »
- **Verrouillage** : hors brouillon, tous les champs sont désactivés, sauf la
  section 5 et les deux champs de la section 6 ; le bouton « + Ajouter une ligne »
  disparaît.
- **Signature** : le manager saisit son nom (**« Nom du manager qualité »**) et
  trace une signature sur une zone de dessin (**« Signature (doigt ou souris) »**).
  Les deux sont exigés : « Indiquez votre nom et signez. »
- **Réouverture** : « Rouvrir (annule la signature) » remet au brouillon **et
  efface la signature**.
- **Renvoi pour correction** : remet au brouillon, sans motif saisi.
- **Impression** : la feuille de style d'impression masque l'en-tête, la barre de
  boutons et les encarts ; le titre du document devient `<référence> – Demande de
  dérogation`.

### 1.6 Ce que la présentation ajoute

> La présentation a été **remplacée** le 2026-11-21 à 23:48 par
> `Concessions (1).pptx`. Les citations ci-dessous sont celles de cette version,
> sensiblement plus précise que la précédente sur la liste et sur l'enchaînement.

**Diapositive 2 — l'emplacement, les colonnes, les statuts et les filtres.**
« Créer une section **« Concessions »** au dessous des sections NC qui regroupent
TOUTES les concessions tous produits confondus pour pouvoir suivre celles qui sont
en statut **« Open »**. avec bouton "Add concession" en haut a droite »
« Donc comme pour les NC, grand tableau avec les infos essentielles **(date de
creation, reference, produit, createur, defaut)**, **statuts (acceptée, refusée,
en attente interne, en attente client)** et **piece jointes en dernière colonne**
et des filtres au dessus du tableau sur le **owner (createur de la derog)**, le
**produit**, le **client**, **le statut** »
« Et dans chaque ligne on trouve l'interface html que j'ai envoyée par wtsp »

Trois choses y sont désormais nommées qui ne l'étaient pas : les **colonnes**
(date de création, référence, produit, créateur, défaut, statut, pièces jointes),
les **quatre statuts**, et les **filtres**. Le filtre sur la date de création a
disparu — elle est devenue une colonne — et **`owner` est défini : le créateur de
la dérogation**. La dernière phrase dit que chaque ligne ouvre la fiche de la
maquette.

**Diapositive 3 — ce que fait l'interface.** Reprend le parcours en cinq sections,
la demande de signature avec contrôle des obligatoires et verrouillage, la
signature du manager (approuver ou renvoyer pour correction), puis : « Une fois
signé, le bouton « Enregistrer en PDF / envoyer au client » ouvre l'impression. […]
Le PDF sera téléchargé et prêt à envoyer par mail au client pour décision. »

**Diapositive 4 — la place de la signature et la décision client.**
« Il faut pouvoir **demander la signature au manager qualité juste après l'étape 5
(approbation)**, et **après réception du retour client (généralement par mail) on
revient pour mettre sa décision en étape 6** »
« Pas besoin du nom et de la signature du client, la décision et la date
suffisent, cette partie sera seulement pour la traça interne, sinon la réponse
signée du client sera le PDF signé électroniquement de sa part et retourné par
mail »

La diapositive se termine sur « Dans l'interface : » suivi d'une **capture
d'écran** : c'est la section 6 de la maquette, où se lisent le menu « Décision »
(« En attente ») et le champ **« Nom, date, signature client »**. C'est ce champ
que la phrase précédente demande de retirer.

Cette première phrase est neuve : elle **place le bouton de validation manager
juste après la section 5**, et dit que la décision du client se **saisit à la
main** au retour, en section 6.

### 1.7 Ce que les deux pièces NE couvrent pas

- La **décision du client** n'est pas un état du dossier dans la maquette : une
  dérogation refusée par le client y reste au badge « Approuvée par la qualité ».
  La version remplacée de la présentation donne les statuts du registre, et le
  cycle composé en §0 comble l'essentiel — il reste deux trous, Q27 et Q28.
- Le mot **« Open »** n'est défini nulle part. Lu avec les quatre statuts, il
  désigne ce qui n'est pas une décision finale du client, mais rien ne l'écrit.
- Aucun refus de la part du **manager** : « Renvoyer pour correction » remet au
  brouillon sans motif et sans trace.
- Aucune **pièce jointe** réelle : le champ « Pièces jointes » est une ligne de
  texte, pas un dépôt de fichier.
- Aucun rattachement à un **produit**, à une **NC**, à une **CAPA** ou à un **8D**
  autre que textuel (« Réf. 8D / CAPA »), alors que la liste doit se filtrer par
  produit.
- Aucune **notification**, aucun **courriel**, aucun **lien** : la maquette simule
  tout par un sélecteur de vue.
- Aucune notion de **droits** : qui peut créer, qui peut signer, qui peut rouvrir.
- Aucune **norme** n'est citée dans l'une ou l'autre pièce. Le vocabulaire
  (dérogation, concession, confinement, 8D, CAPA, forme/ajustement/fonction) est
  celui de la qualité automobile et aéronautique, mais rien ne nomme IATF 16949,
  AS9100D ni ISO 9001 §8.7. Aucune clause normative ne peut donc être rattachée
  sans arbitrage.

---

## 2. Le processus, maquette et consigne fusionnées

### 2.1 Le fil

Tout se joue en deux temps séparés par une frontière nette : **un circuit interne
qui s'achève par la signature du manager**, puis **un envoi au client** qui ne
devient possible qu'ensuite.

```
   [Brouillon] --« Validation manager »--> [Signature demandée]
        ^                                        |
        |                                        | signature du manager
        |<---- « Renvoyer pour correction » -----|
        |                                        v
        |<---- « Rouvrir (annule ... ) » ---- [Signé]
                                                 |
                                                 | « Validation client »
                                                 v
                                        [envoyé au client]
                                                 |
                                                 | retour par courriel, saisi à la main
                                                 v
                                        décision portée en section 6
```

Le dernier tronçon — ce qui suit « Validation client » — n'a pas d'état nommé dans
les pièces fournies ; c'est la question ouverte Q11.

### 2.2 Le demandeur, avant la signature

Le demandeur ouvre une concession depuis la liste (**« Add concession »**),
remplit les cinq sections, et choisit le **manager qualité à solliciter** dans la
section 5. Ce choix est une personne de l'annuaire (ADR 0092) : il se fait au
`qos-user-picker`, sa valeur est l'`accountId` (le `sub` du jeton), et il
s'affiche par son nom. Les deux noms en dur de la maquette sont un décor de
démonstration, pas une liste à reproduire.

Il enregistre son brouillon (**« Enregistrer le brouillon »**) autant de fois
qu'il veut, puis demande la signature. Le bouton porte le libellé
**« Validation manager »**. Le contrôle des six champs obligatoires s'applique
(1.5), le formulaire se verrouille, l'état passe à « Signature demandée » et le
badge à « En attente du manager qualité ».

### 2.3 Le manager, ce qu'il reçoit et ce qu'il voit

Deux canaux partent simultanément :

1. **Un courriel** au manager désigné, portant **un lien qui le ramène dans
   QualitOS**, sur le dossier à valider.
2. **Une notification interne**, qui alimente un onglet **« Mes validations »** :
   cet onglet porte **une alerte en rouge avec le nombre** de validations en
   attente.

En entrant dans « Mes validations », le manager voit **le détail de ce qu'il doit
valider**, c'est-à-dire la fiche, **avec l'option de signature**. Il dispose alors
des boutons de la maquette : « Effacer » (le tracé), **« Renvoyer pour
correction »** et **« Approuver et signer »**. Signer exige son nom et un tracé non
vide.

### 2.4 L'initiateur, ce qu'il reçoit en retour

Une fois le dossier signé, **l'initiateur reçoit à son tour une notification**.
Dans son onglet de menu **« Mes notifications »**, il voit une alerte avec le
nombre ; en y entrant il voit **la ligne du dossier validé** ; en cliquant il est
**redirigé vers l'interface de dérogation, le dossier ouvert**.

Le badge est alors « Approuvée par la qualité ».

### 2.5 La frontière interne / client

**Tout ce qui précède se passe en interne.** Aucun courriel ne part vers
l'extérieur, aucun destinataire externe n'est saisi, et le dossier n'est pas
communicable tant que la signature n'est pas acquise.

À partir de l'état signé, et seulement là, **deux champs s'ajoutent au
formulaire : le mail client et un commentaire**. L'envoi au client se fait en
cliquant sur le bouton **« Validation client »**.

Le bouton de la maquette **« Enregistrer en PDF / envoyer au client »** et le
bouton **« Validation client »** décrivent le même moment du processus par deux
gestes différents : l'un télécharge un PDF à envoyer soi-même, l'autre envoie
depuis la plateforme. Les deux pièces et la consigne ne disent pas si l'un
remplace l'autre ou s'ils coexistent — c'est la question ouverte Q10.

### 2.6 Le retour du client

Le client répond **généralement par courriel** (diapositive 4). Quelqu'un revient
dans le dossier et porte sa **décision** en section 6 : « Acceptée », « Acceptée
sous conditions » ou « Refusée », avec sa date. Le nom et la signature du client ne
sont pas requis ; la preuve opposable est le PDF que le client a signé de son côté
et renvoyé par courriel. Cette section est donc de la **traçabilité interne**.

### 2.7 Qui peut quoi — en l'état des pièces

| Geste | Acteur nommé dans les pièces |
| --- | --- |
| Créer une concession, la remplir, enregistrer le brouillon | le demandeur (« Responsable qualité fournisseur ») |
| « Validation manager » | le demandeur |
| « Renvoyer pour correction », « Approuver et signer » | le manager qualité désigné |
| « Retirer la demande » | le demandeur |
| « Rouvrir (annule la signature) » | non attribué — Q13 |
| « Validation client » | l'initiateur |
| Porter la décision du client (section 6) | non attribué — Q14 |

---

## 3. Réutilisation contre nouveauté, brique par brique

Aucun module de dérogation ou de concession n'existe. Mais la quasi-totalité des
mécaniques de ce processus est déjà portée par la plateforme, et la question est
de les brancher, pas de les refaire.

### 3.1 Notifications internes — **on réutilise**

Tout est en place côté serveur et côté client :

- `apps/api-quality-engine/src/main/java/com/openlab/qualitos/quality/notifications/`
  — `Notification` (domaine pur) porte `recipientUserId` (destinataire nommé ou
  diffusion au client), `type` (`INFO` / `SUCCESS` / `WARNING` / `ALERT`), `title`,
  `body`, **`link` (« route interne à ouvrir au clic »)** et `readAt`.
- `.../notifications/web/NotificationController.java` expose déjà
  `GET /api/v1/notifications`, **`GET /api/v1/notifications/unread-count`**,
  `POST /{id}/read` et `POST /read-all`. Tenant et utilisateur viennent du jeton.
- `apps/web/src/app/core/notifications/notifications.service.ts` expose un flux
  `unread$` et les quatre appels.

Le champ `link` couvre exactement l'exigence « quand il clique il est redirigé vers
l'interface de dérogation, le dossier ouvert ».

Le modèle d'appel à suivre est celui du rappel d'audit :
`.../quality/audit/AuditReminderService.java` écrit une notification interne et,
si la brique courriel est active, envoie un message — l'un ne dépend pas de l'autre.

### 3.2 La surface « Mes notifications » — **nouveau (partiellement)**

Il existe **une cloche** dans la barre supérieure
(`apps/web/src/app/layout/main-shell/main-shell.component.html`, menu
`qos-notifications-panel` : titre « Notifications », action « Tout marquer comme
lu », état vide « Aucune notification. »), avec sa pastille de non-lues.

Il **n'existe aucune page ni aucune entrée de menu** « Mes notifications » : la
recherche sur `Mes notifications`, `mes-notifications` et leurs variantes ne
renvoie rien dans `apps/web/src/app`. L'onglet de menu décrit au point 3 de la
consigne est donc à créer, en s'alimentant au service existant.

### 3.3 La surface « Mes validations » — **nouveau**

Aucune surface de ce nom, ni sous un autre nom : ni `Mes validations`,
ni `my-approvals`, ni `pendingApprovals` n'existent dans `apps/web/src/app`.
Aucun écran ne rassemble aujourd'hui « ce que j'ai à valider », tous modules
confondus — la GED à signataires désignés (ADR 0099) n'offre que la fiche du
document, atteinte depuis la liste des documents.

C'est le seul élément véritablement transverse du lot : une file de validations
par utilisateur. Sa portée (ce module seul, ou tous les objets à signer) est la
question ouverte Q15.

### 3.4 Circuit de validation paramétrable — **existe, mais ne couvre pas ce cas tel quel**

`.../quality/circuit/` met en œuvre l'ADR 0080 et l'ADR 0095 : au plus dix étapes,
chacune portant **un rôle** (`roleCode`) et un nombre d'approbations distinctes,
réglables par client, activables ou suspendables, avec un passage (`CircuitRun`)
par objet. Le contrat pour un module métier est minuscule :
`.../circuit/application/ApprovalCircuits.java` — `start(subject, subjectId,
authorId)` et `decide(subject, subjectId, approve, comment)`.

Deux constats :

- `.../circuit/domain/CircuitSubject.java` ne connaît **qu'un seul sujet** :
  `DOCUMENT_VERSION("document-version", Permission.DOCUMENT_APPROVE)`. Le
  commentaire de l'énumération dit comment en ajouter un.
- **Le circuit désigne des RÔLES, pas des personnes.** Le processus décrit
  désigne au contraire **un manager nommé**, choisi à chaque demande. Les deux
  modèles ne se superposent pas : le circuit dit « une personne ayant le rôle X »,
  la consigne dit « cette personne-ci ».

Le précédent exact est la GED (ADR 0099) :
`.../quality/docs/DocumentVersionSignatory.java` stocke un signataire **désigné à
la soumission** — `signerId` (« le compte de l'annuaire, le `sub` du jeton »),
`signerName` recopié, `position` de 1 à 3, `decision` dans
`SignatoryDecision.{PENDING, APPROVED, REJECTED}`, `decidedAt` et `comment`. Et
l'ADR 0099 précise que « la désignation fait le droit de signer ». C'est le modèle
à reprendre, avec un seul signataire. Faut-il **en plus** passer par le circuit
paramétrable, comme la GED le fait pour ses revues par rôles, est la question
ouverte Q16.

### 3.5 Courriel — **brique présente, mais pas celle de l'ADR 0127**

Deux dispositifs coexistent, et ils ne sont pas au même endroit :

- **Par client (ADR 0127)** : `apps/api-core/src/main/java/com/openlab/qualitos/core/mail/`
  — `TenantMailSettings`, `TenantMailSettingsService`, `TenantMailSettingsController`
  (l'ingress `/api/v1/mail-settings`), `MailSenderFactory`, `MailHostPolicy`,
  `SmtpMailProbe`, plus `PartnerFirmMailSettings*`. Le seul consommateur à ce jour
  est `TenantAwareInvoiceMailer` (facturation). Côté écran :
  `apps/web/src/app/features/admin/mail/` et
  `apps/web/src/app/features/admin/pages/mail-settings/`.
- **Dans le moteur qualité** :
  `apps/api-quality-engine/.../audit/SmtpAuditReminderMailer.java`, derrière
  `qualitos.mail.enabled`, qui lit `spring.mail.*` — donc **la messagerie du
  déploiement**, pas celle du client. C'est précisément ce que l'ADR 0127
  corrige, et le moteur n'a pas encore été repris.

Un courriel de dérogation part au nom du client (au manager en interne, au
destinataire externe ensuite) : il relève du dispositif ADR 0127. Comment le
moteur atteint les réglages de messagerie portés par `api-core` n'est tranché
nulle part — question ouverte Q17.

À reprendre quoi qu'il arrive de `SmtpAuditReminderMailer` : **le corps en texte
brut à dessein** (un titre saisi par un utilisateur n'a pas à être échappé en
HTML), et `singleLine()` sur le sujet (injection d'en-tête, OWASP A03).

### 3.6 Signature — **on réutilise le moteur, la question est le périmètre**

La plateforme sait sceller : `.../nonconformity/eightd/infrastructure/EightDSealAdapter.java`
signe en hybride Ed25519 + ML-DSA-65 (`HybridSignatureService`, ADR 0011) **puis**
ancre l'empreinte (`BlockchainAnchorPort.submitRoot`, ADR 0012), avec un contexte
de suite crypto par usage (`SIGN_CONTEXT = "eightd-report"`) pour qu'une signature
de 8D ne puisse pas être présentée comme autre chose.

Le second facteur existe aussi : `.../quality/common/StepUpGuard.java` et
`StepUpAuthentication` (revendications `acr` / `amr` du jeton), avec son rendu
d'erreur dans `GlobalExceptionHandler`. Un seul appelant à ce jour :
`AuditReportController` — `stepUp.require("émettre un rapport d'audit")`.

**Verdict demandé sur §18.2 #5.** Une dérogation signée part à un client, autorise
l'expédition de pièces non conformes et reste opposable : elle présente tous les
traits d'une action critique au sens du §18.2 #5 (« validation rapport, signature
audit »), ce qui appellerait **MFA + ancrage blockchain** sur le geste du manager.
Mais la règle est écrite par énumération, et la dérogation n'y figure pas ; le
décider seul reviendrait à élargir le périmètre d'une règle non négociable sans
arbitrage. C'est la question ouverte Q18, et elle commande aussi le sort du tracé
manuscrit de la maquette (Q19).

À noter : la maquette conserve la signature **comme une image PNG** produite par
une zone de dessin. Aucun module de la plateforme ne stocke de tracé manuscrit ;
les signataires GED signent par leur décision authentifiée, et le 8D par une
empreinte scellée.

### 3.7 Le choix du manager — **on réutilise, et il n'existe pas de hiérarchie**

`apps/web/src/app/shared/user-picker/` fournit `qos-user-picker` et
`qos-user-team-picker` ; `apps/web/src/app/shared/person-name/` affiche par le nom.
L'ADR 0091 (annuaire en lecture restreinte, `GET /api/v1/directory/users`,
`{id, accountId, displayName, email, active}`) et l'ADR 0093 (l'annuaire se
remplit à la première connexion) complètent le tableau.

**Il n'existe aucune notion de manager hiérarchique** : l'annuaire ne porte ni
supérieur, ni équipe de rattachement, ni service. Le manager doit donc être choisi
à chaque demande — ce que fait déjà la maquette — ou introduit comme donnée
d'annuaire, ce qui serait un chantier à part. Qui est le manager par défaut est la
question ouverte Q4.

Le précédent le plus proche est le MOC (ADR 0087) : « titulaires choisis dans
l'annuaire, jamais le demandeur ».

### 3.8 Droits — **on réutilise le catalogue, les actions sont à ajouter**

`.../quality/authz/domain/Permission.java` (ADR 0078) est le catalogue : un code
stable par action (`capa.close`), un module de rattachement, et des rôles par
défaut tirés du §16. `@RequiresPermission` garde les contrôleurs ; l'ADR 0081 règle
la visibilité (`nc.view.all` et ses sœurs : sans le droit, on ne voit que ce qui
nous concerne, et hors de portée répond 404, jamais 403).

Ce processus suppose, au minimum, des actions à nommer pour : **créer** une
concession, **modifier** un brouillon, **demander la validation manager**,
**signer**, **renvoyer pour correction**, **rouvrir** un dossier signé, **envoyer
au client**, **porter la décision du client**, et **voir tout le registre** (par
symétrie avec `nc.view.all` — sans elle, on ne verrait que ses propres dossiers et
ceux qu'on doit signer). Leur découpage exact, leurs codes et leurs rôles par
défaut sont à arbitrer, de même que le module de rattachement — la liste se range
« au-dessous des sections NC », donc vraisemblablement sous `capa` comme les
autres entrées de ce groupe, mais rien ne le dit.

### 3.9 La liste et sa place dans la navigation — **on réutilise le gabarit**

Le groupe de navigation existe :
`apps/web/src/app/layout/main-shell/main-shell.component.ts`, entrée
`nav.non-conformite`, qui contient « NC interne » (`/nc/interne`), « NC externe
(Réclamations) » (`/nc/externe`), « Fournisseurs », « NC fournisseurs », « CAPA »,
« Efficacité CAPA ». C'est là, « au dessous des sections NC », que la section
« Concessions » se rangerait. Chaque entrée porte un `module` qui conditionne son
affichage.

Le gabarit de liste filtrée existe dans tous les modules
(`apps/web/src/app/features/nc/`). Attention au piège connu de ces écrans : les
compteurs codés en dur dans les tests de navigation, et le `loading$.next(true)`
synchrone (NG0100).

### 3.10 Produit et client — **le produit existe, le client non**

- **Produit** : `.../quality/product/domain/Product.java` et `ProductLookup` — un
  filtre « produit » peut donc s'appuyer sur un vrai rattachement, à condition que
  la fiche porte une référence au produit, ce que la maquette ne prévoit pas (elle
  n'a qu'un champ texte « Réf. pièce / désignation »). Question ouverte Q20.
- **Client** : il n'existe **aucun registre de clients**. La réclamation
  (`.../quality/complaints/Complaint.java`) porte `customerName`, `customerEmail`
  (validé `@Email`, 320 caractères) et `customerExternalId` **en champs libres**.
  C'est le précédent le plus proche pour les deux champs « mail client » et pour
  le filtre par client de la liste.

### 3.11 Export PDF — **on réutilise**

`org.apache.pdfbox` est déjà en service dans le moteur :
`.../dashboards/export/infrastructure/PdfBoxDashboardRenderAdapter.java`,
`.../nonconformity/eightd/infrastructure/PdfBoxEightDRenderAdapter.java`,
`.../dmaic/resolution/infrastructure/PdfBoxClaimReportRenderer.java`,
`.../supplier/SupplierNcReportRenderer.java`. Le 8D va plus loin : PDF **+ QR
code** renvoyant à une vérification publique sans jeton
(`.../eightd/web/EightDPublicController.java`, `GET /api/v1/nc/public/8d/{code}/verify`,
qui ne rend que des faits d'intégrité et répond `valid=false` plutôt que 404 pour
interdire l'énumération).

Si une dérogation doit partir au client sous forme de PDF opposable, c'est ce
chemin-là qu'il faut suivre, et non l'impression navigateur de la maquette —
laquelle est un dispositif de démonstration, pas un livrable.

### 3.12 Journal et audit — **on réutilise**

`audit_events` et le journal chaîné du moteur couvrent déjà les transitions
d'état ; chaque module y écrit ses transitions. Rien de neuf, à condition que
chaque geste du processus (demande, signature, renvoi, réouverture, envoi,
décision client) y laisse sa ligne.

### 3.13 Récapitulatif

| Brique | Verdict |
| --- | --- |
| Notification en base, par utilisateur, avec lien et compteur de non-lues | **on réutilise** (`.../quality/notifications/`) |
| Service front de notifications + cloche | **on réutilise** (`core/notifications/`, `main-shell`) |
| Page / onglet « Mes notifications » | **nouveau** |
| Onglet « Mes validations » | **nouveau** (aucun équivalent, sous aucun nom) |
| Circuit de validation par rôles | **existe** (ADR 0080/0095) mais ne modélise pas un signataire nommé — arbitrage Q16 |
| Signataire nommé choisi à la soumission | **on réutilise le modèle** GED (ADR 0099), à instancier |
| Envoi de courriel par client | **brique existe dans `api-core`** (ADR 0127), **pas atteignable depuis le moteur** — Q17 |
| Envoi de courriel depuis le moteur | **existe**, mais sur la messagerie du déploiement (`SmtpAuditReminderMailer`) |
| Signature cryptographique + ancrage | **on réutilise** (`HybridSignatureService`, `BlockchainAnchorPort`) |
| Second facteur | **on réutilise** (`StepUpGuard`) — périmètre à trancher Q18 |
| Choix d'une personne dans l'annuaire | **on réutilise** (`qos-user-picker`, ADR 0091/0092) |
| Manager hiérarchique | **n'existe pas** |
| Catalogue de droits par client | **on réutilise** (`Permission`, ADR 0078/0081) ; actions à ajouter |
| Liste filtrée + place dans la navigation | **on réutilise le gabarit**, entrée à ajouter sous « Non-conformité » |
| Rattachement produit | **on réutilise** (`ProductLookup`) si la fiche le porte |
| Registre de clients | **n'existe pas** (champs libres, modèle `Complaint`) |
| Export PDF, QR de vérification publique | **on réutilise** (PDFBox, modèle 8D) |

---

## 4. Questions ouvertes

Chacune est un point où les pièces et la consigne se taisent ou se contredisent.
Aucune n'est tranchée ici.

**Q1 — Le manager refuse : que se passe-t-il ?**

> **Tranchée.** Le manager **ne refuse pas** : le dossier passe « À modifier » et revient à l'initiateur, qui corrige et le renvoie, jusqu'à signature. Voir le cycle en §0.
La maquette n'offre que « Renvoyer pour correction », qui remet au brouillon.
Un refus définitif (« cette dérogation ne partira pas au client ») n'existe pas.
*Ce que ça change :* un état terminal de rejet interne, ou son absence assumée ;
le libellé d'un éventuel bouton ; ce que devient une fiche abandonnée.

**Q2 — « Renvoyer pour correction » porte-t-il un motif ?**

> **Tranchée. Oui**, et il est obligatoire : sans lui l'initiateur ne sait pas quoi corriger.

Aujourd'hui, le renvoi est muet : le demandeur retrouve son brouillon sans savoir
pourquoi. La GED, elle, exige un rejet **motivé** (ADR 0099).
*Ce que ça change :* un champ de commentaire obligatoire, son affichage au
demandeur, sa conservation dans l'historique.

**Q3 — Le renvoi efface-t-il la trace du passage ?**

> **Tranchée. Non — la trace est gardée**, et elle sort dans la **piste d'audit** : chaque demande, chaque renvoi motivé, chaque signature est un événement journalisé.

Si un dossier fait trois allers-retours, l'historique les garde-t-il ?
*Ce que ça change :* une table d'historique des demandes de validation, ou un
simple état courant.

**Q4 — Qui est le manager par défaut ?**
L'annuaire ne porte aucune hiérarchie. Le demandeur choisit-il librement à chaque
fois ? Y a-t-il un manager paramétré par client (ce qui relèverait de l'admin
tenant), par service, ou par utilisateur ?
*Ce que ça change :* un réglage de client, une donnée d'annuaire, ou rien.

**Q5 — Le demandeur peut-il se désigner lui-même ?**
Le MOC l'interdit explicitement (ADR 0087 : « jamais le demandeur »).
*Ce que ça change :* un contrôle au moment de la demande, et son message.

**Q6 — Le manager désigné peut-il être remplacé en cours de route ?**
Absence, départ, erreur de destinataire. Le demandeur « retire la demande » et
recommence, ou une réaffectation existe-t-elle ?
*Ce que ça change :* un geste de réaffectation, ses droits, sa notification.

**Q7 — Combien de temps le lien du courriel est-il valable ?**
Le lien « conduit dans QualitOS ». S'il mène à une page authentifiée, la question
est celle de la redirection après connexion. S'il devait donner un accès direct,
ce serait un jeton à durée de vie, et un tout autre sujet de sécurité.
*Ce que ça change :* un simple lien d'application contre un jeton signé à
expiration, avec tout ce que le second suppose (révocation, usage unique,
journalisation).

**Q8 — Le courriel au manager contient-il le contenu de la fiche ?**
Un courriel sort du périmètre authentifié. Porter la référence seule, ou le détail
de l'écart et du client ?
*Ce que ça change :* le gabarit du message, et ce qui transite en clair.

**Q9 — Le mail client est-il vérifié, et contre quoi ?**
La réclamation se contente d'un `@Email` sur 320 caractères. Faut-il une
confirmation, une liste de domaines autorisés par client, un rapprochement avec le
« Contact qualité client » déjà saisi en section 1 — qui est lui aussi un champ
texte « Nom, e-mail » et fait doublon ?
*Ce que ça change :* la validation, et peut-être la disparition du doublon.

**Q10 — « Validation client » et « Enregistrer en PDF / envoyer au client » :
un geste ou deux ?**

> **Tranchée.** Un seul geste : « Validation client » **envoie** le courriel avec
> le PDF en pièce jointe. Le téléchargement par impression de la maquette est
> remplacé.

La maquette télécharge un PDF que l'on envoie soi-même ; la consigne décrit un
envoi depuis la plateforme. Les deux boutons existent-ils côte à côte, ou le
second remplace-t-il le premier ?
*Ce que ça change :* la présence de l'envoi sortant, le libellé affiché, et la
nature du livrable (PDF scellé contre impression navigateur).

**Q11 — Quel état porte un dossier envoyé au client ?**

> **Tranchée en partie (§0).** Le registre porte « En attente client ». Restent Q27 et Q28.
Les trois états s'arrêtent à « Signé ». La diapositive 2 parle d'un suivi des
concessions « en statut **Open** », vocabulaire absent de la maquette.
*Ce que ça change :* le nombre d'états, les libellés (qui sont figés dès qu'ils
sont écrits, ADR 0052), le filtre par défaut de la liste, et le raccordement entre
« Open » et les états existants.

**Q12 — Que devient le dossier si le client ne répond pas ?**
Aucune échéance, aucune relance, aucune clôture sans réponse.
*Ce que ça change :* une date d'échéance sur la fiche, un rappel par
ordonnanceur — la plateforme en a le modèle (`AuditReminderService`, ADR 0053) —
ou une absence assumée.

**Q13 — Qui peut rouvrir un dossier signé, et que devient la signature ?**
Le bouton « Rouvrir (annule la signature) » est visible de tous dans la maquette,
et efface le tracé. Si la fiche a déjà été envoyée au client, la rouvrir le
dément ; si elle a été scellée cryptographiquement, le scellé ne porte plus sur
rien.
*Ce que ça change :* le droit attaché, l'interdiction après envoi, et le sort de
la signature — ce qui recoupe Q21.

**Q14 — Qui porte la décision du client ?**

> **Tranchée (diapositive 4).** « Après réception du retour client (généralement par mail) **on revient** pour mettre sa décision en étape 6 » : c'est l'initiateur qui la porte, à la main.
L'initiateur, le manager, n'importe quel membre ? Et la section 6 reste-t-elle
saisissable **avant** l'envoi, comme dans la maquette, où rien ne l'empêche ?
*Ce que ça change :* un droit dédié, et le verrouillage de la section 6 hors du
moment prévu.

**Q15 — « Mes validations » : ce module seul, ou toute la plateforme ?**
L'onglet est décrit pour la dérogation, mais la GED a déjà des signataires
désignés en attente, et le circuit de validation a des étapes ouvertes.
*Ce que ça change :* une file propre au module (petite, immédiate) contre une file
transverse (plus utile, plus large, et qui touche des modules livrés).

**Q16 — La validation manager passe-t-elle par le circuit paramétrable ?**
Le circuit (ADR 0080/0095) désigne des rôles ; la consigne désigne une personne.
Faut-il un signataire nommé seul, comme en GED, ou un signataire nommé **et** un
circuit par rôles réglable par le client, comme la GED le permet aussi ?
*Ce que ça change :* l'ajout ou non d'un `CircuitSubject`, le réglage offert à
l'admin tenant, et la complexité du moteur d'états.

**Q17 — Par quel chemin le moteur qualité envoie-t-il un courriel au nom du
client ?**

> **Le PRODUIT est tranché ; le chemin reste un détail de mise en œuvre.** Le
> message est simple — corps bref, PDF en pièce jointe (22 novembre) — et
> l'expéditeur est l'adresse que règle l'administrateur du client, l'initiateur
> en copie cachée (consigne explicite du 21 novembre). Reste donc à écrire
> COMMENT le moteur lit ces réglages portés par `api-core` : appel entre
> services, ou envoi déplacé. Le choix vaut aussi pour les rappels d'audit, qui
> souffrent du même manque (§5.1), et mérite son ADR.

> *(analyse antérieure, conservée pour mémoire)* L'expéditeur est l'adresse que
> l'administrateur du client règle dans son écran de messagerie — qui existe déjà
> (`admin/pages/mail-settings`, ADR 0127) — et l'initiateur est en copie cachée du
> courriel au client. Aucun écran, aucun réglage, aucune décision ne manque : il
> manque seulement que le **moteur qualité** sache lire ces réglages, qui vivent
> dans `api-core`. Reste donc à choisir entre l'appel entre services et le
> déplacement de l'envoi — et ce choix vaut aussi pour les rappels d'audit, qui
> souffrent du même manque aujourd'hui (§5.1).

> **Devenue bloquante.** Elle se dédouble. Le courriel **au manager** est interne
> — de l'initiateur vers son manager, avant tout envoi au client — et l'expéditeur
> du déploiement peut suffire à le porter. Celui qui part **au client**, lui, sort
> de l'organisation, emporte la dérogation en pièce jointe et doit partir au nom du
> client de la plateforme : c'est précisément ce que règle l'ADR 0127, et ce que le
> moteur ne sait pas faire. Le second envoi est donc bloqué, le premier non.

Les réglages de messagerie par client vivent dans `api-core` (ADR 0127) ; le
moteur n'a qu'un expéditeur de déploiement. Appel entre services, duplication des
réglages, déplacement de l'envoi vers `api-core` ?
*Ce que ça change :* une décision d'architecture à part entière, probablement un
ADR, et elle dépasse le périmètre de ce module.

**Q18 — La signature du manager est-elle une « action critique » au sens du
§18.2 #5 ?**

> **Tranchée. Non.** Signature simple, comme dans la maquette : ni second facteur, ni ancrage. Le `StepUpGuard` ne s'applique pas.

Si oui : second facteur exigé (`StepUpGuard`) **et** ancrage blockchain. La règle
n'énumère pas la dérogation ; l'élargir est un arbitrage, pas une lecture.
*Ce que ça change :* l'exigence d'un jeton `acr` au moment de signer, un scellé
hybride avec son contexte de suite crypto, un ancrage, et une vérification
publique possible.

**Q19 — Garde-t-on le tracé manuscrit de la maquette ?**

> **Tranchée. Oui**, on le garde — avec sa portée : il vaut comme geste interne, pas comme preuve. La pièce probante est le PDF renvoyé signé par le client.

Aucun module de la plateforme ne stocke de signature dessinée ; la GED signe par
la décision authentifiée d'un signataire désigné, le 8D par une empreinte scellée.
Le tracé a une valeur d'usage (il « fait signature » sur un PDF remis au client)
mais aucune valeur probante.
*Ce que ça change :* le stockage d'une image par fiche, sa présence sur le PDF, et
le risque de laisser croire à une garantie qu'un dessin n'apporte pas.

**Q20 — La fiche se rattache-t-elle au produit, à la NC, à la CAPA, au 8D ?**
La liste doit se filtrer par produit, mais la maquette n'a que des champs texte
(« Réf. pièce / désignation », « Réf. 8D / CAPA »). Le référentiel transverse
(§3.6 de CLAUDE.md) voudrait de vrais liens.
*Ce que ça change :* des clés étrangères, un sélecteur de produit, un sélecteur de
CAPA, et la possibilité de remonter d'une CAPA à ses dérogations.

**Q21 — Une fiche signée puis modifiée perd-elle sa signature ?**
Le verrouillage interdit la modification hors brouillon, mais la réouverture
remet tout en jeu, et les sections 5 et 6 restent ouvertes en permanence.
*Ce que ça change :* soit une signature portant sur une empreinte du contenu (toute
modification la casse et se voit), soit une signature portant sur un geste (et la
modification ultérieure passe inaperçue).

**Q22 — Les « Pièces jointes » sont-elles de vrais fichiers ?**
Le champ est une ligne de texte citant « mesures, photos, rapport 8D ». La
plateforme sait déposer des fichiers (GED, preuves CAPA, photos de NC, documents
d'audit) avec dédoublonnage et contrôles d'entrée (ADR 0094).
*Ce que ça change :* un dépôt de fichiers et ses garde-fous, ou un simple renvoi
textuel.

**Q23 — Quelles colonnes et quels filtres pour le grand tableau ?**

> **Tranchée (diapositive 2).** Colonnes : date de création, référence, produit, créateur, défaut, statut, pièces jointes en dernière. Filtres : owner, produit, client, statut. **`owner` = le créateur de la dérogation.** Ce que montre « défaut » est arrêté en §0.
« Les infos essentielles » n'est pas détaillé ; quatre axes de filtre sont nommés
(owner, produit, client, date de création), dont le dernier avec un « si
possible ». Le terme **owner** n'apparaît nulle part dans la maquette : est-ce le
demandeur, le manager désigné, ou un pilote distinct ?
*Ce que ça change :* le modèle de données (un champ « propriétaire » séparé ou
non), les colonnes, et les index de la table.

**Q24 — « Concessions » ou « Dérogations » ?**

> **Tranchée (§0).** « Dérogations », d'après le titre et l'en-tête de la maquette.
La présentation dit « Concessions » pour la section de navigation et
« Add concession » pour le bouton ; la maquette dit « Dérogations clients » en
en-tête, « Demande de dérogation (concession) client » en titre, et préfixe les
références `DER-`. Les libellés sont figés dès qu'ils sont écrits (ADR 0052), et
l'interface est en français.
*Ce que ça change :* le nom de la section, celui du bouton de création, le préfixe
des références, les clés de traduction, et l'URL.

**Q25 — À quelle norme rattache-t-on ce dossier ?**

> **Tranchée. Aucun rattachement.** Le dossier ne cite aucune clause normative, et n'en citera pas.

Aucune n'est citée. Le vocabulaire est celui de l'automobile et de l'aéronautique,
et la maîtrise des éléments de sortie non conformes est traitée par ISO 9001 §8.7,
IATF 16949 et AS9100D — tous trois déjà au catalogue du Standards Hub.
*Ce que ça change :* le rattachement aux clauses, donc la mutualisation des preuves
en audit, et l'apparition du module dans la matrice des exigences du SMI.

**Q26 — Qui, parmi les rôles du §16, fait quoi ?**
« Responsable qualité fournisseur (demandeur) » et « Manager qualité (signataire) »
sont les deux rôles de la maquette. Le premier ne correspond à aucun rôle du §16.
*Ce que ça change :* les rôles par défaut des nouvelles actions du catalogue de
droits, et l'alias éventuel à prévoir.

**Q27 — Quel statut porte une dérogation signée mais pas encore envoyée ?**

> **Tranchée.** Le statut est **« Accepté »** — la signature du manager, encore en interne.
La maquette la badge « Approuvée par la qualité » ; la liste des quatre statuts de
la présentation ne prévoit rien entre « en attente interne » et « en attente
client ». Or c'est précisément l'état où le dossier attend un geste de
l'initiateur, et non d'un tiers. Sans statut propre, il se confond avec l'un des
deux voisins et le registre ne dit plus qui doit agir.

**Q28 — Où se range « Acceptée sous conditions » ?**
La maquette propose quatre décisions client — « En attente », « Acceptée »,
« Acceptée sous conditions », « Refusée » — quand la présentation ne nomme que
« acceptée » et « refusée » au registre. Soit « acceptée sous conditions » se
replie sur « acceptée » et les conditions se lisent dans la fiche, soit elle
mérite son statut. Replier perd au registre une information qui engage
l'expédition.


**Q29 — « Accepté » (manager) et « Acceptée » (client) dans la même colonne.**
Les deux statuts diffèrent d'une lettre et désignent deux choses sans rapport :
l'un dit que la qualité a signé en interne, l'autre que le client a dit oui. Au
registre ils se lisent l'un sous l'autre, et la colonne « statut » cesse d'être
lisible d'un coup d'œil — or c'est tout ce qu'on lui demande. Deux sorties
possibles : distinguer les libellés (« Signé en interne » contre « Acceptée par le
client », par exemple), ou garder les mots tels quels et porter la distinction
ailleurs qu'au texte. Les libellés étant figés (ADR 0052), c'est à trancher avant
d'écrire l'écran.

> **Tranchée : « Approuvé qualité ».** Le choix a été délégué — « si tu veux tu
> peux proposer autre statut pour la validation interne » — et c'est la première
> des deux propositions ci-dessous qui est retenue. Le libellé est désormais figé
> (ADR 0052).

> **Le cycle est validé ; le libellé était à choisir.** Deux propositions, l'une et
> l'autre impossibles à confondre avec la réponse du client :
>
> - **« Approuvé qualité »** *(recommandé)* — c'est le mot de la maquette
>   elle-même, dont le badge dit « Approuvée par la qualité ». Il nomme **qui** a
>   tranché, ce qu'aucun autre statut du registre ne fait, et il se distingue d'un
>   coup d'œil d'« Acceptée ».
> - **« Prêt à envoyer »** — dit **ce qu'il reste à faire** et désigne celui qui
>   doit agir, ce qui est la vraie fonction d'une colonne de statut ; mais il tait
>   la signature, qui est pourtant l'événement du moment.
>
> Les libellés étant figés (ADR 0052), le choix se fait avant l'écran.


**Q30 — Le PDF envoyé au client porte-t-il la signature, et est-il scellé ?**

> **Tranchée. Non scellé.** Ni signature, ni ancrage, ni QR : un document structuré, pas une preuve revérifiable. À ne pas confondre avec Q18, qui porte sur le régime de la signature **interne** du manager et reste ouverte.

Le rapport 8D est signé (Ed25519 + ML-DSA), son empreinte est ancrée, et il porte
un QR de vérification publique. La dérogation part chez un client et autorise une
expédition non conforme : si elle n'est pas scellée, rien ne permettra de
distinguer le PDF émis de sa copie retouchée, ni de rattacher le PDF signé que le
client renvoie à celui qu'on lui a adressé.

**Q31 — Que contient la pièce jointe, et que contient le corps du courriel ?**

> **Tranchée.** Le commentaire est **optionnel** et accompagne le message. La
> pièce jointe est la fiche **conforme à l'interface d'édition** — ses six
> sections telles qu'à l'écran. Reste Q22 : si les « Pièces jointes » deviennent
> de vrais fichiers, partent-ils avec ?

La fiche seule, ou aussi ses pièces jointes (Q22) ? Et le champ **commentaire**
ajouté au moment de « Validation client » : part-il dans le corps du message, ou
reste-t-il une note interne ? Il a été annoncé comme un champ du formulaire, pas
comme le texte du courriel.

**Q32 — Le PDF part dans quelle langue ?**

> **Tranchée.** Choisie **à l'envoi**, entre **français et anglais**. À retenir sur le dossier, puisque le PDF n'est pas scellé et peut être réémis.

La fiche est en français ; un client étranger lit autre chose. Le rapport 8D a
tranché la question en figeant la langue **à l'émission** (ADR 0129) plutôt qu'en
traduisant à la lecture. La même règle vaudrait ici, mais elle n'est pas écrite.

---

## 5. Repéré en chemin, NON demandé — en attente d'arbitrage

Rien de ce qui suit n'est livré, ni même prévu. C'est signalé parce que le travail
d'analyse l'a fait apparaître, et chaque point attend une décision avant d'exister.

1. **L'adaptateur courriel du moteur qualité ignore l'ADR 0127.**
   `.../quality/audit/SmtpAuditReminderMailer.java` envoie les rappels d'audit
   depuis la messagerie du déploiement, alors que l'ADR 0127 pose que chaque
   organisation paramètre la sienne. La dette est antérieure à ce chantier et le
   déborde : elle concerne les rappels d'audit déjà en production.

2. **Une file « Mes validations » transverse serait utile au-delà de ce module.**
   La GED (ADR 0099) a des signataires désignés en attente et les circuits
   (ADR 0080) des étapes ouvertes, sans qu'aucun écran ne dise à un utilisateur ce
   qu'il a à valider. Construire cette file pour la seule dérogation la
   construirait deux fois. Voir Q15.

3. **Le second facteur n'est exigé qu'à un seul endroit du moteur.**
   `StepUpGuard` n'est appelé que par `AuditReportController`. Si la dérogation
   devient une action critique (Q18), la question du périmètre réel de la règle
   §18.2 #5 se posera pour d'autres gestes déjà livrés.

4. **Il n'existe pas de registre de clients.**
   Le nom et l'adresse du client sont ressaisis à chaque réclamation, et le
   seraient à chaque dérogation. Un registre partagé éviterait les doublons et
   permettrait un vrai filtre par client sur les listes. C'est un module, pas une
   option.

5. **« Contact qualité client » et « mail client » font doublon.**
   La section 1 porte déjà un champ texte « Nom, e-mail » pour le contact qualité
   du client ; le processus ajoute un « mail client » à l'étape d'envoi. Les deux
   désignent vraisemblablement la même personne. Voir Q9.

6. **La section 6 reste saisissable à tous les états dans la maquette.**
   Le verrouillage l'épargne explicitement, ce qui permet de porter une décision
   client sur un brouillon jamais envoyé. C'est probablement un effet de la
   maquette plutôt qu'une intention. Voir Q14.
