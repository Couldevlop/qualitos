# ADR 0127 — Chaque organisation paramètre sa messagerie

- **Statut** : accepté
- **Date** : 2026-11-18
- **Portée** : messagerie d'un client et d'un cabinet partenaire (domaine,
  persistance, chiffrement des secrets, API, envoi réel, consoles)

## Contexte

La plateforme n'avait qu'**une** messagerie : celle du déploiement, réglée par
variables d'environnement (`spring.mail.*`). Tout en partait — les factures, les
rappels d'audit.

Un client recevait donc ses propres relances depuis une adresse qui n'est pas la
sienne, que ses destinataires ne reconnaissent pas, et qui ne passe pas ses
règles SPF/DKIM. Un cabinet partenaire écrivant à ses clients se présentait sous
l'identité de QualitOS.

## Décision

**Chaque organisation règle la sienne, et personne ne règle celle d'une autre.**

### 1. Le premier secret relisible de la plateforme

C'est le point qui a demandé le plus d'attention, parce qu'il introduit une
catégorie qui n'existait pas ici.

Tous les secrets de la plateforme sont **hachés** : les mots de passe des
comptes vivent chez Keycloak, les clés d'API sont stockées en bcrypt. On ne les
relit jamais, on compare une empreinte — et c'est pour cela que leur vol en base
ne donne rien. L'ADR 0102 lui-même n'épingle que le *nom* d'un fournisseur d'IA
par client ; les clés restent dans l'environnement.

Un mot de passe SMTP doit être **présenté tel quel** au serveur de messagerie.
Il faut pouvoir le retrouver, donc le chiffrer.

La conséquence est que la sécurité du secret ne tient plus à la base mais à **la
clé, qui ne doit jamais s'y trouver** :

- port `SecretCipher`, adaptateur **AES-256-GCM** (§11.1 A02) ;
- **GCM et pas CBC**, parce qu'il *authentifie*. Sans cela, quelqu'un qui écrit
  en base — une injection, un accès de maintenance — pourrait faire pointer le
  mot de passe SMTP d'un client vers ce qu'il veut sans que rien ne s'en
  aperçoive ;
- **nonce neuf** à chaque chiffrement, dérivé de rien : ni du client, ni de
  l'horloge, ni du contenu. Réutiliser un nonce avec la même clé est la faute
  qui casse GCM — elle ne fuite pas seulement les messages, elle laisse *forger*
  des cryptogrammes valides ;
- **format versionné** `v1.<nonce>.<chiffré+étiquette>` dès le premier jour.
  Sans ce préfixe, changer d'algorithme obligerait à deviner le format des
  valeurs déjà en base, et c'est précisément ce qui empêche les migrations
  cryptographiques d'avoir lieu (crypto-agilité, §11.4) ;
- la clé vient de `QUALITOS_SECRET_KEY`, **distincte** des autres secrets de
  déploiement : la partager avec le compte de service Keycloak ferait d'une
  rotation l'une un incident sur l'autre, et une clé qu'on hésite à faire
  tourner est une clé qui ne tourne jamais.

### 2. Sans clé, le coffre REFUSE — et l'application démarre quand même

Deux mauvaises réponses étaient possibles, et on les écarte toutes les deux.

**Refuser de démarrer sans clé** aurait cassé chaque déploiement existant et
chaque poste de développement, pour une fonctionnalité qu'aucun d'eux n'utilise
encore. Une migration qui arrête la production n'est pas une migration, c'est
une panne.

**Stocker en clair faute de clé** aurait été bien pire : la fonctionnalité
marcherait, personne ne remarquerait rien, et les mots de passe des messageries
de tous les clients dormiraient en clair dans une colonne. *Le repli silencieux
sur l'option non sûre est la façon dont ces choses arrivent vraiment.*

Alors l'application démarre, journalise l'absence, et le coffre refuse — en
chiffrement **et en déchiffrement**. Accepter de lire laisserait croire qu'une
messagerie déjà configurée fonctionne encore après la perte de la clé, alors que
ses envois échoueraient un par un sans que l'écran le dise.

### 3. Deux tables, pas une

- `tenant_mail_settings` : donnée de client, isolée par **RLS** ;
- `partner_firm_mail_settings` : donnée de **plateforme**, gardée par le rôle,
  comme `partner_firms` (V11) — un cabinet n'appartient à aucun client, il en
  sert plusieurs.

Une table unique aurait demandé un `tenant_id` nullable, et **une ligne à
`tenant_id` NULL échappe à la politique RLS** : selon l'écriture de la politique
elle devient invisible à tous ou visible à tous. Ni l'un ni l'autre n'est
acceptable pour un mot de passe.

Les dix champs, eux, sont écrits **une seule fois** (`MailAccount` partagé,
`MailAccountAssembler` partagé) : ce qui diffère entre un client et un cabinet,
c'est qui a le droit de lire et d'écrire la ligne, pas ce qu'elle contient.

### 4. Un mot de passe absent n'efface rien

L'API ne renvoie **jamais** le mot de passe, ni même son cryptogramme — le
rendre offrirait à qui lit l'écran une cible à attaquer hors ligne, pour aucun
gain. Le formulaire le réaffiche donc vide.

Prendre ce vide pour un effacement couperait la messagerie du client **à chaque
correction de faute de frappe sur le nom de l'expéditeur**. Vide signifie « n'y
touche pas » ; l'effacer se demande par un bouton distinct — ce qui reste
nécessaire, puisqu'un relais interne authentifie parfois par adresse IP.

### 5. Enregistrer n'est pas vérifier

`verifiedAt` ne se pose que par un **envoi d'essai réellement abouti**, et
s'efface dès que change le serveur, le port, la protection, le compte ou
l'expéditeur. Garder la mention « vérifiée » après un tel changement serait pire
que de ne rien afficher : l'écran affirmerait une chose qu'il n'a pas vérifiée,
et personne ne relancerait l'essai.

Le nom affiché et l'adresse de réponse n'en font **pas** partie : ils ne changent
rien à ce que le serveur accepte, et effacer la preuve pour eux apprendrait à
ignorer le signal.

À l'inverse, un essai **échoué n'efface pas** une vérification antérieure : un
serveur momentanément injoignable ne rend pas faux un essai qui a abouti hier, et
effacer la preuve ferait clignoter l'écran au rythme des incidents réseau.

### 6. L'essai envoie un vrai message, et rend son motif

Ouvrir la connexion ne prouverait pas grand-chose : le serveur peut accepter la
connexion, accepter le compte, puis **refuser l'expéditeur** — c'est le cas le
plus fréquent. On envoie donc un message complet.

L'échec répond **200 avec son motif**, pas une erreur HTTP : un mot de passe
rejeté n'est pas une faute de la requête, c'est son résultat. Le motif porte le
code SMTP (535, 550), parce qu'un « échec » sans motif oblige à deviner entre un
nom de serveur faux, un port fermé, un mot de passe refusé et un pare-feu — et
c'est ce qui fait abandonner le réglage d'une messagerie. Il reste **affiché** à
l'écran, non dans une notification fugace.

### 7. La garde SSRF, et sa tension avec le point 6

Sans règle sur l'hôte, la fonctionnalité serait un **scanner de réseau interne**
offert à tout administrateur de client : pointer la messagerie sur `10.0.0.7:22`
et lire le motif suffirait — « refusée » et « délai dépassé » ne se ressemblent
pas, et la différence dit si l'hôte existe et si le port est ouvert (§11.1 A10).

La particularité est que **le motif détaillé, qui sert honnêtement au point 6,
est aussi ce qui renseigne**. On tranche donc en amont plutôt que sur le
message : en SaaS, un hôte qui résout vers une adresse privée, locale, de
bouclage, de lien-local ou multicast est refusé — `169.254.169.254` comprise, et
c'est voulu. Le refus ne dit **pas** vers quoi l'hôte a résolu.

En **on-premise**, la règle ne s'applique pas : un relais interne y est le cas
normal, refuser `smtp.intranet.local` rendrait la fonctionnalité inutilisable là
où elle sert le plus, et l'exploitant a déjà accès à ce réseau par construction.

**La garde est posée à trois endroits, pas un.** Une première version ne la
mettait que dans l'envoi d'essai, et c'était un trou béant : il suffisait
d'enregistrer un hôte interne, d'activer la messagerie et de **ne jamais appuyer
sur « essai »** — le premier courriel émis faisait alors la connexion qu'on
voulait interdire. Elle est donc :

1. à l'**enregistrement**, pour que le refus se dise là où l'on peut le
   corriger — sinon la faute reviendrait sous la forme d'un envoi de facture
   échoué, des jours plus tard, loin de l'écran ;
2. dans `MailSenderFactory`, **le seul point par lequel passent tous les chemins
   qui atteignent le réseau** — l'essai comme l'envoi réel ;
3. dans la sonde, qui garde son contrôle en amont pour rendre un motif lisible
   plutôt qu'une exception.

Le refus est une `IllegalArgumentException`, donc un **400 avec son motif** : un
serveur interne désigné par un administrateur est une saisie à corriger, pas une
panne, et un 500 ferait croire à un incident de la plateforme.

⚠ **Résidu assumé : le réancrage DNS.** On résout, on valide, puis on se
connecte — un nom dont la résolution change entre les deux pourrait encore mener
à une adresse interne. Le fermer demande d'épingler l'adresse validée et de
rendre l'identité TLS par une fabrique de connexions et un SNI explicites.
On ne le fait pas dans ce lot : mal exécuté, ce changement **affaiblit la
vérification du certificat**, c'est-à-dire qu'il échangerait un canal
d'information étroit — l'attaquant est un administrateur déjà authentifié, et ce
qu'il obtient est un message d'erreur — contre un risque sur le chiffrement de
tous les envois. Consigné ici pour être traité séparément, avec le soin que cela
demande.

### 8. L'écran n'est pas décoratif : l'envoi passe par là

`TenantAwareInvoiceMailer` décore le port d'envoi existant. Sans ce branchement,
un client aurait saisi son serveur, vu « vérifiée », et ses factures seraient
quand même parties de l'adresse de la plateforme — *une messagerie configurée et
sans effet est pire que pas de messagerie du tout, parce qu'elle fait croire le
problème réglé.*

Un **décorateur** plutôt qu'un `tenantId` de plus au port : ajouter un paramètre
aurait obligé chaque appelant à le transmettre, et il suffit qu'un seul l'oublie
pour qu'un courriel parte de la mauvaise organisation. Ici le client vient du
contexte de la requête — il n'y a rien à ne pas oublier.

Et **l'échec ne se rabat pas** sur la messagerie de la plateforme : le message
partirait d'une adresse que le client n'a pas choisie, sous l'identité de
QualitOS, pour un courrier qui l'engage. L'échec remonte, la facture reste
renvoyable.

Un marqueur `PlatformInvoiceMailPort` nomme le repli, là où un `@Qualifier` par
nom aurait dépendu de laquelle des deux configurations — SMTP réglé ou envoi
éteint — se trouve active.

### 9. Qui règle quoi

- La messagerie d'un **client** : son administrateur, et aucune route ne prend
  d'identifiant de client — il vient du jeton (§18.2.2). Une route
  `/tenants/{id}/mail` aurait suffi à laisser un administrateur détourner le
  courrier d'une autre organisation.
- Celle d'un **cabinet** : le super administrateur, dans la console de
  l'éditeur. Un cabinet n'a pas d'administrateur dans QualitOS — ses consultants
  travaillent chez les clients qui les ont acceptés, avec le rôle que ces
  clients leur ont donné.
- **Un cabinet ne règle pas celle de ses clients.** Il peut la demander ; il ne
  l'écrit pas à leur place. C'est pourquoi les deux surfaces sont distinctes et
  non une route paramétrée par le type d'organisation — une seule route aurait
  fini par accepter les deux.

### 10. Des délais d'attente, et STARTTLS exigé

Par défaut, une connexion SMTP qui n'aboutit pas attend **indéfiniment** : un
serveur mal saisi bloquerait le fil d'exécution servant la requête, puis le
suivant, jusqu'à épuiser le pool et emporter l'API — pour une faute de frappe
dans un formulaire. Dix secondes.

`starttls.required` et non `starttls.enable` seul : sans cela, un serveur qui
n'annonce pas STARTTLS fait retomber la connexion en clair **sans rien dire**, et
le mot de passe du client part en clair sur le réseau.

## Conséquences

- Les courriels d'un client partent de son adresse, dès qu'il l'active.
- Un client qui n'a rien réglé garde exactement le comportement d'avant.
- ⚠ **Les rappels d'audit du moteur qualité ne sont pas encore concernés.** Ils
  sont émis par `api-quality-engine`, qui n'a pas accès à ces tables ; les lui
  ouvrir demande soit une route interne, soit de déplacer le réglage. Dette
  consignée, et nommée plutôt que tue : aujourd'hui, les relances d'audit
  partent toujours de la messagerie de la plateforme.
- ⚠ La clé `QUALITOS_SECRET_KEY` doit être posée sur chaque environnement avant
  que la fonctionnalité serve. Sans elle, l'écran répond une erreur franche.
- Une nouvelle règle d'ingress (`/api/v1/mail-settings`) ; celle du cabinet
  passe par `/api/v1/partner-firms`, déjà routée.

## Vérification

- `AesGcmSecretCipherTest` (11) : aller-retour, unicode, **nonce neuf à chaque
  chiffrement**, altération d'un seul *bit* refusée, autre clé refusée, version
  présente, vide refusé, et sans clé le coffre refuse au lieu de stocker en
  clair.
  Un banc a d'ailleurs révélé un défaut **dans le banc** : altérer le dernier
  caractère base64url ne change aucun octet — il porte 4 bits inutilisés — et le
  test passait en ne testant rien. Il altère désormais les octets.
- `TenantMailSettingsServiceTest` (19) : un hôte interne est refusé **dès
  l'enregistrement**, et rien n'est écrit ; le mot de passe est chiffré et jamais
  rendu ; **absent, il n'efface rien** ; l'effacement est explicite ; changer le
  serveur efface la preuve, renommer l'expéditeur la garde ; un échec d'essai
  n'efface pas une vérification antérieure ; une messagerie non activée ne
  détourne pas le courrier.
- `MailHostPolicyTest` (11) : bouclage, plages privées, lien-local et
  `169.254.169.254` refusés ; on-premise autorisé ; **le motif ne dit pas vers
  quoi l'hôte a résolu** ; et la **fabrique** refuse elle aussi — c'est le banc
  qui ferme le contournement par « ne jamais appuyer sur essai ».
- `TenantAwareInvoiceMailerTest` (8) : sans réglage la plateforme envoie, avec
  réglage le client envoie, l'échec **ne se rabat pas**, hors contexte on ne
  devine pas, le sujet est ramené sur une ligne (injection d'en-têtes).
- `PartnerFirmMailSettingsServiceTest` (7) : un cabinet introuvable est refusé
  sur **chaque** chemin.
- `CoreContextLoadsTest` : le contexte monte avec le décorateur et les deux
  envoyeurs — c'est la classe de bug que deux beans concurrents produisent.
- i18n `--check` vert : 8970 unités × 5 langues.
- ⚠ Les bancs front (18) n'ont **pas** pu être joués localement : Karma tombe
  faute de mémoire sur cette machine. Le typage passe ; la CI les joue.
