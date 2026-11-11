# Module Risques & opportunités — Le registre unique

[← Retour à l'index](../README.md) · Route : **`/risques`** · Menu : *Risques & opportunités ›
Registre*

## À quoi sert ce module

C'est le registre que l'ISO 9001 §6.1 — et ses équivalents ISO 14001, ISO 45001, IATF 16949 —
demande : la liste des **risques** à traiter et des **opportunités** à saisir, cotés, décidés,
suivis, et rattachés aux exigences qu'ils couvrent.

Le registre a deux onglets, **Risques** et **Opportunités**, parce que les deux ne se cotent pas de
la même façon. Un risque se cote en **gravité × probabilité** ; une opportunité en **gain ×
faisabilité**.

Cinq types classent les lignes : Qualité, Environnement, SST, Sécurité de l'information,
Exigences légales.

## Parcours pas à pas

### 1. Inscrire un risque

1. **Ouvrir** `/risques`, onglet *Risques*, puis **Nouveau risque**.
2. **Identification** : intitulé, description, cause, effet, **processus**, **site**,
   **propriétaire**, **origine**, et les **exigences concernées** (ISO 9001 · 6.1,
   ISO 14001 · 6.1, ISO 45001 · 6.1, IATF 16949 · 6.1.2).
3. **Cotation brute** : gravité et probabilité, de 1 à 5 chacune. Le score et le niveau sont
   calculés **par le serveur** — l'écran n'en montre qu'un aperçu pendant la saisie.
4. **Traitement** : la décision — *Réduire*, *Accepter*, *Éviter*, *Transférer* —, la **cotation
   résiduelle visée** (facultative, et jamais supérieure à la brute), l'échéance et la **prochaine
   revue**.
5. **Vérification d'efficacité** : le critère est écrit **avant** d'être jugé.

### 2. Inscrire une opportunité

Même identification, mais le reste change : **gain attendu**, **faisabilité**, **bénéfice
attendu**, **contexte et levier**, une **décision** — *À décider*, *Planifier*, *Étudier*,
*Reporter*, *Écarter* — et une **vérification du bénéfice** avec son critère.

Une opportunité ne naît ni d'une AMDEC, ni d'une non-conformité, ni d'un incident, ni d'un
changement : ses origines possibles sont la saisie directe, la revue de direction, l'audit, le
retour client et la veille.

### 3. Traiter

- Un **risque** se traite par une **vraie CAPA** : « Créer une action CAPA » ouvre un dossier
  **préventif** de source `RISK`, avec la référence du risque et une criticité reprise du niveau
  brut. La fiche retrouve ensuite ses dossiers par ce couple — le lien est porté par le dossier
  CAPA lui-même, et aucune table de liaison ne peut le contredire.
- Une **opportunité** porte ses **propres actions** (numérotées ACT-n), parce qu'une opportunité
  n'est pas un écart : les faire passer par la CAPA fausserait les indicateurs du module
  ([délai de clôture, récidive](capa.md)).

### 4. Clore

**Rien ne se supprime** : un risque se **clôt**, une opportunité s'**écarte**. La fiche reste, et
compte comme preuve pour l'exigence à laquelle elle est rattachée.

## Le registre relit ses sources

Un risque peut naître ailleurs dans la plateforme, et le registre ne se contente pas de recopier
ce qu'on lui dit :

| Origine | D'où vient la ligne |
| --- | --- |
| AMDEC | une ligne de [FMEA](fmea.md) dont le RPN atteint le seuil critique du projet, ou dont la priorité d'action est haute |
| Non-conformité | une [NC](non-conformites.md) du registre |
| Incident | un incident [EHS](ehs.md) |
| Changement (MOC) | une demande de [changement](moc.md) |
| Exigence de partie intéressée | une exigence du [registre des parties intéressées](stakeholders.md) |
| Audit | un constat d'[audit](audits.md) — un constat de conformité, lui, n'est pas éligible |

Depuis l'objet d'origine, « Créer un risque » ouvre un formulaire **pré-rempli par le serveur**,
qui est allé relire l'objet dans son module. À l'enregistrement, la référence affichée
(« PFMEA-7 #3 », « NC-2026-0042 ») est **reprise de l'objet relu, jamais de ce que l'écran a
envoyé** : un risque ne peut pas se dire issu d'une AMDEC qui n'existe pas, ni d'un objet d'une
autre organisation. L'origine et la source sont ensuite **figées**. Lorsque la cotation vient
d'une AMDEC, ses notes de 1 à 10 deviennent des notes de registre de 1 à 5 **arrondies au-dessus** :
la conversion ne minore jamais un risque. L'écran signale enfin ce qui est **déjà au registre
depuis cet objet**, pour éviter le doublon qui ferait compter deux fois le même risque.

## Comment lire le résultat

Le niveau se lit sur le score, et le serveur seul le calcule :

| Score | Niveau d'un **risque** | Niveau d'une **opportunité** |
| --- | --- | --- |
| 1 – 4 | Faible | Faible |
| 5 – 9 | Moyen | Moyen |
| 10 – 14 | Élevé | Élevé |
| 15 – 25 | **Critique** | **Prioritaire** |

Le registre se filtre par type, par processus et par statut, se cherche et s'**exporte**.

Les statuts d'un risque sont : *À traiter*, *En traitement*, *Surveillé*, *Accepté*, *Clos*. Ceux
d'une opportunité : *En étude*, *Planifiée*, *En cours*, *Réalisée*, *Écartée*.

Chaque fiche porte son **suivi** — la chronologie lisible de ce qui lui est arrivé — à côté du
**journal d'audit chaîné**, qui en garde la trace opposable **sans aucun texte libre** : ni
intitulé, ni nom de propriétaire n'y figurent.

## Qui peut quoi

La **lecture est ouverte** à tout utilisateur authentifié. L'**écriture** est réservée au manager
qualité, au directeur qualité et à l'administration de l'organisation, et demande le module `risk`
actif — comme l'AMDEC. Après résiliation du module, les lectures restent possibles : un registre
qu'on ne peut plus relire ne serait plus une preuve.

## Bonnes pratiques

- **Cotez le risque brut d'abord**, et ne touchez à la cotation résiduelle que quand le traitement
  est réellement en place. Une résiduelle optimiste écrite d'avance est le défaut le plus courant
  de ces registres.
- **Un risque critique appelle une CAPA préventive**, pas une surveillance. « Surveillé » est un
  statut, pas un traitement.
- **Tenez les prochaines revues** : c'est la date que le [tableau de bord SMI](smi.md) relit pour
  annoncer ce qui est en retard.
- **Écrivez le critère d'efficacité avant la mesure** : un critère posé après coup n'est pas un
  critère, c'est un commentaire.
- **Reliez chaque ligne à son exigence** : c'est ce rattachement qui fait du registre une preuve
  de clause pour le [Standards Hub](standards-hub.md), au lieu d'un tableau de plus.
