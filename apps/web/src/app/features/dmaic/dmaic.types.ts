import { SpringPage } from '../pdca/pdca.types';

export type DmaicPhase = 'DEFINE' | 'MEASURE' | 'ANALYZE' | 'IMPROVE' | 'CONTROL';
export type DmaicStatus = 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';

export type PokaYokeType = 'PREVENTION' | 'DETECTION';

export type PokaYokeMechanism =
  | 'PHYSICAL_SHAPE'
  | 'INTERLOCK'
  | 'LIMIT_SWITCH'
  | 'SENSOR'
  | 'VISION'
  | 'CHECKLIST'
  | 'COLOR_CODING'
  | 'POSITION_REFERENCE'
  | 'COUNTER'
  | 'SOFTWARE_VALIDATION'
  | 'OTHER';

export type PokaYokeAssignmentStatus =
  | 'PROPOSED'
  | 'IN_DESIGN'
  | 'IMPLEMENTED'
  | 'VERIFIED'
  | 'ABANDONED';

export interface DmaicProjectResponse {
  id: string;
  tenantId: string;
  title: string;
  problemStatement?: string;
  goalStatement?: string;
  phase: DmaicPhase;
  status: DmaicStatus;
  championId?: string;
  blackBeltId: string;
  targetCompletionDate?: string;
  specLowerLimit?: number;
  specUpperLimit?: number;
  specTarget?: number;
  specUnit?: string;
  estimatedSavingsEur?: number;
  measureCount: number;
  pokaYokeCount: number;
  startedAt?: string;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export type DmaicProjectPage = SpringPage<DmaicProjectResponse>;

export interface CreateDmaicProjectRequest {
  title: string;
  problemStatement?: string;
  goalStatement?: string;
  blackBeltId: string;
  championId?: string;
  targetCompletionDate?: string;
  specLowerLimit?: number;
  specUpperLimit?: number;
  specTarget?: number;
  specUnit?: string;
  estimatedSavingsEur?: number;
}

export interface UpdateDmaicProjectRequest {
  title?: string;
  problemStatement?: string;
  goalStatement?: string;
  blackBeltId?: string;
  championId?: string;
  targetCompletionDate?: string;
  specLowerLimit?: number;
  specUpperLimit?: number;
  specTarget?: number;
  specUnit?: string;
  estimatedSavingsEur?: number;
}

export interface AddMeasureRequest {
  value: number;
  subgroupId?: string;
  sourceRef?: string;
  recordedAt?: string;
  operatorId?: string;
  note?: string;
}

export interface MeasureResponse {
  id: string;
  projectId: string;
  value: number;
  subgroupId?: string;
  sourceRef?: string;
  recordedAt?: string;
  operatorId?: string;
  note?: string;
  createdAt: string;
}

export interface CapabilityResponse {
  sampleSize: number;
  mean?: number;
  stdDev?: number;
  min?: number;
  max?: number;
  specLowerLimit?: number;
  specUpperLimit?: number;
  specTarget?: number;
  cp?: number;
  cpk?: number;
  cpu?: number;
  cpl?: number;
  sigmaLevel?: number;
  interpretation?: string;
  warnings: string[];
}

export interface DeviceSummary {
  id: string;
  code: string;
  name: string;
  type: PokaYokeType;
  mechanism: PokaYokeMechanism;
  applicableIndustries?: string;
  implementationCost?: string;
}

export type DeviceSummaryPage = SpringPage<DeviceSummary>;

export interface DeviceDetail extends DeviceSummary {
  description?: string;
  examples?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AssignPokaYokeRequest {
  deviceId: string;
  note?: string;
}

export interface UpdateAssignmentRequest {
  status?: PokaYokeAssignmentStatus;
  note?: string;
  defectReductionPct?: number;
}

export interface AssignmentResponse {
  id: string;
  projectId: string;
  deviceId: string;
  deviceCode: string;
  deviceName: string;
  deviceType: PokaYokeType;
  status: PokaYokeAssignmentStatus;
  note?: string;
  implementedAt?: string;
  verifiedAt?: string;
  defectReductionPct?: number;
  createdAt: string;
  updatedAt: string;
}
