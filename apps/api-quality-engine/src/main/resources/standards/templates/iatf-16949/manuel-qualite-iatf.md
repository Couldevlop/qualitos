# Manuel Qualité — IATF 16949:2016

> Template QualitOS Standards Hub. Tenant : `{{tenant.name}}` — Site(s) : `{{tenant.sites}}` — Date : `{{date.today}}`.
> Ce manuel se conforme aux exigences IATF 16949:2016 et ISO 9001:2015 (HLS).

## 1. Présentation de l'organisme

- Raison sociale : `{{tenant.legal_name}}`
- Activités automobiles couvertes : `{{scope.products}}`
- Clients OEM : `{{scope.oem_customers}}` (Renault, Stellantis, BMW, Toyota, Ford, GM...)
- Sites de fabrication concernés : `{{scope.manufacturing_sites}}`
- Activités exclues du périmètre IATF (justifier) : `{{scope.exclusions}}`

## 2. Champ d'application du SMQ

Conformément à IATF 16949 §4.3, le SMQ couvre :

- La conception et le développement des produits automobiles (sauf cas d'exclusion §8.3 documentée).
- La production série, le pré-série, les pièces de service et les pièces accessoires.
- Tous les processus d'approvisionnement et de sous-traitance externalisés.

## 3. Politique qualité automobile

Voir document distinct `POL-QUAL-AUTO-001`, validé par la Direction Générale et révisé annuellement.

## 4. Exigences spécifiques clients (CSR)

| OEM | Référence CSR | Version | Date de revue | Statut couverture |
|-----|---------------|---------|---------------|-------------------|
| Renault | Renault SQA | `{{csr.renault.version}}` | `{{csr.renault.date}}` | `{{csr.renault.coverage}}` |
| Stellantis | PSQA / PSA-CSR | `{{csr.stellantis.version}}` | `{{csr.stellantis.date}}` | `{{csr.stellantis.coverage}}` |
| BMW | GS 95014 / GS 95020 | `{{csr.bmw.version}}` | `{{csr.bmw.date}}` | `{{csr.bmw.coverage}}` |
| Toyota | TQM / Jishuken | `{{csr.toyota.version}}` | `{{csr.toyota.date}}` | `{{csr.toyota.coverage}}` |

## 5. Cartographie des processus

- Processus de management : revue de direction, audits internes, amélioration continue (PDCA).
- Processus de réalisation : APQP, conception, fabrication, contrôle, libération produit.
- Processus support : RH/compétences, métrologie/MSA, achats/fournisseurs, maintenance, infrastructure.

## 6. Approche risque et plans de contingence (§6.1.2.3)

Pour chaque processus critique, un plan de contingence documenté décrit :
- Le scénario de défaillance.
- L'impact production estimé.
- Les actions de bascule (transfert de charge, sous-traitance d'urgence).
- Les tests de plan exécutés au minimum une fois par an.

## 7. APQP / PPAP (§8.3 — §8.3.4.4)

Tous les nouveaux projets suivent la méthode APQP en 5 phases (P0–P5). Aucune pièce ne part en série sans un PPAP approuvé client (niveau 1 à 5 selon exigence OEM).

## 8. Plans de contrôle (§8.5.1.1)

Un plan de contrôle est établi pour chaque famille de produit et chaque phase (prototype, pré-série, série). Il identifie les caractéristiques spéciales (sécurité / critique / significative) et les méthodes de mesure associées.

## 9. Méthode 8D (§10.2.3)

Toute réclamation client donne lieu à un rapport 8D : D0 (préparation), D1 (équipe), D2 (description), D3 (containment), D4 (cause racine), D5 (actions correctives), D6 (vérification), D7 (prévention récidive), D8 (clôture & félicitations).

## 10. Validation, signature et ancrage blockchain

Ce manuel est :
- Validé par la Direction Générale (signature électronique 21 CFR 11 compatible).
- Versionné dans le module Document Control de QualitOS.
- Empreinte SHA-256 ancrée sur la chaîne Hyperledger Fabric (cf. registre Standards Hub).

| Version | Date | Auteur | Approbateur | Hash SHA-256 (blockchain) |
|---------|------|--------|-------------|---------------------------|
| 1.0 | `{{date.today}}` | `{{author}}` | `{{approver}}` | `{{hash}}` |
