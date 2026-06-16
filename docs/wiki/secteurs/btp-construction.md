# Secteur — BTP / Construction

[← Retour aux secteurs](README.md) · Pack : **`construction`**

## Enjeux qualité du secteur

Le BTP pilote la **qualité d'ouvrage et la sécurité chantier** : levée des réserves à la réception,
conformité du PPSPS, sécurité (taux de fréquence/gravité des accidents), avancement et qualité de
la maquette **BIM** (ISO 19650), gestion des non-conformités d'exécution et du DOE.

## Normes pertinentes (Standards Hub)

- **ISO 19650** — BIM / gestion de l'information.
- **ISO 9001** — Socle SMQ.
- **ISO 45001** — Santé & sécurité au travail.
- **ISO 14001** — Environnement.

## KPIs clés (apportés par le pack)

| KPI | Cible | Sens |
|---|---|---|
| **Taux de réserves levées** | ≥ 95 % | Qualité de finition à la livraison (PV réception) |
| **Délai moyen de levée de réserve** | < 30 j | Réactivité après réception |
| **Taux de fréquence accidents (TF1)** | < 10 | Accidents avec arrêt / million d'heures (ISO 45001) |
| **Taux de gravité (TG)** | < 0,5 | Sévérité des accidents |
| **Conformité PPSPS** | ≥ 95 % | Respect du plan sécurité chantier |
| **Avancement maquette BIM** | ≥ 90 % | Complétude objets BIM (ISO 19650, LOD/LOIN) |
| **NC chantier ouvertes** | < 10 | Non-conformités d'exécution non soldées |

## Modules QualitOS recommandés

- **[NC](../modules/non-conformites.md)** + **[CAPA](../modules/capa.md)** : réserves, NC
  structurelles, accidents (`levee-reserves`, `inspection-securite`).
- **[Ishikawa](../modules/ishikawa.md)** (`/ishikawa`) : `Réserves de réception nombreuses`,
  `Accident du travail`, `Non-respect PPSPS`, `Maquette BIM incomplète`, `Béton hors résistance`.
- **[5S](../modules/fives.md)** + **[Audits](../modules/audits.md)** : inspections sécurité chantier.
- **[PDCA](../modules/pdca.md)** (`/pdca`) : pilotage planning et amélioration.
- **Poka-Yoke** : détection de clash BIM bloquante, contrôle d'accès chantier par badge
  (habilitation), détrompeur de coffrage, vision IA détection EPI manquant.

## Connecteurs IoT typiques

`BIM`, `IoT` (capteurs structure), `SAP ERP`, `SharePoint`. Badges chantier, drones, BMS.

## Exemple de parcours

1. Une caméra (vision IA) détecte l'absence de casque en zone à risque → alerte sécurité.
2. Une [NC](../modules/non-conformites.md) est ouverte ; un [Ishikawa](../modules/ishikawa.md)
   `Non-respect PPSPS` cadre l'analyse (sous-traitant non sensibilisé, protections manquantes…).
3. Une [CAPA](../modules/capa.md) traite la cause ; un **Poka-Yoke** (badge avec vérification
   d'habilitation) sécurise l'accès.
4. À la réception, les réserves sont suivies (`levee-reserves`) → KPI **réserves levées**.
5. Les KPIs **TF1/TG**, **conformité PPSPS** et **avancement BIM** alimentent le pilotage qualité
   et sécurité.
