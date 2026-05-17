# Cadre de gestion du risque lié aux TIC — DORA (UE) 2022/2554 Art. 6

> Tenant (entité financière) : `{{tenant.name}}` — `{{date.today}}`.

## 1. Objet et périmètre

Décrire le cadre de gestion du risque TIC, conforme aux articles 5 à 12 du règlement DORA, applicable à l'ensemble des fonctions opérationnelles supportées par les TIC.

## 2. Gouvernance (Art. 5)

- **Organe de direction** : approuve et supervise le cadre annuellement.
- **CIO / RSSI** : responsable opérationnel.
- **Direction des risques** : intègre le risque TIC dans le cadre global de risque.
- **Audit interne** : revue indépendante annuelle.

## 3. Identification (Art. 8)

- Inventaire des actifs TIC (matériel, logiciel, données, services, fournisseurs).
- Cartographie des fonctions critiques et de leurs dépendances TIC.
- Mise à jour ≥ 1/an et à chaque changement matériel.
- Outil : registre QualitOS Standards Hub + IoT Connectivity.

## 4. Protection et prévention (Art. 9)

- Politique de sécurité de l'information (alignée ISO 27001).
- Contrôles d'accès logiques (RBAC + ABAC).
- Chiffrement des données sensibles (au repos AES-256-GCM, en transit TLS 1.3 + post-quantique X25519+ML-KEM-768).
- Segmentation réseau.
- Sécurité des changements (CAB, validations).
- Sécurité des développements (SAST, DAST, SCA — voir CLAUDE.md §14).

## 5. Détection (Art. 10)

- SIEM corrélant logs applicatifs, infrastructures et IAM.
- SOC 24/7 (interne ou MSSP).
- Surveillance anomalies par IA (UEBA — User & Entity Behavior Analytics).
- Tests de pénétration réguliers (Art. 25).

## 6. Réponse et reprise (Art. 11)

- Plan de continuité TIC documenté.
- RTO et RPO définis par fonction critique (validés métier).
- Sauvegardes testées trimestriellement.
- Plan de gestion de crise multi-acteurs.
- Sites de repli (cloud / DC secondaire).

## 7. Apprentissage et évolution (Art. 12)

- Post-mortem systématique après incident majeur.
- Boucle de retour vers le cadre de risque.
- Indicateurs et tableau de bord trimestriel.

## 8. Tableau de bord (KRI)

| KRI | Cible | Fréquence |
|-----|-------|-----------|
| Vulnérabilités critiques non patchées > 30j | 0 | hebdomadaire |
| Taux d'incidents majeurs / trimestre | tendance baissière | trimestriel |
| Temps moyen de détection (MTTD) | ≤ 24h | mensuel |
| Temps moyen de remédiation (MTTR) | ≤ 4h pour critiques | mensuel |
| Couverture des tests sauvegarde | 100 % | trimestriel |
| Tiers TIC critiques sans clause DORA | 0 | mensuel |

## 9. Approbation et revue

Cadre approuvé par l'organe de direction. Revue annuelle minimum.

| Version | Date | Approbateur | Hash blockchain |
|---------|------|-------------|-----------------|
| 1.0 | `{{date.today}}` | `{{board.chair}}` | `{{hash}}` |
