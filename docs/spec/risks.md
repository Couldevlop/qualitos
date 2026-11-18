# QualitOS — Risques majeurs & mitigation

> Chapitre de la specification produit, extrait de `CLAUDE.md` pour
> n alourdir que les sessions qui en ont besoin. La numerotation des sections
> est celle de la specification.

---

## 21. Risques majeurs & mitigation

| Risque                                 | Probabilité  | Impact        | Mitigation                                                                                    |
| -------------------------------------- | ------------ | ------------- | --------------------------------------------------------------------------------------------- |
| Adoption faible faute de pédagogie     | Moyenne      | Critique      | Formation/gamification dès P3, onboarding guidé, success manager                              |
| Complexité IA → effet boîte noire      | Haute        | Élevé         | Explicabilité systématique (SHAP, citations RAG), human-in-the-loop                           |
| Hallucination LLM sur conseils qualité | Haute        | Élevé         | RAG strict + guardrails + disclaimer + validation humaine                                     |
| Coût infra (LLM, blockchain)           | Moyenne      | Élevé         | Modèles open-source on-prem, blockchain ancrage horaire, tarification par module              |
| Migration crypto post-quantique        | Faible court | Critique long | Crypto-agility, CBOM tenu à jour                                                              |
| RGPD sur blockchain                    | Moyenne      | Critique      | **Jamais** de données personnelles on-chain, hashes uniquement                                |
| Verrouillage Keycloak                  | Faible       | Moyen         | Abstraction `IdentityProvider`, OIDC standard, alternatives Authentik/Ory                     |
| Multi-secteur trop ambitieux           | Moyenne      | Élevé         | Industry Packs livrés par vagues (P3 puis P4), framework testé sur 3 secteurs avant extension |
| Pricing perçu comme cher               | Moyenne      | Élevé         | Modèle transparent, free tier généreux, comparaison frontale avec MasterControl/ETQ           |

---

