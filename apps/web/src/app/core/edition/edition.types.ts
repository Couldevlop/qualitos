/** L'édition de l'installation et l'état de sa licence (ADR 0082), tels que `/api/v1/edition` les rend. */

export type Edition = 'SAAS' | 'ONPREM';

export type LicenseStatus =
  'VALID' | 'GRACE' | 'EXPIRED' | 'NOT_YET_VALID' | 'INVALID' | 'MISSING' | 'NOT_REQUIRED';

export interface EditionView {
  edition: Edition;
  licenseStatus: LicenseStatus;
  /** Faux : l'installation est en lecture seule. */
  writable: boolean;
  /** Pourquoi la licence n'est pas utilisable, à montrer à l'administrateur. */
  reason: string | null;
  customer: string | null;
  licenseId: string | null;
  tier: string | null;
  modules: string[];
  /** 0 = sans limite. */
  maxUsers: number | null;
  activeUsers: number | null;
  expiresAt: string | null;
  graceEndsAt: string | null;
  /** Jours avant l'échéance ; négatif une fois passée. */
  daysLeft: number | null;
}

/** L'édition historique : la plateforme de l'éditeur. */
export const SAAS: EditionView = {
  edition: 'SAAS', licenseStatus: 'NOT_REQUIRED', writable: true, reason: null, customer: null, licenseId: null,
  tier: null, modules: [], maxUsers: null, activeUsers: null, expiresAt: null, graceEndsAt: null, daysLeft: null
};

/** On prévient l'administrateur un mois avant l'échéance. */
export const WARN_DAYS = 30;
