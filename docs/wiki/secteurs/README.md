# Guides par secteur d'activité

[← Retour à l'index](../README.md)

QualitOS est **adaptable à tout secteur** : aucune logique métier n'est codée en dur, tout passe
par les **Industry Packs** (packs sectoriels). Cette section regroupe un **guide utilisateur par
secteur** : enjeux qualité, normes pertinentes (du [Standards Hub](../modules/standards-hub.md)),
KPIs clés, modules QualitOS recommandés, connecteurs IoT typiques et un exemple de parcours.

> Ces guides décrivent **ce que le pack apporte** et **comment l'utiliser**. Pour la mécanique
> générale d'activation, voir le module [Packs sectoriels](../modules/industry-packs.md).

---

## 1. Les 14 secteurs livrés

| Secteur | Pack | Normes principales | Guide |
|---|---|---|---|
| Industrie / Manufacturing | `manufacturing` | ISO 9001, IATF 16949 | [industrie-manufacturing.md](industrie-manufacturing.md) |
| Santé / Hôpital / CHU | `healthcare-hospital` | ISO 13485, ISO 15189, HAS V2024, Joint Commission | [sante-hopital.md](sante-hopital.md) |
| Pharma / Dispositifs médicaux | `pharma` | ISO 13485, FDA 21 CFR Part 11/820, GAMP 5, MDR, ISO 14971 | [pharma-medtech.md](pharma-medtech.md) |
| Banque / Finance | `banking` | DORA, Bâle III, Solvabilité II, MiFID II, LCB-FT, ISO 27001, ISO 22301 | [banque-finance.md](banque-finance.md) |
| IT / ITSM / SaaS | `it-itsm` | ISO 20000-1, ISO 27001 | [it-itsm.md](it-itsm.md) |
| Agro-alimentaire | `agro` | ISO 22000, FSSC 22000, IFS Food v8, BRCGS v9, GlobalG.A.P., HACCP | [agroalimentaire.md](agroalimentaire.md) |
| Aéronautique / Défense | `aerospace` | AS9100, AS9110, NADCAP, ISO 9001 | [aeronautique-defense.md](aeronautique-defense.md) |
| Automobile | `automotive` | IATF 16949, VDA 6.3, ISO 9001 | [automobile.md](automobile.md) |
| BTP / Construction | `construction` | ISO 19650, ISO 9001, ISO 45001, ISO 14001 | [btp-construction.md](btp-construction.md) |
| Énergie / Utilities | `energy` | ISO 50001, ISO 55001, NIS 2, CIS Controls | [energie-utilities.md](energie-utilities.md) |
| Secteur public / Administration | `public` | ISO 9001, Marianne, RGAA, ISO 14001 | [secteur-public.md](secteur-public.md) |
| Éducation / EdTech | `education` | ISO 21001, Qualiopi, ISO 9001 | [education.md](education.md) |
| Retail / Distribution | `retail` | ISO 9001, ISO 10002 | [retail-distribution.md](retail-distribution.md) |
| Logistique / Transport | `logistics` | ISO 9001, ISO 45001, ISO 14001 | [logistique-transport.md](logistique-transport.md) |

---

## 2. Comment activer un pack sectoriel (déclaratif)

Tout est **déclaratif** : le Super Admin active le pack pour votre organisation, vous le configurez,
l'IA suggère. Un pack apporte d'un seul coup : **KPIs sectoriels**, **normes pré-chargées**,
**templates de workflows**, **exemples d'Ishikawa**, **dispositifs Poka-Yoke** et **connecteurs**
typiques du domaine.

La configuration prend la forme d'une déclaration simple (exemple pour un hôpital) :

```yaml
tenant_id: hopital-xyz
industry_packs:
  - healthcare-hospital
modules_enabled:
  - pdca, ishikawa, capa, audit, training
  - non-conformance, document-control, complaint
norms:
  - iso-9001, iso-13485, has-v2024, joint-commission
connectors:
  - hl7-fhir, his-system, lis-laboratory
language: fr-FR
```

**En pratique, côté écran :**

1. Ouvrez `/industry-packs` et **parcourez le catalogue**.
2. **Consultez le détail** du pack de votre secteur (KPIs, normes, templates apportés).
3. **Activez** le pack (selon configuration, action réservée à l'[Admin Tenant](../roles/admin-tenant.md)
   ou au [Super Admin](../roles/super-admin.md)).
4. Retrouvez vos packs dans **Mes packs** ; suivez les changements dans l'**historique**.
5. Opérationnalisez les normes apportées dans le [Standards Hub](../modules/standards-hub.md).

> **Multi-secteur ?** Vous pouvez activer plusieurs packs (ex. `manufacturing` + `automotive`).
> Les normes communes (ISO 9001, ISO 14001, ISO 45001…) sont mutualisées par le Standards Hub
> (système de management intégré).

---

## 3. Les briques communes à tous les secteurs

Quel que soit votre secteur, vous vous appuyez sur le même socle :

- **5 méthodes** : [PDCA](../modules/pdca.md), [5S](../modules/fives.md),
  [Ishikawa](../modules/ishikawa.md), [DMAIC + Poka-Yoke](../modules/dmaic.md),
  [Cercle de Qualité](../modules/circles.md).
- **Qualité opérationnelle** : [Non-conformités](../modules/non-conformites.md),
  [CAPA](../modules/capa.md), [Audits](../modules/audits.md).
- **IA explicable livrée** : [SPC / règles de Nelson](../modules/spc.md) (`/spc`),
  [détection d'anomalies](../modules/anomaly.md) (`/anomaly`),
  [prévision KPI](../modules/forecast.md) (`/forecast`),
  [clustering de NC](../modules/nc-clusters.md) (`/nc-clusters`),
  [NLP réclamations](../modules/complaints-nlp.md) (`/complaints-nlp`),
  [explicabilité SHAP](../modules/explicabilite-ia.md).
- **Normes** : [Standards Hub](../modules/standards-hub.md) (`/standards`).

Les guides sectoriels ci-dessous se contentent de **pointer les bonnes briques** pour chaque métier.
