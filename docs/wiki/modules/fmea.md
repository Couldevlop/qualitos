# Module FMEA / AMDEC — Analyse des modes de défaillance

[← Retour à l'index](../README.md) · Route : **`/fmea`** · Menu : *Opérations › Risques (FMEA)*

## À quoi sert ce module

La **FMEA** (*Failure Mode and Effects Analysis*, AMDEC en français) prend un périmètre — un
procédé, un équipement, un produit — et le parcourt défaillance par défaillance : qu'est-ce qui
peut échouer, quel en serait l'effet, par quelle cause, et qu'est-ce qui l'empêcherait d'arriver
jusqu'au client.

Chaque ligne est cotée sur trois dimensions, et leur produit donne la criticité :

```
RPN = Sévérité × Occurrence × Détection
```

Le module gère les **projets FMEA** généraux. Pour la FMEA de procédé attachée à un produit et
son **control plan**, voir [Produit, PFMEA et Control Plan](produit-pfmea-control-plan.md).

## Parcours pas à pas

### 1. Créer le projet

1. **Ouvrir** `/fmea`, puis **Nouveau projet**.
2. Renseigner son **code** (lettres, chiffres, point, tiret, souligné), son **nom**, son
   **périmètre** et son **seuil de RPN critique** (entre 1 et 1000).
3. Le projet naît en brouillon ; il s'**active** quand l'analyse démarre, se **rouvre** en
   brouillon si elle doit être reprise, et s'**archive** quand elle est close.

Le seuil de RPN critique est le choix structurant du projet : c'est lui qui décide ce que la
plateforme appellera « critique » dans ce périmètre, et donc ce qui déclenchera une action.

### 2. Remplir les lignes d'analyse

**Nouvel item**, puis pour chaque mode de défaillance :

- la **fonction** ou l'étape du processus concernée ;
- le **mode de défaillance** — la manière dont elle échoue ;
- l'**effet** — ce que le client ou l'opérateur constate ;
- la **cause** — ce qui la produit ;
- les **contrôles actuels** — ce qui est déjà en place pour l'empêcher ou la détecter.

Puis la **cotation initiale** : **Sévérité**, **Occurrence**, **Détection**, de 1 à 10 chacune. Le
RPN se calcule.

### 3. Agir, puis recoter

Chaque ligne porte une **action recommandée**, son **échéance**, son **responsable**, les
**actions réalisées** et leur **date de réalisation**. Une fois l'action faite, la ligne se
**recote** : la fiche affiche côte à côte la cotation initiale et la cotation **après action**,
avec son RPN.

C'est cette comparaison qui donne sa valeur à l'exercice : un RPN qui ne bouge pas après action
signale que l'action n'a pas agi sur la bonne dimension.

## Le barème de cotation

**Référentiel de cotation** ouvre les trois échelles — sévérité, occurrence, détection — avec,
pour chaque score de 1 à 10, sa description. L'écran annonce clairement lequel des deux cas
s'applique :

- **Barème de référence** — votre organisation n'a rien redéfini et cote sur l'échelle livrée.
- **Barème de l'organisation** — vous avez adapté le barème, et la date de modification est
  affichée.

Cette distinction n'est pas cosmétique : **deux RPN issus de barèmes différents ne se comparent
pas**, et le lecteur doit pouvoir le savoir sans enquêter.

**Adapter ce barème** remplace une échelle **d'un bloc, de 1 à 10** — jamais une ligne isolée. Un
barème troué ferait coter au jugé exactement là où le barème existe pour l'éviter.
**Rétablir la référence** revient à l'échelle livrée.

Redéfinir le barème est réservé à la **direction qualité** et à l'**administration de
l'organisation**. Le manager qualité en est volontairement exclu : il cote, il ne redéfinit pas
l'échelle sur laquelle il cote. Chaque redéfinition et chaque retour à la référence s'inscrivent au
**journal d'audit chaîné**, avec leur auteur et leur date — devant deux RPN de 120 cotés à six mois
d'écart, un auditeur peut ainsi établir si l'échelle a bougé entre les deux.

## Comment lire le résultat

La liste des projets se filtre par **statut** et par **seuil de RPN critique**, et se cherche. La
fiche d'un projet porte trois chiffres en tête :

| Chiffre | Ce qu'il dit |
| --- | --- |
| **Items** | l'étendue de l'analyse |
| **Items critiques** | combien de lignes dépassent le seuil du projet |
| **RPN max** | le pire point du périmètre |

Une FMEA dont le nombre d'items critiques ne décroît pas d'une revue à l'autre n'est pas une
analyse, c'est un inventaire.

## Bonnes pratiques

- **Cotez la détection sur ce qui existe, pas sur ce qui est prévu.** Un contrôle planifié ne
  détecte rien.
- **Un item critique appelle une [CAPA](capa.md)**, pas une note : c'est le dossier CAPA qui porte
  l'assignation, l'échéance et la vérification d'efficacité.
- **Revoyez la FMEA quand la réalité bouge** : une [non-conformité](non-conformites.md) survenue
  sur un mode de défaillance coté « occurrence 2 » est la preuve que la cotation était optimiste.
  Un [changement](moc.md) majeur appelle la même relecture.
- **Ne redéfinissez le barème qu'avec une décision écrite** : tous les RPN d'avant et d'après
  deviennent incomparables, et c'est le journal qui l'expliquera à l'auditeur.
- **Reliez la FMEA au [registre des risques](risques-opportunites.md)** : la FMEA traite le risque
  technique de défaillance, le registre porte le risque au niveau du système de management.
