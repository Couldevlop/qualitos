# ADR 0125 — Un cabinet figure sur un accord : il doit donc nommer quelqu'un de vérifiable

- **Statut** : accepté
- **Date** : 2026-11-16
- **Portée** : cabinets partenaires (domaine, persistance, API, console plateforme)
- **Revient sur** : la décision de la migration `V11__partner_firms.sql`

## Contexte

La V11 avait tranché, en toutes lettres : « Le minimum : ce n'est pas un client,
il n'a ni facturation ni thème ici. » La fiche d'un cabinet portait donc quatre
champs — nom, numéro d'immatriculation, courriel, téléphone — contre quinze pour
celle d'un client.

Ce raisonnement tenait tant que le cabinet n'était qu'un **porte-consultants** :
un regroupement interne, utile pour couper l'accès de toute une équipe d'un seul
geste.

Il ne tient plus pour une raison précise : **le cabinet figure sur un accord
d'engagement**, c'est-à-dire sur une pièce que le client conserve et qu'un
auditeur peut demander. Or un accord qui désigne « Cabinet Durand » sans forme
juridique, sans adresse et sans numéro de TVA ne désigne **personne de
vérifiable** — alors que nommer qui entre chez le client, de façon opposable,
est tout l'objet de cet accord.

## Décision

**Le cabinet gagne l'identité légale et postale, et rien d'autre.**

Huit colonnes : `legal_name`, `legal_form`, `vat_number`, `address_line1`,
`address_line2`, `postal_code`, `city`, `country_code`.

### Ce que cette décision n'ajoute pas, et pourquoi

Pas de thème, pas de couleurs, pas de logo, pas de slogan. **Un client a un
espace à habiller ; un cabinet n'en a pas.** Des colonnes d'image de marque qui
ne s'afficheraient nulle part se rempliraient quand même, et le jour où un écran
les lirait enfin, il afficherait des valeurs saisies des années plus tôt sans que
personne ait jamais pu les relire. La V11 avait raison sur ce point-là.

### Dans la même table, alors que l'identité d'un CLIENT est à part

La raison de l'ADR qui a produit la V10 était la **fréquence de lecture** :
`tenants` est relu à chaque requête par le routage, la facturation et les accès ;
son identité une fois par session. `partner_firms` n'a pas ce problème — elle est
lue dans la console des cabinets et sur les accords, jamais sur un chemin chaud.
Une seconde table coûterait une jointure et une règle de cloisonnement de plus
pour rien.

### Le code pays est normalisé, pas refusé

« fr » est un code pays juste, écrit en minuscules. Le rejeter apprendrait à
l'utilisateur une règle de forme qui ne sert qu'à la machine, alors que la mise
en majuscules ne perd aucune information. Ce qui **n'est pas deux lettres** reste
refusé, par le DTO *et* par une contrainte en base : normaliser n'est pas
rattraper.

La contrainte est posée aux deux endroits à dessein. Une écriture qui passerait à
côté de l'application — reprise de données, correctif en base — ne doit pas
pouvoir semer un code que les écrans ne savent pas rendre.

### Un champ vidé vaut « pas de valeur »

Une chaîne vide en base se lit « renseigné à vide », ce qui n'est pas la même
chose que « pas renseigné » **sur une mention légale**.

### La création reste courte ; la fiche complète

Déclarer un cabinet demande toujours un repère, un nom et un contact. Les
mentions légales et l'adresse se complètent ensuite dans la fiche. Exiger les
douze champs à la déclaration ferait d'un formulaire long le passage obligé —
alors qu'au moment où l'on déclare un cabinet on a souvent son nom et son
contact, pas encore son numéro de TVA.

### Ce qui se saisit doit se lire

Le détail déplié d'un cabinet porte un bloc **Identité**. Sans lui, les huit
champs auraient été saisissables sans être consultables — et l'on n'aurait jamais
pu vérifier qu'un cabinet est en état de figurer sur un accord.

Ce bloc signale l'**absence totale**, pas le formulaire incomplet : réclamer les
douze champs ferait du rouge la couleur normale de l'écran. Un cabinet joignable
est déjà moins anonyme qu'un nom seul.

L'adresse est assemblée côté composant, pas dans le gabarit : une suite de
conditions imbriquées y produisait des virgules orphelines dès qu'un morceau
manquait, et une adresse qui commence par une virgule se lit comme une erreur
d'affichage.

## Conséquences

- Un accord d'engagement peut désigner un cabinet de façon opposable.
- Les cabinets déjà déclarés restent valides : les huit colonnes sont nulles, et
  leur fiche le dit au lieu de faire semblant.
- Le cloisonnement est inchangé : `partner_firms` ne porte pas de `tenant_id`,
  c'est une donnée de plateforme gardée côté applicatif par le rôle (V11).
- Le numéro de TVA n'est **pas vérifié** auprès d'un service officiel, et
  l'écran le dit — le taire ferait prendre son acceptation pour une validation.

## Vérification

- `PartnerFirmServiceTest` (21) : l'identité complète fait l'aller-retour à la
  création **et** à la modification ; le code pays est mis en majuscules ; les
  douze champs vides deviennent `null`, pas des chaînes vides.
- `firm-edit-dialog.component.spec` : toute l'identité saisie repart ; un champ
  vide part en `null` ; un pays qui n'est pas deux lettres **bloque l'envoi sans
  fermer la fenêtre** — le laisser partir ferait échouer l'enregistrement sur une
  erreur de base que personne ne sait lire, après que la fenêtre a donné
  l'impression que c'était pris.
- `partner-firms.component.spec` : l'adresse s'assemble sans virgule orpheline,
  y compris quand seule la ville est connue ; une fiche sans rien le dit, et se
  tait dès qu'un contact est connu.
- i18n : 8926 unités × 5 langues, `--check` vert.
- ⚠ Les bancs front n'ont **pas** pu être joués localement (Chrome se déconnecte,
  mémoire saturée) : ils le sont en CI.
