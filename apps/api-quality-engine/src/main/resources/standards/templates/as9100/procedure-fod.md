# Procédure — Prévention des FOD (Foreign Object Debris) — AS9100D §8.1.4

> Tenant : `{{tenant.name}}` — Version 1.0 — `{{date.today}}`.

## 1. Objet

Prévenir, détecter et éliminer les FOD pouvant compromettre la sécurité du produit aéronautique.

## 2. Définitions

- **FOD** : Foreign Object Debris — tout objet étranger (vis, copeau, chiffon, outil oublié) susceptible de causer un dommage aéronef.
- **FO Damage** : dommage causé par FOD.

## 3. Organisation

- **FOD Manager** désigné par site.
- **FOD Champions** dans chaque atelier critique.
- Comité FOD trimestriel.

## 4. Mesures préventives

- **Shadow boards** pour outils (silhouette + numérotation).
- **Tool count** avant/après chaque opération critique.
- **Tethering** des outils en altitude.
- **Bagage / poches** interdits en zone FOD critique.
- **Marquage zones** FOD critiques au sol.

## 5. Mesures de détection

- Inspection visuelle avant fermeture (peer review obligatoire).
- Endoscopie / fibroscopie pour zones inaccessibles.
- Aspiration / dépoussiérage avant fermeture cellule.
- Caméra QualitOS Vision (YOLOv8 fine-tuné) en assistance.

## 6. Mesures de réponse

- Tout outil ou pièce non retrouvé déclenche un arrêt production immédiat.
- Enquête formelle avec rapport 8D.
- Notification client si livraison déjà effectuée.

## 7. Indicateurs

| KPI | Cible | Fréquence |
|-----|-------|-----------|
| Audit FOD zones critiques | ≥ 1/mois | mensuel |
| FOD escape rate | 0 | continu |
| Formation FOD opérateurs | 100 % à jour | annuelle |

## 8. Audits

Audit FOD inopiné mensuel par le FOD Champion. Audit annuel par auditeur interne AS9100.

## 9. Workflow QualitOS

- Audits FOD enregistrés via module 5S (formulaire dédié).
- NC FOD → CAPA automatique avec lien FMEA.
- Indicateurs en temps réel dans dashboard aéro.
