# Module Parc IoT — Équipements, télémétrie et seuils qui ouvrent une CAPA

[← Retour à l'index](../README.md) · Route : **`/iot`** · Menu : *Opérations › Parc IoT*

## À quoi sert ce module

Le module relie la qualité au **terrain**. Il tient le **registre des équipements** qui
remontent des mesures (sondes de température, de vibration, de pression, d'humidité,
automates, caméras, équipements biomédicaux, stations agro, GTB, passerelles), conserve leur
**télémétrie**, et surtout : il transforme un **franchissement de seuil** en **dossier CAPA**
sans qu'un humain ait à s'en apercevoir.

C'est ce dernier point qui distingue le module d'un simple tableau de capteurs : une dérive de
mesure n'est pas une courbe de plus, c'est un écart à traiter.

## Parcours pas à pas

### 1. Enregistrer un équipement

1. **Ouvrir** `/iot`, puis créer un équipement.
2. Renseigner son **code** (votre repère sur le terrain), son **nom**, son **type**, son
   **protocole** d'ingestion, et au besoin son **emplacement**, sa description et des
   métadonnées libres.
3. L'équipement naît **provisionné** : il est déclaré, mais **n'accepte encore aucune mesure**.

### 2. Le mettre en service, le suspendre, le retirer

Quatre états, et ils décident de ce qui est accepté :

| État | Ce qu'il accepte |
| --- | --- |
| **Provisionné** | rien — déclaré seulement |
| **Actif** | la télémétrie, et elle seule dans cet état |
| **Suspendu** | rien ; la fiche et l'historique restent lisibles |
| **Décommissionné** | rien, définitivement : « l'historique reste consultable, plus aucune mesure ni modification n'est acceptée » |

Une mesure envoyée à un équipement qui n'est pas **actif** est refusée explicitement — elle
n'est jamais enregistrée en silence.

### 3. Lire la fiche d'un équipement

La fiche donne, en tête : le **dernier signal** reçu (ou « n'a jamais émis »), le **total de
mesures ingérées**, le nombre de **métriques** distinctes et le nombre de **seuils** posés.

Deux mentions de vigilance sont calculées pour vous : un équipement en service est déclaré
**muet après une heure sans signal**, et porte la mention **« signal vieillissant » dès trente
minutes**. Un capteur silencieux est une panne de surveillance, pas une absence de problème.

En dessous, les **cent dernières mesures**, toutes métriques confondues : horodatage, métrique,
valeur, unité, et **source** (le protocole par lequel la mesure est arrivée ; `MANUAL` pour un
relevé déposé directement). Une fenêtre temporelle peut être demandée sur une métrique précise ;
l'écran annonce quand il n'affiche que le début de la fenêtre.

### 4. Poser un seuil

C'est le cœur du module. Depuis la fiche de l'équipement, ajoutez un seuil :

- la **métrique** surveillée ;
- une **borne basse**, une **borne haute**, ou les deux — l'écran refuse des bornes inversées et
  exige au moins une des deux ;
- la **criticité** de la CAPA qui sera ouverte (basse, moyenne, haute, critique) ;
- le **responsable** de cette CAPA — il est désigné à l'avance, aucune assignation implicite ;
- une **fiche FMEA** à rattacher (facultatif), pour que l'alerte retrouve l'analyse de risque
  qui l'avait prévue ;
- l'option **« ouvrir aussi un cycle PDCA »** ;
- l'interrupteur **actif / inactif**.

Un seuil posé **sans équipement** s'applique à **tous les équipements** du client pour cette
métrique : c'est la façon de dire « nulle part dans l'usine le réfrigérateur ne dépasse 8 °C ».

## Ce qui se passe quand un seuil est franchi

À l'instant de l'ingestion de la mesure, dans la **même transaction** :

1. Une **CAPA corrective** est ouverte, de source `IOT_ALERT`. Son titre nomme la dérive et sa
   valeur ; sa description énonce le seuil dépassé, l'équipement, la mesure et son horodatage.
2. Si le seuil porte une **fiche FMEA**, la description la cite.
3. Si le seuil le prévoit, un **cycle PDCA** d'amélioration est ouvert et la CAPA le cite.
4. Le responsable du seuil devient le **responsable du dossier**.

**Une seule CAPA active par équipement et par métrique** : tant que le dossier ouvert n'est pas
clôturé, une nouvelle dérive de la même origine ne crée pas de doublon. Sans cette règle, un
capteur qui oscille autour de sa borne produirait des centaines de dossiers en une nuit.

Le dossier se traite ensuite comme tout autre : voir [CAPA](capa.md).

## Comment les mesures arrivent

Deux chemins, et ils ne servent pas le même usage.

- **Relevé direct** — une mesure est déposée sur l'équipement par l'API du moteur qualité
  (`POST /api/v1/iot/devices/{id}/telemetry`). C'est ce chemin que l'écran emploie, et c'est
  celui qui déclenche les seuils.
- **Ingestion depuis le terrain** — un service d'ingestion dédié reçoit les flux des
  installations : **MQTT**, **OPC-UA**, **Sparkplug B**, **Modbus TCP/RTU** (lecture structurée
  décodée par la passerelle locale), **LoRaWAN** (relevés de réseaux type TTN ou ChirpStack) et
  **HL7 FHIR** pour les équipements de santé. Il tient aussi la **hiérarchie ISA-95**
  (entreprise → site → zone → îlot → équipement), le **jumeau numérique** de chaque équipement
  — sa face *rapportée*, alimentée par l'ingestion, et sa face *désirée* — et les **agrégats
  horaires** de télémétrie pour les séries longues.

Le protocole **DICOM** (imagerie médicale) figure dans la liste des protocoles qu'un équipement
peut déclarer, mais **aucun connecteur d'ingestion DICOM n'est livré** : un équipement ainsi
déclaré ne recevra de mesures que par relevé direct.

Pour les sites où la liaison est intermittente, un composant d'inférence **en bordure** (sur
site) tamponne les mesures et les **rejoue dans l'ordre** au retour de la liaison, et fonctionne
entièrement hors connexion.

## Bonnes pratiques

- **Déclarez le seuil avant le capteur.** Un capteur sans seuil ne produit que de l'histoire ;
  c'est le seuil qui produit de l'action.
- **Nommez le responsable au seuil, pas au dossier.** La CAPA s'ouvre souvent la nuit : elle
  doit déjà savoir à qui elle est destinée.
- **Rattachez la fiche FMEA.** Une dérive prévue par l'analyse de risque vaut une dérive
  comprise ; sans ce lien, l'alerte repart de zéro.
- **Surveillez les muets.** Un équipement « sans signal depuis une heure » mérite autant
  d'attention qu'un dépassement de borne.
- **N'empilez pas les seuils sur une même métrique.** Une seule CAPA est ouverte par mesure,
  même si plusieurs seuils correspondent : mieux vaut un seuil juste que trois approximatifs.
- **Un décommissionnement est définitif.** Pour un équipement temporairement arrêté,
  **suspendez-le**.
