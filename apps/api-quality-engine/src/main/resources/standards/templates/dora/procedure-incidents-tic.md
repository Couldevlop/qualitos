# Procédure — Gestion et notification des incidents TIC — DORA Art. 17-19

> Tenant : `{{tenant.name}}` — Version 1.0 — `{{date.today}}`.

## 1. Objet

Détecter, classer, traiter et notifier aux autorités compétentes les incidents TIC majeurs, conformément aux articles 17-19 du règlement DORA.

## 2. Définitions

- **Incident TIC** : événement non planifié compromettant la sécurité du système TIC, de réseau, des données, ou la fourniture de services.
- **Incident TIC majeur** : incident dépassant les seuils définis par l'autorité (clients impactés, durée, criticité du service, impact économique / réputationnel, dimension transfrontalière).
- **Cyber-menace significative** : menace nouvelle ou perfectionnée susceptible de causer un incident majeur.

## 3. Workflow

1. **Détection** (SIEM, supervision, signalement utilisateur, partenaire, ESA).
2. **Enregistrement** dans l'outil de gestion d'incidents (QualitOS CAPA + module incident).
3. **Classification** (Art. 18) :
   - Niveau 1 / 2 / 3 / critique selon matrice DORA + critères locaux ESA.
   - Évaluation impact métier (clients, données, services).
4. **Si majeur — Notification initiale** à l'autorité compétente **≤ 4h** après classification (Art. 19).
5. **Confinement et investigation** (ouverture cellule de crise).
6. **Notification intermédiaire** ≤ 72h.
7. **Notification finale** ≤ 1 mois.
8. **Remédiation + actions correctives** (CAPA QualitOS).
9. **Post-mortem** et intégration au cadre de risque (Art. 12).

## 4. Critères de classification (Art. 18 — RTS)

- Nombre de clients / contreparties impactés et / 24h.
- Durée et indisponibilité du service.
- Étendue géographique.
- Pertes de données et leur criticité.
- Impact économique (perte revenu, sanctions).
- Impact réputationnel.
- Effet sur le marché financier.

## 5. Délais réglementaires

| Étape | Délai après classification |
|-------|----------------------------|
| Notification initiale | ≤ 4h (max 24h justifié) |
| Rapport intermédiaire | ≤ 72h |
| Rapport final | ≤ 1 mois |

## 6. Cellule de crise

| Rôle | Responsabilité |
|------|----------------|
| Direction Générale | Activation cellule, décision communication externe |
| RSSI / CIO | Pilotage technique |
| Compliance | Pilotage réglementaire, communication régulateur |
| Communication | Communication interne et externe |
| Juridique | Aspects contractuels / contentieux |
| RH | Communication interne / mobilisation |

## 7. Templates de notification

(modèles XML / API conforme RTS DORA fournis pour transmission automatisée vers ESA)

## 8. Indicateurs

| KPI | Cible |
|-----|-------|
| Délai détection → classification | ≤ 1h pour critique |
| Respect délai 4h notification initiale | 100 % |
| Délai clôture incident majeur | ≤ 30 jours |
| Taux de récidive | tendance baissière |

## 9. Exercice annuel obligatoire

Simulation d'incident majeur ≥ 1/an + test de la procédure de notification (en mode simulation autorité).
