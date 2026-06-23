# ADR 0044 — Génération de compte-rendu de réunion Cercle de Qualité par LLM

**Date :** 2026-06-23
**Statut :** Accepté
**Domaine :** Cercle de Qualité (§3.3 CLAUDE.md) · IA (§12)

---

## Contexte

La section §3.3 du CLAUDE.md définit les **comptes-rendus auto-générés par LLM** comme l'une des fonctionnalités clés du module Cercle de Qualité : résumé, décisions, actions extraites automatiquement depuis le texte d'une réunion.

La transcription audio (Whisper audio → texte) est hors périmètre de cet incrément (infrastructure modèle GPU non disponible en phase courante). Elle fera l'objet d'un ADR distinct.

## Décision

### Entrée : transcript textuel

L'API accepte un **transcript textuel brut** (paste du compte-rendu manuscrit, export Teams/Jitsi, etc.) via :

```
POST /api/v1/circles/{id}/meetings/{mid}/minutes/generate
Body: { "transcript": "<texte>" }
```

La transcription **audio → texte (Whisper)** est documentée comme étape future et sera adressée lorsque l'infrastructure GPU sera disponible.

### Traitement LLM via AiGatewayClient

Le service utilise `AiGatewayClient.complete(systemPrompt, userPrompt, maxTokens=800)` — passerelle centralisée (OWASP LLM04, redaction PII, rate-limit par tenant).

**Prompt système** : demande strictement un objet JSON avec trois champs (`summary`, `decisions`, `actions`), sans texte avant ni après.

**Extraction défensive** : extraction du bloc `{ … }` entre le premier `{` et le dernier `}` avant parsing Jackson, pour absorber les markdown code fences éventuellement générées.

**Fallback** : si Jackson échoue à parser le JSON, le texte brut LLM est stocké comme résumé ; `decisions` et `actions` retournent vides.

### Persistance

Deux colonnes ajoutées via migration Flyway V97 :

| Colonne | Type | Contenu |
|---|---|---|
| `minutes_summary` | TEXT | Résumé extrait (ou texte brut en cas d'erreur de parsing) |
| `minutes_json` | TEXT | Réponse JSON brute du LLM |

### Sortie : DTO structuré

```json
{
  "summary": "…",
  "decisions": ["…"],
  "actions": [{ "label": "…", "suggestedAssignee": "…" }]
}
```

### Frontend

Dialogue Angular `CirclesMinutesDialogComponent` (NgModule, non-standalone) accessible via un bouton ✨ sur chaque ligne de réunion dans `circles-detail`.

## Alternatives rejetées

| Option | Rejet |
|---|---|
| Whisper audio dès maintenant | Infrastructure GPU non disponible ; hors périmètre incrément |
| Parsing regex au lieu de JSON | Fragile ; JSON structuré + fallback texte brut est plus robuste |
| Appel LLM direct sans AiGatewayClient | Interdit par règle 18.2 §4 (tout appel IA passe par la passerelle) |
| Champ JSON dans la colonne existante `minutes` | Bris de contrat DTO existant ; colonnes dédiées préservent la séparation |

## Conséquences

- ✅ Compte-rendu structuré disponible pour tout tenant sans infrastructure ML supplémentaire.
- ✅ Pipeline Whisper branché en remplacement de l'entrée `transcript` dans un incrément futur, sans changer le contrat de l'API.
- ⚠️ Hallucinations LLM possibles : les comptes-rendus nécessitent une relecture humaine avant validation officielle.
- ⚠️ Coût LLM par appel : couvert par le `AiGuard` (quota/rate-limit par tenant, ADR 0017).
