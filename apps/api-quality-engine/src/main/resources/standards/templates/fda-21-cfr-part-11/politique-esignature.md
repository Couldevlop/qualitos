# Politique — Signature électronique 2FA — 21 CFR §11.100 / §11.200 / §11.300

> Tenant : `{{tenant.name}}` — Version 1.0 — `{{date.today}}`.

## 1. Principe

Chaque signature électronique sur un enregistrement GxP est juridiquement équivalente à une signature manuscrite (21 CFR §11.2). Elle doit être :

- **Unique** à un individu (§11.100).
- **Non transférable**, jamais réutilisée par un autre individu (§11.100).
- **Précédée d'une vérification d'identité** par l'organisation avant attribution.

## 2. Composantes (§11.200)

Pour les signatures non biométriques (cas standard QualitOS), deux composantes distinctes :

- **Identification code** : login fédéré Keycloak (sub claim JWT).
- **Authentication factor** : 2FA TOTP (Google Authenticator, Authy) ou WebAuthn (FIDO2 — YubiKey, Touch ID, Windows Hello).

## 3. Workflow de signature

1. L'utilisateur initie l'action (ex. : approuver un PSW, un FAI, un PPAP).
2. Modale de signature : récap du record + raison du changement (obligatoire texte libre minimum 10 caractères).
3. Utilisateur ressaisit son **2FA** (TOTP 6 chiffres ou tap WebAuthn).
4. Si premier signage de la session, ressaisie du **password** (§11.200).
5. Signature persisted dans `e_signature_records` avec :
   - `user_id`, `record_id`, `timestamp_utc`, `reason`, `auth_method`.
   - Hash SHA-256 du record signé (intégrité).
   - Signature ML-DSA-65 (post-quantique).
6. Manifestation visible (§11.50) : nom imprimable + date/heure + raison.
7. Hash ancré Hyperledger Fabric (preuve opposable).

## 4. Contrôle des passwords (§11.300)

- Longueur minimale 12 caractères.
- Complexité : 3 classes sur 4 (majuscule, minuscule, chiffre, spécial).
- Rotation obligatoire ≤ 90 jours.
- Historique : 12 derniers passwords interdits.
- Verrouillage compte après 5 échecs.
- Alerte automatique en cas de réutilisation détectée.

## 5. Cas de force majeure

- Perte de 2FA : procédure de réenrôlement avec vérification d'identité formelle.
- Suspicion d'usurpation : suspension immédiate + audit complet.

## 6. Formation et engagement

- Tout utilisateur signe une lettre d'engagement (§11.100(c)) avant attribution de droits e-signature.
- Lettre conservée dans le dossier RH.
- Formation Part 11 obligatoire (re-certif annuelle).

## 7. Indicateurs

| KPI | Cible |
|-----|-------|
| Couverture 2FA sur signatures GxP | 100 % |
| Délai blocage compte en cas d'incident | ≤ 5 min |
| Formation Part 11 à jour | 100 % personnels GxP |
