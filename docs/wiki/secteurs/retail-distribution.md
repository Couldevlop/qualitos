# Secteur — Retail / Distribution

[← Retour aux secteurs](README.md) · Pack : **`retail`**

## Enjeux qualité du secteur

Le retail et la distribution (magasins, e-commerce, grande distribution) pilotent la **qualité
produit et l'expérience client** : taux de retours, satisfaction (NPS), conformité des audits
magasin, disponibilité en rayon (OSA), chaîne du froid des produits frais et traitement des
réclamations.

## Normes pertinentes (Standards Hub)

- **ISO 9001** — Socle SMQ.
- **ISO 10002** — Traitement des réclamations clients.

## KPIs clés (apportés par le pack)

| KPI | Cible | Sens |
|---|---|---|
| **Taux de retours produits** | < 5 % | Proxy qualité produit / adéquation client |
| **Retours pour défaut produit** | < 1 % | Isole les défauts qualité (→ NC fournisseur) |
| **NPS** | ≥ 40 | Recommandation et fidélité client |
| **Score audit magasin** | ≥ 90 % | Merchandising, hygiène, sécurité, étiquetage |
| **Taux de rupture en rayon (OSA)** | < 3 % | On-Shelf Availability |
| **Délai de résolution des réclamations** | < 5 j | ISO 10002 |
| **Démarque produits frais** | < 4 % | Pertes (DLC, chaîne du froid) |

## Modules QualitOS recommandés

- **[Réclamations / NLP](../modules/complaints-nlp.md)** (`/complaints-nlp`) : classification et
  sentiment des réclamations clients.
- **[Audits](../modules/audits.md)** (`/audits`) : audits magasin (`audit-magasin`).
- **[NC](../modules/non-conformites.md)** + **[CAPA](../modules/capa.md)** : retours pour défaut,
  rupture chaîne du froid (`gestion-retours`, `traitement-reclamation`).
- **[Ishikawa](../modules/ishikawa.md)** (`/ishikawa`) : `Taux de retours élevé`, `NPS en baisse`,
  `Rupture en rayon (OSA)`, `Rupture chaîne du froid`, `Démarque produits frais`.
- **Poka-Yoke** : alerte de rupture rayon (capteur de poids), blocage en caisse d'un produit
  rappelé, alerte DLC à J-2 en rayon frais, synchronisation prix étiquette ↔ POS.

## Connecteurs IoT typiques

`POS` (caisse), `IoT` (capteurs étagères / sondes T°), `SAP ERP`, `SMS gateway`.

## Exemple de parcours

1. Un capteur d'étagère (IoT) signale une rupture → Poka-Yoke d'alerte réassort ; KPI **OSA** suivi.
2. En parallèle, le NPS baisse ; le [NLP réclamations](../modules/complaints-nlp.md) révèle un
   motif récurrent (files d'attente, ruptures).
3. Un [Ishikawa](../modules/ishikawa.md) `NPS en baisse` cadre l'analyse.
4. Une [CAPA](../modules/capa.md) ajuste le réassort ; un audit magasin (`audit-magasin`) vérifie
   l'application des standards.
5. Les KPIs **NPS**, **score audit magasin** et **délai de résolution réclamations** pilotent
   l'expérience client.
