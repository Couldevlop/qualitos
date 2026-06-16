# Secteur — Logistique / Transport

[← Retour aux secteurs](README.md) · Pack : **`logistics`**

## Enjeux qualité du secteur

La logistique et le transport pilotent la **qualité de service de bout en bout** : livraison à
l'heure et complète (OTIF), maîtrise des avaries/casse, intégrité de la chaîne du froid, sécurité
routière de la flotte, exactitude des stocks et traitement des litiges transport.

## Normes pertinentes (Standards Hub)

- **ISO 9001** — Socle SMQ.
- **ISO 45001** — Santé & sécurité au travail (sécurité routière).
- **ISO 14001** — Environnement.

## KPIs clés (apportés par le pack)

| KPI | Cible | Sens |
|---|---|---|
| **OTIF** | ≥ 98 % | Livré à l'heure ET complet |
| **Délai moyen de livraison** | < 48 h | Engagement de délai client |
| **Taux d'avarie / casse** | < 0,5 % | Coût de non-qualité |
| **Conformité chaîne du froid** | ≥ 99,5 % | Expéditions sans excursion T° (suivi IoT) |
| **Incidents sécurité routière / M km** | < 2 | Sécurité de la flotte (ISO 45001) |
| **Exactitude des stocks** | ≥ 99 % | Fiabilité du WMS (comptages tournants) |
| **Délai de traitement des litiges** | < 10 j | Avarie, perte, retard |

## Modules QualitOS recommandés

- **[NC](../modules/non-conformites.md)** + **[CAPA](../modules/capa.md)** : avaries, excursions T°,
  litiges (`preparation-commande`, `gestion-litige`, `controle-chaine-froid`).
- **[Ishikawa](../modules/ishikawa.md)** (`/ishikawa`) : `OTIF dégradé`, `Avarie à la livraison`,
  `Excursion de température`, `Accident sécurité routière`, `Écart d'inventaire`.
- **[Détection d'anomalies](../modules/anomaly.md)** (`/anomaly`) : dérive T° transport, télématique.
- **[PDCA](../modules/pdca.md)** (`/pdca`) : amélioration des tournées et de l'OTIF.
- **Poka-Yoke** : scan obligatoire au picking (contrôle SKU), alerte temps réel d'excursion T°,
  blocage de départ si arrimage non validé, détrompeur d'adressage entrepôt.

## Connecteurs IoT typiques

`GPS`, `RFID` (conteneurs), `IoT` (data loggers T°), `SAP ERP`, `MQTT`.

## Exemple de parcours

1. Un data logger (IoT) signale une excursion T° en transit → Poka-Yoke d'alerte temps réel ; KPI
   **conformité chaîne du froid** impacté.
2. À l'arrivage, le lot est mis en quarantaine ; une [NC](../modules/non-conformites.md) est ouverte.
3. Un [Ishikawa](../modules/ishikawa.md) `Excursion de température` cadre l'analyse (groupe
   frigorifique, pré-refroidissement, quai ouvert…).
4. Une [CAPA](../modules/capa.md) `controle-chaine-froid` traite la cause.
5. Les KPIs **OTIF**, **taux d'avarie** et **conformité chaîne du froid** pilotent la qualité de
   service.
