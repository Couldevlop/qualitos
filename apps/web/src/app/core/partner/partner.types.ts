/**
 * Les cabinets partenaires, vus de l'écran.
 *
 * <p>Ces types sont dans `core/` et non dans un module d'écran parce que
 * <b>trois surfaces</b> les partagent, et qu'elles appartiennent à trois mondes
 * différents : la console de l'éditeur (qui constitue le portefeuille), le
 * paramétrage d'un client (qui accorde), et la console d'un cabinet (qui en
 * bénéficie). Les loger dans l'une des trois aurait fait dépendre les deux
 * autres d'elle.
 */

/** Ce qu'un cabinet a le droit de faire chez un client. */
export type EngagementScope = 'READ' | 'INTERVENE';

/**
 * Où en est l'accord. `DECLINED` et `REVOKED` ne se confondent pas : le premier
 * n'a jamais rien ouvert, le second a été repris.
 */
export type EngagementStatus = 'PROPOSED' | 'GRANTED' | 'DECLINED' | 'REVOKED';

/** Un cabinet partenaire. */
export interface PartnerFirm {
  id: string;
  slug: string;
  name: string;
  registrationNumber: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  active: boolean;
  /** Consultants actifs. */
  consultantCount: number;
  /** Missions actuellement accordées. */
  grantedCount: number;
  createdAt: string;
}

/** Ce qu'on envoie pour créer ou modifier un cabinet. */
export interface PartnerFirmRequest {
  slug: string;
  name: string;
  registrationNumber: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
}

/** Une personne du cabinet. */
export interface PartnerConsultant {
  id: string;
  firmId: string;
  displayName: string;
  email: string;
  active: boolean;
  /** Vrai dès que la personne s'est connectée une première fois. */
  linked: boolean;
}

export interface PartnerConsultantRequest {
  displayName: string;
  email: string;
}

/** L'accord par lequel un client laisse un cabinet entrer. */
export interface PartnerEngagement {
  id: string;
  tenantId: string;
  tenantName: string | null;
  firmId: string;
  firmName: string | null;
  scope: EngagementScope;
  status: EngagementStatus;
  startsOn: string | null;
  endsOn: string | null;
  purpose: string | null;
  /** L'accès est-il ouvert AUJOURD'HUI : statut accordé ET dates tenantes. */
  openToday: boolean;
  proposedAt: string | null;
  decidedAt: string | null;
  revokedAt: string | null;
}

/** Ce que l'éditeur propose : un client, et ce que le cabinet y ferait. */
export interface ProposeRequest {
  tenantId: string;
  scope: EngagementScope;
  purpose: string | null;
}

/** Ce que le client accorde. Le terme est obligatoire, et c'est le point. */
export interface GrantRequest {
  scope: EngagementScope;
  endsOn: string;
  purpose: string | null;
}
