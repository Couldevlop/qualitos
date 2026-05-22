/**
 * GDPR DPO Appointments — désignations du Délégué à la Protection des
 * Données (Art. 37-39 RGPD).
 *
 * Workflow : PROPOSED → ACTIVE → ENDED, ou PROPOSED → CANCELLED.
 * L'activation exige la notification à l'autorité de contrôle
 * (Art. 37§7) — référence de notification + date obligatoires.
 */

export type DpoAppointmentStatus = 'PROPOSED' | 'ACTIVE' | 'ENDED' | 'CANCELLED';

export type DpoType = 'INTERNAL' | 'EXTERNAL';

export interface DpoAppointmentView {
  id: string;
  tenantId: string;
  reference: string;
  dpoFullName: string;
  dpoEmail: string;
  dpoPhone?: string;
  dpoType: DpoType;
  externalCompanyName?: string;
  qualifications?: string;
  scope: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  regulatorNotifiedAt?: string;
  regulatorNotificationReference?: string;
  linkedProcessingActivityIds: string[];
  status: DpoAppointmentStatus;
  endReason?: string;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProposeDpoRequest {
  reference: string;
  dpoFullName: string;
  dpoEmail: string;
  dpoPhone?: string;
  dpoType: DpoType;
  externalCompanyName?: string;
  qualifications?: string;
  scope: string;
  linkedProcessingActivityIds?: string[];
  createdByUserId: string;
}

export interface EditDpoRequest {
  dpoFullName: string;
  dpoEmail: string;
  dpoPhone?: string;
  dpoType: DpoType;
  externalCompanyName?: string;
  qualifications?: string;
  linkedProcessingActivityIds?: string[];
}

export interface ActivateDpoRequest {
  effectiveFrom: string;
  regulatorNotifiedAt: string;
  regulatorNotificationReference: string;
}

export interface EndDpoRequest {
  reason: string;
  effectiveTo: string;
}

export interface CancelDpoRequest {
  reason: string;
}
