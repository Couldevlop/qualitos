# Registre des prestataires TIC tiers — DORA Art. 28 / 30

> Tenant : `{{tenant.name}}` — `{{date.today}}`.

## 1. Objet

Tenir le registre des accords contractuels avec les prestataires tiers de TIC, conformément à l'article 28 et aux clauses minimales obligatoires de l'article 30 DORA.

## 2. Information par contrat

Pour chaque accord :

| Champ | Description |
|-------|-------------|
| Identifiant prestataire (LEI si possible) | — |
| Raison sociale, pays, groupe | — |
| Fonction TIC fournie | — |
| Critique pour fonctions critiques ? | Oui / Non |
| Type de service (laaS / PaaS / SaaS / hébergement / dev / consulting) | — |
| Données accessibles (catégorie, classification) | — |
| Localisation traitement / stockage | — |
| Sous-traitants chainés (Art. 30 §2 a) | Liste avec autorisation préalable |
| Notation criticité (1-5) | — |
| Date début contrat | — |
| Date fin contrat | — |
| Date dernière revue | — |
| RTO / RPO contractuel | — |
| SLA disponibilité | — |

## 3. Clauses contractuelles minimales DORA (Art. 30 §2-3)

- [ ] Description complète et précise des services.
- [ ] Lieux des prestations (UE / hors UE) + traitement des données.
- [ ] Disponibilité, authenticité, intégrité et confidentialité.
- [ ] Niveaux de service garantis (SLA).
- [ ] Notification des incidents TIC + délais.
- [ ] Mécanismes de coopération avec l'entité financière.
- [ ] Droits d'accès, d'inspection, d'audit (entité + ESA + autorités).
- [ ] Stratégies de sortie et plans de transition (Art. 30 §3 e).
- [ ] Plan de réponse / continuité du prestataire.
- [ ] Sécurité de l'information (alignée ISO 27001 / SOC 2).
- [ ] Gestion des sous-traitants en cascade.
- [ ] Résolution des conflits, juridiction et droit applicable.

## 4. Évaluation préalable (Art. 29)

Avant signature d'un nouveau contrat :
- Due diligence prestataire (référentiel public, financial health, géopolitique).
- Évaluation du risque de concentration (% de fonctions critiques chez le même prestataire).
- Évaluation du risque de substituabilité (alternatives crédibles).
- Évaluation conformité réglementaire (RGPD, NIS 2, secret bancaire / médical).

## 5. Suivi continu

- Évaluation périodique (au moins annuelle pour les contrats critiques).
- KPI fournisseur intégré au dashboard Standards Hub.
- Renouvellement / sortie planifiés ≥ 6 mois avant échéance.

## 6. Reporting régulateur

- Registre soumis à l'autorité compétente (ACPR, BCE, ESMA, EIOPA) sur demande.
- Soumissions périodiques selon ESA.
- Hash blockchain de chaque soumission (preuve d'envoi opposable).
