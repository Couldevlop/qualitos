/**
 * Types du Storyboard IA (§7.4). Le SPA envoie une liste d'indicateurs (libellé, valeur,
 * tendance/cible optionnelles) + une période à l'engine (`POST /api/v1/ai/storyboard`), qui
 * applique les garde-fous IA (OWASP LLM04) et relaie vers ai-service (LLM réel + fallback).
 * Le tenant vient du JWT côté serveur (jamais envoyé dans le body).
 */

export interface IndicatorPoint {
  label: string;
  value: string;
  /** Tendance optionnelle (ex. « -12 % », « stable », « ↗ »). */
  trend?: string;
  /** Cible optionnelle (ex. « < 2 », « 95 »). */
  target?: string;
  /** Unité optionnelle (ex. « %, j, ppm »). */
  unit?: string;
}

export interface StoryboardRequest {
  period: string;
  context?: string;
  points: IndicatorPoint[];
}

export interface StoryboardResponse {
  /** Récit narratif généré par l'IA. */
  narrative: string;
  /** Fournisseur LLM ayant produit le récit (explicabilité §12.3). */
  provider: string;
  period: string;
  /** Rappel fidèle des chiffres source (explicabilité §12.3). */
  sources: IndicatorPoint[];
}
