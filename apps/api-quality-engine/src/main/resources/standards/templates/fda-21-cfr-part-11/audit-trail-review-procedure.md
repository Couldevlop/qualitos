# Procédure — Revue d'audit trail GxP — 21 CFR §11.10(e)

> Tenant : `{{tenant.name}}` — Version 1.0 — `{{date.today}}`.

## 1. Objet

Définir la méthode, la fréquence, les rôles et les enregistrements pour la revue des audit trails techniques des systèmes GxP, conformément à §11.10(e) (audit trails sécurisés, horodatés, non altérables, ne masquant pas les informations originales).

## 2. Périmètre

- Systèmes GxP critiques : `{{at.systems.critical}}` — revue mensuelle.
- Systèmes GxP supports : `{{at.systems.supporting}}` — revue trimestrielle.
- Systèmes non-GxP : pas de revue formelle, mais journalisation conservée.

## 3. Contenu minimum d'un audit trail

- Date et heure (UTC + fuseau local).
- Identifiant utilisateur (jamais générique).
- Action (CREATE / READ / UPDATE / DELETE / SIGN / EXPORT).
- Entité métier et identifiant.
- Valeur avant / valeur après (UPDATE).
- Raison du changement (si signature électronique).
- Adresse IP source.
- Hash du record (intégrité).

## 4. Caractéristiques techniques

- Audit trail **append-only** (aucun DELETE/UPDATE possible côté DB).
- Stockage chiffré au repos (AES-256-GCM).
- Conservation : durée du record GxP + 5 ans (selon plus long).
- Sauvegarde et restauration testées au moins annuellement.

## 5. Méthode de revue

- Échantillonnage stratifié : 100 % des actions critiques (signatures, changements de statut GxP) + échantillon aléatoire 1 % des autres.
- Recherche d'anomalies : modifications anormales, accès hors horaires, escalades de droits.
- Vérification cohérence métier vs technique.
- Vérification absence de gap horodaté.

## 6. Rôles

| Rôle | Responsabilité |
|------|----------------|
| Data Steward | Exécute la revue |
| QA | Valide les conclusions |
| RSSI | Notifié sur anomalies sécurité |

## 7. Enregistrements

Chaque revue produit un rapport signé 21 CFR 11 :
- Périmètre revu.
- Échantillon analysé.
- Anomalies identifiées.
- Plan d'action.
- Signature reviewer + QA.
- Hash blockchain.

## 8. Indicateurs

| KPI | Cible |
|-----|-------|
| % revues exécutées dans les délais | 100 % |
| % anomalies traitées dans les 30j | 100 % |
| Délai détection anomalie critique | ≤ 24h |
