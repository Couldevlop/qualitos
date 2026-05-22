import { SpringPage } from '../pdca/pdca.types';

export type KpiStatus    = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';
export type KpiDirection = 'HIGHER_IS_BETTER' | 'LOWER_IS_BETTER';
export type KpiFrequency = 'REALTIME' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY' | 'ON_DEMAND';
export type KpiHealth    = 'OK' | 'WARNING' | 'CRITICAL' | 'UNKNOWN';
export type MeasurementSource = 'MANUAL' | 'COMPUTED' | 'IMPORT' | 'IOT_AGGREGATED';

export interface KpiResponse {
  id: string;
  tenantId: string;
  code: string;
  name: string;
  description?: string;
  category?: string;
  unit?: string;
  direction: KpiDirection;
  frequency?: KpiFrequency;
  targetValue?: number;
  thresholdWarning?: number;
  thresholdCritical?: number;
  status: KpiStatus;
  applicableIndustriesCsv?: string;
  ownerUserId?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export type KpiPage = SpringPage<KpiResponse>;

export interface CreateKpiRequest {
  code: string;
  name: string;
  description?: string;
  category?: string;
  unit?: string;
  direction: KpiDirection;
  frequency?: KpiFrequency;
  targetValue?: number;
  thresholdWarning?: number;
  thresholdCritical?: number;
  applicableIndustriesCsv?: string;
  ownerUserId?: string;
  createdBy: string;
}

export interface UpdateKpiRequest {
  name?: string;
  description?: string;
  category?: string;
  unit?: string;
  frequency?: KpiFrequency;
  targetValue?: number;
  thresholdWarning?: number;
  thresholdCritical?: number;
  applicableIndustriesCsv?: string;
  ownerUserId?: string;
}

export interface MeasurementResponse {
  id: string;
  tenantId: string;
  kpiId: string;
  periodStart: string;
  periodEnd: string;
  value: number;
  unit?: string;
  source?: MeasurementSource;
  recordedByUserId?: string;
  notes?: string;
  health: KpiHealth;
  createdAt: string;
}

export type MeasurementPage = SpringPage<MeasurementResponse>;

export interface RecordMeasurementRequest {
  periodStart: string;
  periodEnd: string;
  value: number;
  unit?: string;
  source?: MeasurementSource;
  recordedByUserId?: string;
  notes?: string;
}

export interface KpiCurrentStatus {
  kpiId: string;
  code: string;
  name: string;
  definitionStatus: KpiStatus;
  direction: KpiDirection;
  latestValue?: number;
  unit?: string;
  latestPeriodStart?: string;
  latestPeriodEnd?: string;
  health: KpiHealth;
  targetValue?: number;
  thresholdWarning?: number;
  thresholdCritical?: number;
}

export interface KpiTrendPoint {
  periodStart: string;
  periodEnd: string;
  value: number;
  health: KpiHealth;
}

export interface KpiTrend {
  kpiId: string;
  code: string;
  sampleCount: number;
  points: KpiTrendPoint[];
}
