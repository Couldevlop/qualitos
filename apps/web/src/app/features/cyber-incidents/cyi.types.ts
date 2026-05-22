export type CyiStatus = 'DETECTED' | 'ASSESSING' | 'MITIGATED' | 'CLOSED' | 'REJECTED';
export type CyiSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type CyiType =
  | 'RANSOMWARE' | 'DATA_BREACH' | 'DDOS' | 'INSIDER_THREAT'
  | 'MALWARE' | 'PHISHING' | 'SYSTEM_OUTAGE' | 'SUPPLY_CHAIN'
  | 'UNAUTHORIZED_ACCESS' | 'OTHER';

export const TYPE_LABEL: Record<CyiType, string> = {
  RANSOMWARE:          'Rançongiciel',
  DATA_BREACH:         'Violation de données',
  DDOS:                'DDoS',
  INSIDER_THREAT:      'Menace interne',
  MALWARE:             'Malware',
  PHISHING:            'Phishing',
  SYSTEM_OUTAGE:       'Indisponibilité système',
  SUPPLY_CHAIN:        'Chaîne d\'approvisionnement',
  UNAUTHORIZED_ACCESS: 'Accès non autorisé',
  OTHER:               'Autre'
};

export interface CyiView {
  id: string;
  tenantId: string;
  reference: string;
  title: string;
  description?: string | null;
  detectedAt: string;
  occurredAt?: string | null;
  earlyWarningDeadlineAt?: string | null;
  initialAssessmentDeadlineAt?: string | null;
  finalReportDeadlineAt?: string | null;
  incidentType: CyiType;
  severity: CyiSeverity;
  status: CyiStatus;
  estimatedAffectedUsers: number;
  affectedAssets?: string[] | null;
  affectedServices?: string[] | null;
  linkedBreachId?: string | null;
  containmentMeasures?: string | null;
  impactDescription?: string | null;
  earlyWarningSentAt?: string | null;
  earlyWarningReference?: string | null;
  initialAssessmentSentAt?: string | null;
  initialAssessmentReference?: string | null;
  finalReportSentAt?: string | null;
  finalReportReference?: string | null;
  closureNotes?: string | null;
  rejectionReason?: string | null;
  reportedByUserId: string;
  handledByUserId?: string | null;
  closedAt?: string | null;
  updatedAt: string;
  earlyWarningOverdue: boolean;
  initialAssessmentOverdue: boolean;
  finalReportOverdue: boolean;
  significant: boolean;
}

export interface CyiDetectRequest {
  reference: string;
  title: string;
  description?: string;
  detectedAt: string;
  occurredAt?: string;
  incidentType: CyiType;
  severity: CyiSeverity;
  estimatedAffectedUsers: number;
  affectedAssets?: string[];
  affectedServices?: string[];
  linkedBreachId?: string;
  reportedByUserId: string;
}

export interface CyiStartAssessmentRequest { handledByUserId: string; }
export interface CyiMitigateRequest {
  containmentMeasures: string;
  impactDescription?: string;
  handledByUserId?: string;
}
export interface CyiNotificationRequest { sentAt: string; reference: string; }
export interface CyiCloseRequest { closureNotes?: string; }
export interface CyiRejectRequest { reason: string; }
export interface CyiUpdateSeverityRequest { severity: CyiSeverity; }
export interface CyiLinkBreachRequest { breachId: string; }
