# ADR 0138 — La dérogation client se valide en interne avant de partir

- **Statut** : accepté
- **Date** : 2026-11-22
- **Portée** : module `derogation` du moteur qualité, surface d'envoi de courriel
  dans `api-core`, jeu de droits associé
- **S'appuie sur** : l'ADR 0081 (hors de portée répond 404), l'ADR 0127
  (messagerie par client), l'ADR 0134 (jeu de caractères des polices standard),
  et la règle 18.3 de `CLAUDE.md` (architecture en quatre couches)

## Contexte

Un fournisseur qui livre une pièce hors spécification ne peut pas décider seul
de la livrer quand même : il demande au client une **dérogation**, c'est-à-dire
l'autorisation d'expédier un lot qui s'écarte du plan. La pièce part ou ne part
pas selon la réponse, et la réponse engage les deux parties — c'est une
correspondance contractuelle, pas une note interne.

Le besoin décrit un parcours en deux temps, et c'est sa charnière qui structure
tout le reste :

> « Tout ce processus se passe en interne. À ce stade, l'initiateur peut envoyer
> la demande au client. »

Autrement dit : rien ne sort de l'organisation avant qu'un manager qualité ait
signé. Le document qui part au client porte cette signature ; c'est ce qui le
rend opposable.

## Décision

### 1. Le cycle a sept états, et un seul fait sortir le dossier

```
Brouillon ──demande──▶ En attente interne ──signature──▶ Approuvé qualité
    ▲                        │                                │
    │                        ▼ renvoi (motif obligatoire)      │ envoi au client
    └─ retrait ◀───── À modifier ──────┘                       ▼
                                                      En attente client
                                                             │
                                               ┌─────────────┴─────────────┐
                                               ▼                           ▼
                                           Acceptée                    Refusée
```

**Le manager ne refuse pas** : il signe ou il renvoie pour correction. Refuser
serait usurper la décision du client, qui est seul à pouvoir accorder ou non la
dérogation. Un renvoi exige **un motif** — sans lui, l'initiateur ne sait pas
quoi corriger et le cycle tourne à vide.

**Rouvrir annule la signature.** Un dossier approuvé peut être repris tant qu'il
n'est pas parti : la signature est alors effacée et le dossier redevient un
brouillon. Une fois parti au client, il **ne se rouvre plus** — le client détient
un document signé, et le modifier en silence le démentirait sans qu'il en sache
rien.

### 2. Les invariants vivent sur l'agrégat, jamais dans le service

Chaque geste porte sa garde d'état **et** sa garde d'acteur, dans la méthode du
domaine qui l'exécute :

| Geste | Qui |
| --- | --- |
| Modifier, demander la signature, retirer | l'initiateur |
| Signer, renvoyer pour correction | le manager **désigné sur ce dossier** |
| Envoyer au client, porter sa décision | l'initiateur |
| Rouvrir | l'initiateur ou son manager désigné |

Un service qui contrôlerait cela de l'extérieur laisserait un second appelant les
contourner. Le droit de la console (`derogation.approve`) dit qui peut signer
**des** dérogations ; l'agrégat dit qui peut signer **celle-ci**. Les deux sont
nécessaires, et aucun ne remplace l'autre.

**Conséquence sur le jeu de droits** : `derogation.approve` est accordée
largement, et volontairement. Le dossier désigne son manager et l'agrégat
vérifie le signataire ; restreindre le droit laisserait un dossier confié à un
compte hors pilotage **insignable par quiconque**. Le même raisonnement impose
que `derogation.send` soit accordée au même ensemble que `derogation.create` :
l'agrégat n'accepte que l'initiateur, donc un dossier ouvert par un rôle qui
n'aurait pas le droit d'envoyer serait inenvoyable par tout le monde. Un banc
d'essai vérifie cette cohérence sur l'énumération elle-même, parce que ce
cul-de-sac ne lève ni à la compilation ni aux tests de domaine.

Pour la même raison, la **lecture** porte son propre droit (`derogation.view`) :
elle s'appuyait d'abord sur le droit de créer, si bien qu'un manager désigné qui
n'est qu'auditeur voyait le compteur de ses validations puis se faisait refuser
le dossier qu'il doit signer.

### 3. Six champs obligatoires, et seulement à la demande de signature

Le brouillon s'enregistre **incomplet, autant de fois qu'on veut** : c'est ce que
décrit le besoin. Les six champs — client, réf. pièce, quantité, justification,
cause, action corrective — ne sont exigés qu'au moment de demander la signature,
et le refus porte **la liste** des manquants, pour que l'écran les marque plutôt
que d'afficher une phrase à relire. Les colonnes correspondantes sont donc
nullables en base ; les rendre obligatoires aurait interdit d'enregistrer un
brouillon.

### 4. Le courriel part de la messagerie du client, et `api-core` l'envoie

Les réglages de messagerie par client (ADR 0127) vivent dans `api-core`, base
`qualitos`. Le moteur travaille sur `qualitos_quality` et **ne peut pas les
lire**. C'est donc `api-core` qui envoie, sur une surface dédiée que le moteur
appelle en **propageant le jeton de l'appelant** — aucun jeton de service n'est
forgé.

Pourquoi la propagation plutôt qu'un jeton de service : le client vient ainsi du
jeton et non du corps de la requête (règle 18.2 #2). Pour un envoi sous
l'identité d'une organisation, le dire dans le corps serait une usurpation à une
requête près. C'est le choix déjà retenu en sens inverse par
`QualityEngineActivationAdapter`, et il évite un secret de plus à faire tourner.

La surface d'envoi n'est **pas** un relais générique, et le contrôle est au point
d'envoi — pas dans l'omission du chemin dans l'ingress, qui n'est pas un contrôle
d'accès : destinataire unique et vérifié (ni retour chariot, ni virgule, ni
point-virgule), corps en texte brut, pièce jointe vérifiée PDF **sur ses octets**
et bornée, adresse en copie cachée tirée du jeton, et **refus explicite** quand la
messagerie du client n'est pas configurée — jamais de repli sur la boîte de la
plateforme, qui ferait partir un courrier contractuel sous une autre identité.

Le courriel adressé au **manager**, lui, est interne : son échec est avalé et
journalisé. Perdre une demande de validation parce qu'un relais SMTP tousse
serait l'inverse de ce qu'on veut, l'alerte étant déjà posée dans l'application.

### 5. L'ordre de l'envoi : inscrire d'abord, envoyer ensuite

Le dossier est enregistré comme parti **avant** que le courriel ne parte. Dans
l'autre ordre, un échec d'enregistrement laisserait un client détenir un document
signé pour un dossier que la plateforme croirait encore interne — l'écart le plus
coûteux que ce module puisse produire.

### 6. Le document reprend les six sections, et rien d'autre

PDF non scellé, langue figée au dossier (FR/EN) au moment de l'émission. Les
valeurs sont **repliées**, jamais tronquées : une référence pièce peut faire cinq
cents caractères, et la couper changerait l'information que le client lit. Les
symboles hors WinAnsi (« ≤ », « ≥ », « ≈ », « ≠ », « ⌀ ») sont **translittérés**
et non remplacés par « ? » : « Ø12 ±0,02 ≤ 5 µm » deviendrait « ?12 ?0,02 ? 5
?m », c'est-à-dire une autre cote. Le pied de page ne porte que la pagination :
rien d'inventé sur un document que lit un tiers.

### 7. Deux formes de lien, et elles ne sont pas interchangeables

Le courriel porte une adresse **absolue**, la notification en application un
**chemin** — l'écran la suit avec `router.navigateByUrl`, où une adresse absolue
n'est pas une navigation mais une route introuvable. L'adresse publique vient de
la configuration (`WEB_BASE_URL`), jamais d'un en-tête de requête : la déduire de
`Host` ou `X-Forwarded-Host` laisserait un appelant choisir le domaine d'un lien
qui part par courriel à un collègue.

## Conséquences

- Le module est le premier écrit d'emblée sur les quatre couches de la §18.3 :
  le domaine se teste en JUnit nu, sans base, sans jeton et sans serveur.
- `CORE_BASE_URL` et `WEB_BASE_URL` deviennent des clés de déploiement
  **obligatoires** pour le moteur : la passerelle échoue fermé, et désigner un
  manager demande son nom à l'annuaire.
- Une dette reste ouverte : la maquette montre le bouton « Rouvrir » à tous les
  utilisateurs. La garde posée ici (initiateur ou manager désigné) est plus
  stricte que la maquette, parce que c'est le seul geste qui détruit quelque
  chose. À confirmer en recette.
