# Module MOC — Gestion des changements

[← Retour à l'index](../README.md) · Route : **`/changes`** · Menu : *Opérations › MOC*
Administration : **`/admin/moc`** · Menu : *Administration › Paramètres MOC*

## À quoi sert ce module

Le **MOC** (*Management of Change*) encadre toute modification délibérée d'un procédé, d'un
équipement, d'un produit ou d'une organisation. Son objet n'est pas de ralentir le changement,
mais de **répondre avant** à une question : qu'est-ce que cette modification touche, et qui doit
l'avoir validée avant qu'elle ne soit mise en œuvre.

Chaque demande reçoit un code attribué par le serveur, de la forme **`MOC-AAAA-NNN`**, séquentiel
par organisation et par année.

## Parcours pas à pas

### 1. Déposer la demande

1. **Ouvrir** `/changes`, puis **Nouvelle demande**.
2. Décrire la modification, sa **justification** et, si elle est connue, la **date de mise en
   œuvre prévue** (facultative — elle alimente les échéances du
   [tableau de bord SMI](smi.md)).
3. La demande naît en **Brouillon**. Seul un brouillon se supprime ; brouillon et analyse se
   modifient.

### 2. Analyser l'impact — les huit domaines

**Envoyer en analyse**, puis évaluer **les huit domaines** :

Sécurité · Santé et environnement · Qualité · Production · Maintenance · Coûts · Réglementaire ·
Formation.

Chacun se note **Aucun (0)**, **Faible (1)**, **Moyen (2)** ou **Élevé (3)**.

Le score est le rapport des niveaux à leur maximum, pondéré par domaine et exprimé en pourcentage :

```
score = arrondi( Σ(niveau du domaine × poids du domaine) / Σ(3 × poids du domaine) × 100 )
```

Tant qu'un domaine n'est pas évalué, il n'y a **pas de score** et la classe affichée est
« Non évalué ». Un domaine de poids 0 doit être évalué, mais ne compte pas.

### 3. Soumettre à validation

La soumission **exige les huit domaines évalués** : le bouton reste bloqué et la raison est dite à
l'écran.

Elle est également refusée, en nommant le rôle concerné, si un rôle de validation requis n'a
**aucun titulaire**, ou n'a **pour titulaire que le demandeur** — la séparation des tâches n'est pas
négociable.

### 4. Valider, ou refuser

À la soumission, **une carte par rôle requis** est créée et figée avec le libellé du rôle :
renommer ou réordonner les rôles plus tard ne change ni les demandes en cours de validation, ni
l'historique.

- Seul un **titulaire du rôle** statue sur sa carte, et les titulaires sont relus **au moment de la
  décision** — un remplacement ou un départ est donc pris en compte sans rien reprendre.
- **Jamais le demandeur**, même s'il est titulaire du rôle. Un utilisateur titulaire de plusieurs
  rôles statue sur **chaque carte séparément**.
- **Un refus exige un commentaire**, et **un seul refus rejette** la demande. Toutes les cartes
  approuvées l'approuvent.
- Une demande rejetée peut être **rouverte en analyse** : ses cartes sont recréées à la soumission
  suivante, et l'historique garde chaque décision passée.

### Le cycle de vie, en une ligne

```
Brouillon → En analyse → En validation → Approuvée
                              ↘ Refusée → (réouverture) → En analyse
```

## Comment lire le résultat

La **classe** de la demande découle de son score, selon les seuils de l'organisation (par défaut
25 et 50) :

| Score | Classe | Ce que cela change |
| --- | --- | --- |
| < 25 | Mineur | les rôles de validation de base |
| 25 – 49 | Majeur | un niveau de validation supplémentaire |
| ≥ 50 | Critique | la validation la plus large |

Les rôles de validation sont des **fonctions de votre organisation** — par défaut Responsable de
service, HSE, Directeur technique, Directeur de site — et non les rôles d'habilitation de la
plateforme. Par défaut : Mineur → Responsable de service et HSE ; Majeur → en plus le Directeur
technique ; Critique → en plus le Directeur de site. Chaque transition et chaque décision s'écrit
**à l'historique de la demande** (le plus récent en haut) et au **journal d'audit chaîné**.

## Notifications et rappels

- À la soumission, chaque titulaire d'un rôle requis reçoit **une** notification listant ses rôles.
  Le demandeur n'en reçoit pas ; il est prévenu de la **décision finale**, avec le motif du refus.
- Les notifications « à valider » devenues sans objet sont marquées lues : on ne demande pas de
  statuer sur une demande déjà rejetée.
- Un **rappel** part vers les titulaires des cartes encore en attente, après le délai réglé par
  l'organisation (3 jours par défaut), puis se répète. Il cesse dès que la carte est décidée ou que
  la demande quitte la validation.
- Le courriel réutilise la brique d'envoi du [rappel d'audit](audits.md), **inactive par défaut** :
  sans serveur SMTP configuré, les notifications dans l'application fonctionnent et aucun courriel
  ne part.

## Ce que l'administrateur du client règle

`/admin/moc` est réservé à l'administrateur de l'organisation, **en lecture comme en écriture**.
Il y fixe :

- les **seuils** majeur et critique ;
- le **poids** de chaque domaine (entier de 0 à 10, 1 par défaut ; au moins un poids non nul) ;
- le **délai de rappel** (1 à 30 jours) ;
- les **rôles de validation**, leurs **titulaires** — choisis dans l'annuaire, principal puis
  suppléants — et les **classifications qui les exigent**.

Les autres utilisateurs ne voient que les valeurs **appliquées** à une demande : le poids affiché
près d'un domaine lorsqu'il diffère de 1, et la liste des validations requises. Une organisation
qui n'a rien réglé utilise les valeurs livrées.

## Bonnes pratiques

- **Évaluez les huit domaines honnêtement** : c'est le score qui décide du niveau de validation, et
  sous-évaluer un domaine revient à choisir soi-même ses validateurs.
- **Tenez les titulaires à jour** dans `/admin/moc` : une demande ne peut pas être soumise si un
  rôle requis n'a personne, et le rappel s'adresse aux titulaires du moment.
- **Reliez le changement à ses suites** : un changement majeur ou critique appelle en général une
  revue de [FMEA](fmea.md), une mise à jour de la [documentation](documents-ged.md) et une entrée
  au [registre des risques](risques-opportunites.md), qui sait citer un changement comme origine.
- **Refusez avec un motif utile** : il sera la seule matière de la réouverture.
