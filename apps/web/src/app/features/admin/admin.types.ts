/**
 * Administration des modules par tenant (§10.4 — « 100 % modulable »).
 * Contrat de `/api/v1/tenant-modules`.
 */

/** Cycle de vie d'une activation. `TRIAL` et `ACTIVE` sont les états ouverts. */
export type ActivationStatus = 'TRIAL' | 'ACTIVE' | 'SUSPENDED' | 'EXPIRED' | 'DISABLED';

/** Palier de facturation ; un module peut exiger un palier minimum. */
export type BillingTier = 'FREE' | 'STANDARD' | 'PRO' | 'ENTERPRISE';

/** Entrée du catalogue : ce que la plateforme sait faire, indépendamment du tenant. */
export interface ModuleCatalogEntry {
  code: string;
  name: string;
  category: string;
  minimumTier: BillingTier;
  /** Codes des modules requis pour activer celui-ci. */
  dependencies: string[];
  /** Un module cœur ne peut pas être désactivé. */
  coreModule: boolean;
}

/** État d'activation d'un module pour le tenant courant. */
export interface ModuleActivation {
  id: string;
  tenantId: string;
  moduleCode: string;
  status: ActivationStatus;
  enabled: boolean;
  billingTier: BillingTier;
  configurationJson: string | null;
  trialEndsAt: string | null;
  expiresAt: string | null;
  activatedAt: string | null;
  activatedBy: string | null;
  statusChangedAt: string | null;
  lastChangedBy: string | null;
  updatedAt: string | null;
}

/** Compteurs de la barre de synthèse. */
export interface TenantModuleSummary {
  tenantId: string;
  tenantTier: BillingTier;
  totalActivations: number;
  enabledCount: number;
  trialCount: number;
  activeCount: number;
  suspendedCount: number;
  expiredCount: number;
  disabledCount: number;
  activations: ModuleActivation[];
}

/**
 * Ligne affichée : une entrée de catalogue enrichie de son activation éventuelle.
 * Un module jamais activé n'a pas d'activation — d'où le `null`.
 */
export interface ModuleRow {
  entry: ModuleCatalogEntry;
  activation: ModuleActivation | null;
}
