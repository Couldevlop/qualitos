import { SpringPage } from '../pdca/pdca.types';

/**
 * Types front du module Industry Packs (CLAUDE.md §5, ADR 0019 Phase 2).
 *
 * Le backend renvoie le manifeste sérialisé en string JSON dans `manifestJson`
 * (modèle interne normalisé `IndustryPackManifest`, camelCase, NON_NULL). Deux
 * schémas convergent vers ce modèle :
 *  - schéma PLAT (3 packs historiques) : `kpis` = liste de slugs, `glossary`,
 *    `templates` (map workflows/ishikawa/poka-yoke = slugs) ;
 *  - schéma RICHE (11 packs canoniques) : `richKpis` (objets §6.6),
 *    `ishikawaTemplates`, `pokaYokeLibrary`, `standards`, `glossary`, `sectors`.
 *
 * Le parsing est TOLÉRANT : un pack plat sans données riches ne casse pas l'UI,
 * elle affiche un message sobre à la place.
 */

export type ActivationStatus = 'ACTIVE' | 'DEACTIVATED';

/** Réponse brute du catalogue (GET /api/v1/industry-packs[/{code}]). */
export interface PackResponse {
  id: string;
  code: string;
  name: string;
  description?: string;
  version: string;
  locale?: string;
  tags: string[];
  /** Manifeste sérialisé (string JSON) — voir IndustryPackManifest backend. */
  manifestJson?: string;
  createdAt?: string;
  updatedAt?: string;
}

/** Réponse d'activation/désactivation (POST/DELETE …/{code}/activate). */
export interface ActivationResponse {
  id: string;
  tenantId: string;
  packCode: string;
  status: ActivationStatus;
  activatedBy?: string;
  activatedAt?: string;
  deactivatedAt?: string;
  deactivatedBy?: string;
  /**
   * Champs de provisionnement (réponse en cours d'enrichissement backend) —
   * affichés UNIQUEMENT s'ils sont présents (code tolérant).
   */
  kpisCreated?: number;
  kpisSkipped?: number;
  warnings?: string[];
}

export interface ActivateRequest {
  activatedBy: string;
  configOverridesJson?: string;
}

// ---------------------------------------------------------------------------
// Manifeste typé (résultat du parse tolérant de manifestJson)
// ---------------------------------------------------------------------------

/** KPI du schéma riche (§6.6). */
export interface ManifestKpi {
  kpiId?: string;
  name?: string;
  category?: string;
  formula?: string;
  unit?: string;
  target?: string;
  thresholdWarning?: string;
  thresholdCritical?: string;
  dataSource?: string;
  refreshFrequency?: string;
  owner?: string;
  applicableIndustries?: string[];
  relatedKpis?: string[];
  explainability?: string;
}

/** Template Ishikawa avec branches 6M/7M (schéma riche). */
export interface ManifestIshikawaTemplate {
  id?: string;
  name?: string;
  problemArchetype?: string;
  /** branche (man/machine/material/method/measurement/environment…) → causes. */
  branches?: Record<string, string[]>;
}

/** Entrée Poka-Yoke (schéma riche). */
export interface ManifestPokaYoke {
  id?: string;
  name?: string;
  description?: string;
  sectorFit?: string[];
}

/** Une branche Ishikawa aplatie pour l'affichage. */
export interface IshikawaBranch {
  label: string;
  causes: string[];
}

/** Vue normalisée d'un template Ishikawa, prête pour le gabarit. */
export interface IshikawaTemplateView {
  id?: string;
  name?: string;
  problemArchetype?: string;
  branches: IshikawaBranch[];
}

/**
 * Manifeste normalisé et tolérant : tous les tableaux sont garantis non-null
 * (vides si absents), de sorte que le gabarit n'a jamais à se prémunir contre
 * `undefined`. `rich` indique si le pack porte le schéma riche (au moins un KPI
 * objet, un template Ishikawa ou une entrée Poka-Yoke).
 */
export interface PackManifest {
  rich: boolean;
  sectors: string[];
  standards: string[];
  /** Slugs KPI (schéma plat). */
  kpiSlugs: string[];
  /** KPIs objets (schéma riche). */
  kpis: ManifestKpi[];
  ishikawaTemplates: IshikawaTemplateView[];
  pokaYoke: ManifestPokaYoke[];
  glossary: { term: string; definition: string }[];
  connectors: string[];
}

/** Vue composée d'un pack : réponse + manifeste parsé + état d'activation. */
export interface PackView {
  pack: PackResponse;
  manifest: PackManifest;
  active: boolean;
}

export type PacksPage = SpringPage<PackResponse>;
