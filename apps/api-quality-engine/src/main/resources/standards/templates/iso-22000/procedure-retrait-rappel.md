# Procédure — Retrait & rappel produit — ISO 22000:2018 §8.9.4

> Tenant : `{{tenant.name}}` — Version 1.0 — `{{date.today}}`.

## 1. Objet

Définir la procédure permettant le retrait (avant remise au consommateur) ou le rappel (après remise) d'un lot non conforme ou potentiellement dangereux.

## 2. Définitions

- **Retrait** : retour des produits depuis les distributeurs / clients professionnels avant la mise à disposition du consommateur final.
- **Rappel** : restitution depuis les consommateurs finaux.
- **Niveau 1** : risque vital immédiat.
- **Niveau 2** : risque pour la santé non vital.
- **Niveau 3** : risque mineur (non-conformité étiquetage, qualité organoleptique).

## 3. Cellule de crise

| Rôle | Fonction |
|------|----------|
| Directeur Général | Décision et notification autorités |
| Directeur Qualité | Pilotage opérationnel |
| Responsable Logistique | Récupération physique |
| Responsable Communication | Communication externe |
| Responsable Juridique | Conformité réglementaire |
| Vétérinaire / Microbiologiste (si applicable) | Évaluation risque |

## 4. Workflow

1. **Détection** (NC interne, plainte client, autorité, fournisseur, alerte vigilance).
2. **Évaluation préliminaire** (1h max) : type de danger, lots impactés, niveau de risque.
3. **Décision** de retrait / rappel (Direction).
4. **Notification autorités** (DGCCRF / DDPP / autres) — délais légaux selon niveau.
5. **Notification distributeurs / clients pro** dans les 4h.
6. **Communication consommateurs** (rappel niveau 1 ou 2) :
   - Site Rappel Conso (rappel.conso.gouv.fr).
   - Presse, RS, affichage en magasin.
7. **Logistique de récupération** + isolation produits.
8. **Investigation root cause** (Ishikawa, CAPA).
9. **Évaluation efficacité** (% lots récupérés vs distribués).
10. **Rapport final aux autorités + clôture**.

## 5. Délais cibles

| Action | Délai cible |
|--------|-------------|
| Évaluation préliminaire | 1h |
| Décision Direction | 2h |
| Notification autorités niveau 1 | 4h |
| Notification distributeurs | 4h |
| Communication consommateurs | 24h |

## 6. Exercice annuel (obligatoire)

Simulation au moins une fois par an :
- Choix d'un lot fictif.
- Exécution complète du processus (sans communication externe réelle).
- Mesure : temps d'identification du lot et de ses destinataires, exhaustivité.
- Cible : `{{drill.target_time_hours}}` h pour identifier 100 % des destinataires d'un lot donné.

## 7. Indicateurs

| KPI | Cible |
|-----|-------|
| Exercice annuel exécuté | 1/an |
| Temps de remontée traçabilité | ≤ 2h |
| Taux récupération produits niveau 1 | ≥ 95 % |
| Délai notification autorités | 100 % respect |

## 8. Validation et blockchain

Procédure approuvée par Direction. Toutes notifications autorités sont signées 21 CFR 11 + ancrées blockchain (preuve opposable).
