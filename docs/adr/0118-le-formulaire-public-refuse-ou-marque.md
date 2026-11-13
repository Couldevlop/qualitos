# ADR 0118 — Le formulaire public refuse ce qui ne peut pas être un prospect, et marque le reste

- **Statut** : Accepté
- **Date** : 2026-11-12
- **Owners** : @Couldevlop
- **Portée** : `apps/site-contact` (`app/soupcon.py`, `app/jeton.py`,
  `app/debit.py`, `app/domaine.py`, `app/api.py`), `apps/site` (`site.js`)
- **S'inscrit dans** : CLAUDE.md §11.1 (A01, A04), §22-9 (aucune donnée
  personnelle en clair dans les journaux)

## Contexte

Le 12 novembre 2026, une demande de démonstration est arrivée par le formulaire
public du site. Elle portait une adresse d'expéditeur chez un fournisseur de
boîtes jetables et, dans le champ de texte libre, un lien vers un domaine sans
rapport dont le chemin usurpait le nom du produit. Le lien était écrit
**`hhtps://`** — avec une faute.

Cette faute n'est pas une maladresse : elle sert à traverser les filtres qui
cherchent `https://` tout en restant cliquable pour un lecteur humain, ou
recopiable à la main. C'est la signature d'un envoi industriel.

Le service tenait déjà quatre défenses, et aucune n'avait de prise :

| Défense                    | Pourquoi elle n'a rien vu                                       |
| -------------------------- | --------------------------------------------------------------- |
| Piège à robots             | Laissé vide — l'automate était réglé sur ce formulaire-ci        |
| 5 demandes/heure et par IP | Un seul envoi                                                   |
| Corps borné à 32 ko        | Le message était court                                          |
| Injection d'en-tête        | Rien dans les en-têtes : la charge était dans le corps du texte |

Autrement dit : rien ne regardait **ce que contenait le message**, et rien ne
mesurait **le temps** passé à le remplir.

## Décision

### 1. Deux verdicts, et un arbitrage asymétrique

Un formulaire public reçoit ce que n'importe qui veut y écrire : aucune règle ne
rendra ce fait faux. On ne cherche donc pas à deviner des intentions, mais à
répondre à deux questions vérifiables.

**Refuser** ce qui ne peut pas être la saisie d'un prospect :

- un lien dans `nom`, `fonction`, `organisation`, `telephone` ou `secteur` ;
- du balisage HTML ou BBCode dans n'importe quel champ — une balise **fermante**
  (`</a>`), une balise ouvrante d'au moins deux lettres (`<script>`), ou une
  balise portant un attribut (`<a href=…`). Le motif exclut volontairement
  `a<b>c` : une comparaison écrite en toutes lettres n'est pas du balisage, et
  ce contrôle-ci **refuse** ;
- trois liens ou plus dans le texte libre ;
- un texte libre qui, lien retiré, ne contient plus dix caractères de prose.

**Marquer, sans refuser**, ce qui est seulement douteux : un ou deux liens dans
le texte libre. Le sujet du courriel est préfixé `[à vérifier]`, et le corps
porte les motifs suivis d'une consigne — n'ouvrir aucun lien.

L'asymétrie est volontaire. Sur un formulaire de prospection, **un faux positif
écarte un client et personne ne l'apprend jamais** ; un faux négatif arrive dans
une boîte où un humain le lit. Le doute ne refuse pas : il avertit. Et
l'avertissement va dans le **sujet**, parce qu'écrit seulement dans le corps il
serait lu après le lien qu'il vise à signaler.

### 2. Le temps de remplissage, mesuré par un jeton signé

Le formulaire demande un jeton en s'affichant et le rend en se soumettant. Le
jeton porte l'instant de son émission, signé en HMAC-SHA256 : le service date la
soumission sans mémoriser quoi que ce soit entre les deux appels. En deçà de
**trois secondes**, refus ; au-delà de **deux heures**, refus avec invitation à
recharger.

C'est la seule chose qu'un automate ne peut pas imiter sans perdre l'intérêt
économique de l'opération. Un horodatage non signé, écrit par le navigateur, se
réécrirait : il suffirait d'annoncer une minute d'ancienneté.

Le secret est **tiré au hasard au démarrage et ne quitte pas la mémoire du
processus**. Il n'y a donc ni secret à déposer dans un coffre, ni secret à faire
tourner, ni secret à fuir. Le prix, assumé : un redémarrage périme les jetons
déjà distribués, et la page ouverte au mauvais moment reçoit un refus explicite
qui lui dit de recharger. Cela suppose **un seul exemplaire du service** —
exactement l'hypothèse que fait déjà le compteur de débit.

Ce `replicas: 1` est donc une **exigence de correction, pas un choix de
capacité**, et le manifeste le dit à l'endroit où quelqu'un changerait ce
nombre : à deux exemplaires, environ un visiteur sur deux verrait « jeton non
reconnu », et les deux plafonds de débit doubleraient sans que rien ne le
signale. Monter en exemplaires suppose d'abord de sortir le secret et les
compteurs du processus.

Contrairement au piège à robots, ce refus est **franc, mais pas bavard**. Le
piège fait semblant d'accepter, parce qu'un piège rempli ne peut venir que d'un
automate et que lui dire qu'il a été repéré ne lui apprendrait qu'à mieux se
cacher. Un jeton manquant ou périmé a en revanche des causes légitimes —
l'onglet resté ouvert, le script qui n'a pas chargé — et le visiteur doit savoir
qu'il suffit de recharger : cette raison-là est **répondue**.

Le refus « soumis trop vite » ne l'est pas. Aucun être humain ne le déclenche,
et le nommer apprendrait à l'automate exactement quoi corriger : attendre trois
secondes. Le visiteur reçoit un refus sans motif et une invitation à réessayer ;
la raison précise reste dans le journal, pour celui qui exploite le service.
C'est la règle générale : **on répond une raison quand elle dit au visiteur quoi
faire, on la journalise quand elle ne sert qu'à l'attaquant.**

La route qui délivre le jeton **n'est pas comptée** dans la limite de débit :
elle n'envoie rien, et la compter ferait tomber le visiteur qui recharge trois
fois avant de se décider.

### 3. Un plafond pour le service entier

La borne par adresse arrête celui qui insiste ; elle ne voit pas un réseau
réparti sur mille adresses, chacune restant sous sa limite. Un plafond global —
soixante demandes par heure par défaut — dit ce que la boîte aux lettres
accepte, toutes adresses confondues. Il est très haut devant le trafic réel :
il ne se voit jamais en exploitation normale, et il borne le pire.

**Il se compte juste avant l'envoi, et non à l'entrée de la route.** Placé en
tête, il se consommait sur des requêtes qui n'envoyaient rien — corps illisible,
piège rempli, jeton absent. Douze adresses et soixante requêtes de rebut,
chacune restant sous la borne par adresse, suffisaient alors à fermer le
formulaire pour tout le monde pendant une heure : la protection devenait le
moyen le moins cher de faire taire le service. Un plafond qui dit ce que la
**boîte** accepte doit compter des **courriels**, pas des requêtes HTTP.

## Ce qui a été écarté, et pourquoi

**Vérifier l'enregistrement MX du domaine de l'adresse.** Cela n'aurait pas
arrêté ce message : un fournisseur de boîtes jetables a un MX parfaitement
valide. Le gain réel se limite aux adresses inventées, pour le coût d'une
dépendance DNS, d'un appel réseau sortant depuis un service public et d'une
latence sur le chemin critique. Le rapport ne le justifie pas.

**Une liste de domaines jetables.** Elle se périme plus vite qu'on ne la
maintient, et son résultat ne serait de toute façon qu'un marquage — que le
filtre de contenu produit déjà, sans liste à tenir.

**Un CAPTCHA.** Il déplace le coût sur le visiteur légitime, dépend d'un tiers
qui voit passer le trafic du site, et la plateforme s'interdit par ailleurs d'en
résoudre. Le jeton obtient le même effet sans rien demander à personne.

**Refuser tout lien dans le texte libre.** Un prospect cite légitimement son
site. Voir l'asymétrie ci-dessus.

## Conséquences

- Le message qui a motivé cet ADR est désormais refusé en 422, et le banc qui le
  rejoue le nomme : `test_le_message_recu_le_12_novembre_2026_est_refuse`.
- Le site demande un jeton au chargement. Un échec de cette requête ne casse
  rien : la soumission part sans jeton, le service la refuse en le disant, et le
  repli par messagerie reste disponible.
- `connect-src 'self'` suffit : la route du jeton est sur la même origine.
- Les journaux ne gagnent **aucun contenu**. Le nombre de soupçons est un compte,
  pas une donnée : il dit si le filtre travaille sans révéler ce qu'on a reçu.
- Couverture du service : 100 % (la CI exige 98 %).

## Ce que cet ADR ne traite pas

Recevoir un message indésirable par un formulaire public **n'indique aucune
compromission** : c'est le fonctionnement normal d'un formulaire public. Le
service ne lit aucune base, ne détient aucun compte et n'a aucun accès au
cluster — il reçoit, met en forme, expédie, et oublie. L'exposition du cluster
est un sujet distinct, à traiter pour lui-même.
