# QualitOS — Module IoT & Edge Connectivity

> Chapitre de la specification produit, extrait de `CLAUDE.md` pour
> n alourdir que les sessions qui en ont besoin. La numerotation des sections
> est celle de la specification.

---

## 9. Module **IoT & Edge Connectivity** — universalité multi-domaines

> **Promesse du module : "Quel que soit le domaine — usine, hôpital, exploitation agricole, laboratoire, smart city, datacenter — QualitOS s'interface nativement avec les capteurs, équipements et systèmes du terrain."**

C'est le module qui transforme QualitOS d'un QMS classique (déconnecté du shop-floor, comme les concurrents) en un **système nerveux qualité connecté en temps réel** au monde physique.

### 9.1 Pourquoi un module IoT natif

La principale faiblesse identifiée chez les leaders du marché (cf. section 1.2) est la **déconnexion entre le QMS et le terrain**. MasterControl gère les documents, ETQ gère les workflows, mais **ni l'un ni l'autre ne lit la vibration de la machine ou la température du réfrigérateur médical**. La cause-racine d'un défaut reste invisible.

QualitOS comble ce vide par un **IoT Hub natif, multi-protocoles, multi-domaines**.

### 9.2 Domaines couverts (universalité par design)

| Domaine                       | Sources de données IoT typiques                                                                                             | Cas d'usage QualitOS                                                                        |
| ----------------------------- | --------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| **Industrie / Manufacturing** | OPC-UA (PLC Siemens, Schneider, Rockwell), MQTT, Modbus, capteurs vibration/température/pression, MES, SCADA, robots cobots | SPC en temps réel, prédiction de dérive, détection panne, lien défaut→machine               |
| **Santé / Hôpital**           | DICOM (imagerie), HL7 FHIR (EHR), capteurs biomédicaux, frigos pharma, autoclaves, biocompatibilité                         | Surveillance T° médicaments/vaccins, traçabilité dispositifs, détection anomalies cliniques |
| **Pharma / Biotech**          | Bioreactors, chromatographes, spectro, monitoring environnement (T°, HR, particules), Building Management System            | GxP en continu, batch right first time, détection excursion CCP                             |
| **Agriculture / AgriTech**    | Stations météo, capteurs sol (humidité, NPK), drones, GPS tracteurs, capteurs élevage, irrigation connectée, serres         | Traçabilité GlobalG.A.P., HACCP au champ, optimisation phytosanitaire, bien-être animal     |
| **Agro-alimentaire**          | Capteurs T° chaîne du froid, pH, débitmètres, étiqueteuses, RFID lots, systèmes pesage                                      | HACCP CCP en continu, traçabilité lot complète, alertes excursion                           |
| **Laboratoire**               | LIMS, automates de paillasse, balances connectées, thermo-cycleurs, microscopes                                             | ISO 15189, traçabilité analytique, calibration auto                                         |
| **Énergie / Utilities**       | Compteurs, smart grid, capteurs environnementaux, SCADA énergie                                                             | ISO 50001, NERC CIP, surveillance assets critiques                                          |
| **BTP / Smart Buildings**     | BIM, BMS, capteurs structure (béton, acier, déformation), badges chantier, drones                                           | ISO 19650, qualité chantier, sécurité, levée de réserves                                    |
| **Logistique / Transport**    | GPS, RFID conteneurs, capteurs T° transport, ELD (Electronic Logging Devices)                                               | ISO 39001, OTIF, chaîne du froid, qualité service                                           |
| **Datacenter / IT**           | Sondes T°/HR salles, métriques serveurs (Prometheus), APM, logs sécurité                                                    | ISO 27001, ISO 50001 datacenter, SLA, SRE                                                   |
| **Retail / Magasin**          | Caméras, capteurs étagères (poids/présence), sondes T° vitrine, files d'attente vidéo                                       | Audit magasin auto, fraîcheur produits, qualité service                                     |
| **Smart City / Public**       | Capteurs qualité air, bruit, trafic, propreté urbaine                                                                       | ISO 18091, qualité service public, environnement                                            |

### 9.3 Architecture IoT — vue d'ensemble

```
┌──────────────────────────────────────────────────────────────┐
│  Capteurs / Équipements / Systèmes terrain (multi-domaines)  │
│  (PLC, biomed, agro, BMS, GPS, RFID, caméras, LIMS, MES...)  │
└──────────────────────────────┬───────────────────────────────┘
                               │
                  ┌────────────┴────────────┐
                  │                         │
        Protocoles legacy/proprio      Protocoles modernes
        (Modbus, BACnet, KNX,         (MQTT 5, OPC-UA, AMQP,
         Profinet, EtherCAT)          CoAP, HL7 FHIR, DICOM,
                  │                    Sparkplug B, LoRaWAN)
                  ▼                         ▼
        ┌─────────────────────────────────────────┐
        │     QualitOS Edge Gateway (K3s)         │
        │  - Containerisé, multi-arch (ARM/x86)   │
        │  - Buffer local, store-and-forward      │
        │  - Modèles IA embarqués (TFLite/ONNX)   │
        │  - Sécurité : mTLS, signed firmware     │
        └────────────────────┬────────────────────┘
                             │ TLS 1.3 + mTLS
                             ▼
        ┌─────────────────────────────────────────┐
        │     QualitOS IoT Hub (Cloud/On-prem)    │
        │  - Apache Kafka + MQTT broker (EMQX)    │
        │  - Schema Registry (Avro)               │
        │  - Device Registry (provisioning)       │
        │  - Twin / Shadow management             │
        └────────────────────┬────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
    ┌─────────────┐  ┌──────────────┐  ┌──────────────┐
    │ Stream      │  │ Time-series  │  │ AI Service   │
    │ Processing  │  │ Storage      │  │ (anomaly,    │
    │ (Flink,     │  │ (TimescaleDB │  │  prediction) │
    │  Kafka      │  │  + InfluxDB  │  │              │
    │  Streams)   │  │  + Mongo)    │  │              │
    └──────┬──────┘  └──────┬───────┘  └──────┬───────┘
           │                │                  │
           └────────┬───────┴──────────────────┘
                    ▼
         ┌─────────────────────────┐
         │  Quality Engine         │
         │  - SPC en continu       │
         │  - Auto-déclenche NC    │
         │  - Alimente KPIs        │
         │  - Génère évidences     │
         │    blockchain           │
         └─────────────────────────┘
```

### 9.4 Protocoles supportés (V1)

#### Industriels

- **OPC-UA** (IEC 62541) avec PubSub, méthode standard pour PLCs modernes.
- **MQTT 3.1.1 / 5.0** (broker EMQX en interne) — IoT généraliste.
- **Sparkplug B** sur MQTT — modèle de données industriel unifié.
- **Modbus TCP / RTU** — équipements legacy.
- **Profinet, EtherCAT** — via passerelles dédiées.
- **BACnet, KNX, LonWorks** — building/HVAC.
- **CAN-bus / CANopen** — véhicules, automatisation.

#### Santé / Médical

- **HL7 v2 / HL7 FHIR R5** — EHR/HIS.
- **DICOM** — imagerie.
- **IEEE 11073** — dispositifs médicaux personnels.
- **SDC (IEEE 11073-20701)** — interopérabilité équipements bloc opératoire.
- **ASTM E1394 / LIS01-A2** — automates labo.

#### Agriculture / IoT longue portée

- **LoRaWAN** (MAC v1.0.4) — capteurs autonomes longue portée.
- **Sigfox** — capteurs ultra-low-power.
- **NB-IoT / LTE-M** — IoT cellulaire.
- **ISOBUS (ISO 11783)** — engins agricoles.

#### Web / Cloud

- **REST + Webhooks**, **GraphQL**, **WebSocket**, **gRPC**.
- **AMQP 1.0** (RabbitMQ).
- **Kafka Connect** — connecteurs ERP, CMMS, MES.

#### Légerrement / contraints

- **CoAP** (IETF RFC 7252).
- **DTLS** pour le transport sécurisé sur UDP.

### 9.5 QualitOS Edge Gateway

Composant déployable **sur site** (usine, hôpital, ferme, datacenter, magasin) :

- **Distribution** : K3s (Kubernetes léger) sur Linux, multi-arch (ARM64 pour Raspberry Pi/Jetson, x86 pour serveurs industriels).
- **Footprint** : 512 Mo RAM minimum, fonctionne sur appliance type Intel NUC, Siemens IPC, Dell Edge Gateway.
- **Fonctions** :
  - Découverte automatique des équipements (auto-discovery OPC-UA, mDNS, BACnet broadcast).
  - Buffer local (24-72h) pour résilience aux coupures réseau (store-and-forward).
  - Filtrage/agrégation locale pour réduire la bande passante (ex : moyenne 1-min vs flux 10 Hz).
  - **Inférence IA locale** : modèles ONNX/TFLite embarqués pour détection d'anomalies sans aller-retour cloud (cas usage : détection visuelle 5S, alarme T° frigo médical, alerte vibration anormale).
  - **Sécurité** : firmware signé Cosign, TLS 1.3 + mTLS vers le hub, attestation TPM 2.0 si disponible.
  - **Mode déconnecté** : fonctionne 100 % offline, sync différée intelligente.
  - **OTA updates** : mise à jour gérée par GitOps (ArgoCD edge).

### 9.6 Device Registry & Digital Twin

- **Provisioning automatique** des équipements : QR code, certificats X.509 par device, rotation auto.
- **Digital Twin / Device Shadow** : représentation logique de chaque équipement avec son état (modèle Eclipse Ditto ou équivalent open-source).
- **Hiérarchie ISA-95** pour l'industrie (Enterprise → Site → Area → Work Center → Equipment), exposée naturellement aux dashboards.
- **Tagging multi-dimensionnel** : par site, ligne, criticité, norme couverte, propriétaire.
- **Lifecycle complet** : provisioning → exploitation → maintenance → décommissionnement, traçé en blockchain.

### 9.7 IA & IoT — convergence

L'IA exploite massivement les flux IoT pour :

- **Détection d'anomalies multivariées** sur signaux temps réel (Autoencoders, Isolation Forest, LSTM-AE) — au-delà des règles SPC classiques.
- **Maintenance prédictive** (RUL — Remaining Useful Life) — anticipe les pannes avant qu'elles ne génèrent des défauts qualité.
- **Vision industrielle / médicale / agricole** : YOLOv8 sur flux caméra pour détecter encombrements 5S, EPI manquants, défauts visuels produits, mauvaises herbes au champ, détection d'anomalies en imagerie médicale (avec validation médecin).
- **Corrélation cross-domaines** : "La hausse du taux de défaut produit X corrèle à la dérive du capteur de pression Y depuis 6 jours" — analyse causale automatique.
- **Acoustique** (sonomètres, micros industriels) : détection roulements défaillants, anomalies process.
- **Audit automatisé** : un audit 5S peut être pré-rempli par les capteurs (T°, propreté capteur poussière, présence détectée).

### 9.8 Sécurité IoT (zero-trust)

- **mTLS obligatoire** entre Edge Gateway et IoT Hub.
- **Certificats X.509 par device**, rotation 90j max, révocation immédiate possible (CRL/OCSP).
- **Signed firmware** sur les Edge Gateways (Cosign + Sigstore).
- **Network segmentation** : VLAN dédié IoT, ACLs strictes, pare-feu applicatif.
- **Aucune connexion entrante** sur l'Edge Gateway : seul le sortant TLS est autorisé (pattern reverse-tunnel).
- **Audit complet** : chaque commande envoyée à un équipement (write OPC-UA, FHIR PUT, etc.) est journalisée, signée, ancrée blockchain.
- **AI Act compliance** : si l'IA prend une décision sur un système physique critique (ex. : arrêt machine), traçabilité complète + supervision humaine obligatoire.
- **Cyber Resilience Act** : SBOM des Edge Gateways, gestion des vulnérabilités.

### 9.9 Cas d'usage emblématiques

#### Industriel — usine

Capteur vibration cobot → MQTT → Edge Gateway → IoT Hub → Stream Processing → détection anomalie → **création automatique d'une NC + alerte au manager qualité + lien vers la fiche FMEA + déclenchement d'un Cycle PDCA**.

#### Santé — hôpital

Frigo médicaments → sonde T° HL7 → Hub → seuil dépassé → **NC auto + alerte pharmacien + lot médicaments mis en quarantaine + preuve blockchain pour ANSM/ARS**.

#### Agriculture — exploitation céréalière

Capteurs sol + station météo + drone NDVI → Edge Gateway local (4G/LoRa) → Hub → IA agronomique → **alerte stress hydrique + plan d'irrigation + traçabilité GlobalG.A.P. + preuve d'application du cahier des charges Bio**.

#### Pharma — bioréacteur

Sonde pH/O₂/T° → OPC-UA → Hub → batch en cours → **excursion CCP détectée → arrêt batch + NC GxP + investigation + dossier audit FDA pré-rempli + signature 21 CFR Part 11**.

#### Smart Building — datacenter

Sondes salles + métriques serveurs → MQTT/Prometheus → Hub → corrélation IA → **alerte risque thermique + ouverture incident ITSM + lien ISO 27001 §A.7.5 (sécurité physique)**.

#### Logistique — transport pharma

GPS + sonde T° conteneur → NB-IoT → Hub → trajectoire suivie + **alerte excursion T° → quarantaine au déchargement + preuves blockchain pour assurance et BPDG**.

### 9.10 Adaptation aisée à n'importe quel domaine

Le module est conçu pour qu'un nouveau domaine puisse être supporté **sans développement core** :

1. **Adapter de protocole** : si un protocole exotique est nécessaire (ex : un bus propriétaire industriel), il est codé comme un **plugin Java SPI** et déployé sur l'Edge Gateway.
2. **Mapping de données** : un fichier YAML déclare comment les variables du protocole sont mappées dans le modèle QualitOS (point de mesure → KPI cible).
3. **Industry Pack** (cf. §5) : fournit les seuils, normes, processus types du domaine.
4. **Modèles IA pré-entraînés** par domaine : disponibles dans le **Model Hub** (TorchHub interne du tenant), ré-entraînables sur les données locales.

Exemple : ajouter le support d'une exploitation viticole (nouveau sous-domaine agro) prend 2 à 4 semaines pour un intégrateur, sans toucher au cœur de la plateforme.

---

