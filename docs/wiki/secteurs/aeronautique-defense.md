# Secteur — Aéronautique / Défense

[← Retour aux secteurs](README.md) · Pack : **`aerospace`**

## Enjeux qualité du secteur

L'aéronautique, le spatial et la défense exigent une **qualité sans compromis** : taux d'échappées
client extrêmement bas, première inspection d'article (FAI), prévention des corps étrangers (FOD),
maîtrise des procédés spéciaux (NADCAP), lutte contre les pièces contrefaites et gestion de
configuration rigoureuse.

## Normes pertinentes (Standards Hub)

- **AS9100** — Système de management qualité aéronautique.
- **AS9110** — Maintenance / MRO.
- **NADCAP** — Accréditation des procédés spéciaux.
- **ISO 9001** / **ISO 14001** / **ISO 45001** — Socles SMQ / environnement / SST.

## KPIs clés (apportés par le pack)

| KPI | Cible | Sens |
|---|---|---|
| **Escape Rate (PPM)** | < 10 | Défauts atteignant le client (AS9100D §10.2.1.1) |
| **FAI Pass Rate** | ≥ 90 % | Conformité première inspection (AS9102) |
| **Findings FOD / mois** | 0 | Corps étrangers (tolérance zéro zones critiques) |
| **Supplier On-Time Delivery** | ≥ 98 % | Livraison fournisseurs à l'heure |
| **Counterfeit Parts Block Rate** | suivi | Interception pièces contrefaites |
| **NADCAP Audit Pass Rate** | 100 % | Procédés spéciaux |

## Modules QualitOS recommandés

- **[NC](../modules/non-conformites.md)** + **[CAPA](../modules/capa.md)** : FAI, contrefaçon,
  procédés spéciaux (`fai-as9102`, `fod-audit`, `configuration-mgmt`).
- **[Ishikawa](../modules/ishikawa.md)** (`/ishikawa`) : `Pièce contrefaite`, `Échec FAI`,
  `FOD critique`, `Procédé spécial hors tolérances (soudure)`.
- **[5S](../modules/fives.md)** (`/fives`) : programme FOD, shadow board, tool control.
- **Poka-Yoke** : tool tethering en altitude, shadow board avec compteur photoélectrique,
  vision IA détection FOD pré-fermeture, verrouillage cellule si NADCAP expirée.
- **[SPC](../modules/spc.md)** (`/spc`) : maîtrise des procédés critiques.

## Connecteurs IoT typiques

`SAP ERP`, `MES générique`, `PLM Windchill`, `OPC-UA`.

## Exemple de parcours

1. À la réception, une pièce suspectée contrefaite est interceptée → KPI **Counterfeit Block Rate**
   (escalade directe direction + autorité).
2. Une [NC](../modules/non-conformites.md) est ouverte et un [Ishikawa](../modules/ishikawa.md)
   `Pièce contrefaite` cadre l'analyse (source non validée, marché gris…).
3. Une [CAPA](../modules/capa.md) renforce la procédure d'approvisionnement.
4. Un audit [5S](../modules/fives.md) FOD avec **vision IA** sécurise les cellules ; Poka-Yoke
   shadow board déployé.
5. Les KPIs **Escape Rate** et **FAI Pass Rate** alimentent le dossier AS9100 / surveillance OASIS.
