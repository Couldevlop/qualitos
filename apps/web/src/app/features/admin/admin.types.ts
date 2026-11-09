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

/**
 * Membre de l'équipe du tenant (§16). Reflète `UserDto.Response` d'api-core :
 * le tenant y est déterminé par le JWT, jamais transmis par le client.
 */
export interface TenantUser {
  id: string;
  tenantId: string;
  keycloakId: string;
  email: string;
  roles: string[];
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Page renvoyée par l'API utilisateurs (pagination Spring Data). */
export interface TenantUserPage {
  content: TenantUser[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/**
 * Rôles du realm Keycloak, dans l'ordre de responsabilité décroissante (§16).
 * `super_admin` n'y figure pas : il appartient à l'éditeur de la plateforme, et
 * un administrateur de tenant ne doit pas pouvoir se l'attribuer.
 */
export const ASSIGNABLE_ROLES = [
  'admin_tenant',
  'quality_director',
  'quality_manager',
  'auditor',
  'user',
  'external_auditor'
] as const;

export type AssignableRole = typeof ASSIGNABLE_ROLES[number];

// ---------- comptes : invitation et création de client (ADR 0079) ----------

export interface InviteRequest {
  email: string;
  firstName?: string | null;
  lastName?: string | null;
  roles: string[];
}

/**
 * Le membre invité, et comment il entrera. Le mot de passe provisoire n'est
 * rendu qu'une fois : rien ne le relira plus tard, ni le serveur ni l'écran.
 */
export interface InviteResponse {
  user: TenantUser;
  temporaryPassword: string | null;
  invitationSent: boolean;
}

export type TenantPlan = 'STARTER' | 'PRO' | 'ENTERPRISE';

export interface TenantSummary {
  id: string;
  slug: string;
  name: string;
  plan: TenantPlan;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * Ce que l'editeur peut corriger sur un client deja cree.
 *
 * <p>Le repere (`slug`) n'y figure pas : il nomme le client dans les URL, les
 * jetons et les journaux, et le changer rendrait illisible tout ce qui a deja
 * ete ecrit sous l'ancien. L'etat (`active`) y figure, parce que suspendre et
 * reactiver sont la meme ecriture vue des deux cotes.
 */
export interface TenantUpdateRequest {
  name: string;
  plan: TenantPlan;
  active?: boolean;
}

export interface TenantPage {
  content: TenantSummary[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface OnboardRequest {
  name: string;
  slug: string;
  plan: TenantPlan;
  modules: string[];
  admin: { email: string; firstName?: string | null; lastName?: string | null };
}

export interface ModuleOutcome {
  code: string;
  activated: boolean;
  message: string | null;
}

export interface OnboardResponse {
  tenant: TenantSummary;
  admin: TenantUser;
  temporaryPassword: string | null;
  invitationSent: boolean;
  modules: ModuleOutcome[];
}

/**
 * Les quatre thèmes livrés. Liste fermée côté serveur (`TenantTheme`) : un
 * éditeur de jetons libre laisserait composer un espace illisible, et le
 * serveur refuse de toute façon un code inconnu.
 */
export type TenantTheme = 'DENSE_LIGHT' | 'AIRY_LIGHT' | 'DARK' | 'HIGH_CONTRAST';

/** L'identité d'un client telle qu'on la pose. Un champ vide se pose à `null`. */
export interface TenantIdentityRequest {
  slogan: string | null;
  legalName: string | null;
  legalForm: string | null;
  registrationNumber: string | null;
  vatNumber: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  postalCode: string | null;
  city: string | null;
  countryCode: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  primaryColor: string | null;
  secondaryColor: string | null;
  themeCode: TenantTheme;
}

/**
 * L'identité telle qu'elle revient. Les octets du logo n'y sont pas : l'écran
 * apprend qu'il en existe un et le demande à la route dédiée, qui a un cache.
 */
export interface TenantIdentityResponse extends Omit<TenantIdentityRequest, 'themeCode'> {
  themeCode: TenantTheme;
  logoAvailable: boolean;
}
