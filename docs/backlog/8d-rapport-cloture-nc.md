# Différé — extraire un rapport 8D à la clôture d'une non-conformité

**Demandé le** 12 septembre 2026, et **explicitement différé** par le commanditaire
(« pour plus tard »). Cette fiche existe pour que la demande ne se perde pas, et
pour que le lot suivant ne reparte pas de zéro.

## Ce qui est demandé

À la clôture d'une non-conformité, produire un rapport **8D** téléchargeable.

Gabarit fourni : `docs/D8_Pump_Leakage_Analysis.pdf` — les huit disciplines, leur
mise en page, le niveau de détail attendu.

## Ce qui existe déjà et servira

| Discipline | Source dans QualitOS |
| --- | --- |
| D1 — Équipe | **Rien** : à saisir, ou à tirer du cercle de qualité concerné |
| D2 — Description du problème | La NC elle-même : titre, constat, zone, photos, gravité |
| D3 — Endiguement immédiat | **Rien** : l'endiguement CAPA existe (V-CAPA), à relier |
| D4 — Cause racine | Ishikawa et les 5 Pourquoi, déjà reliés à la NC |
| D5 / D6 — Actions correctives et mise en œuvre | La CAPA escaladée depuis la NC, avec ses actions et leurs preuves |
| D7 — Prévention de la récurrence | Poka-Yoke, PFMEA, plan de surveillance |
| D8 — Reconnaissance de l'équipe | **Rien** : à saisir à la clôture |

Le rendu signé est acquis : le moteur d'export PDF signé ML-DSA + ancrage existe
déjà (dossier de certification, rapports d'audit, plans de surveillance scellés).
Rien à réinventer côté signature.

## Ce qui reste à décider

1. **Document figé ou vue recalculée ?** La clôture plaide pour un document figé —
   donc une preuve, scellée comme un plan de surveillance (ADR 0062), avec son
   empreinte. Une vue recalculée changerait après coup, et un 8D qui change n'est
   plus un 8D.
2. **D1, D3 et D8 n'ont pas de source.** Les saisir à la clôture allonge un geste
   qu'on veut court. Une alternative : les rendre optionnels et marquer le rapport
   « partiel » tant qu'ils manquent — ce qui est plus honnête qu'un document aux
   cases vides.
3. **Sur quelles NC ?** Un 8D pour chaque écart mineur serait une charge inutile.
   Probablement : sur demande, et automatiquement sur les NC critiques ou celles
   qui ont donné lieu à une CAPA.

## À lire avant de commencer

- `docs/D8_Pump_Leakage_Analysis.pdf` — le gabarit
- ADR 0062 — l'empreinte et le scellement d'un document de preuve
- `NcService.close` — le point d'accroche naturel
