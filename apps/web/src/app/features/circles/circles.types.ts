import { SpringPage } from '../pdca/pdca.types';

export type CircleStatus = 'ACTIVE' | 'PAUSED' | 'ARCHIVED';
export type CircleRole = 'FACILITATOR' | 'SECRETARY' | 'MEMBER';

export interface CircleResponse {
  id: string;
  tenantId: string;
  name: string;
  description?: string;
  topic?: string;
  status: CircleStatus;
  memberCount: number;
  createdAt: string;
  updatedAt: string;
  members: { id: string; userId: string; role: CircleRole; joinedAt: string }[];
  meetings: Array<{
    id: string; title: string; status: string; scheduledAt: string;
    agenda?: string; durationMinutes?: number; location?: string;
  }>;
  proposals: Array<{
    id: string; title: string; status: string;
    description?: string; proposedBy?: string;
  }>;
}

export type CirclesPage = SpringPage<CircleResponse>;

export interface CreateCircleRequest {
  name: string;
  description?: string;
  topic?: string;
}

export interface UpdateCircleRequest {
  name?: string;
  description?: string;
  topic?: string;
}

export interface AddMemberRequest {
  userId: string;
  role: CircleRole;
}

export interface AddMeetingRequest {
  title: string;
  agenda?: string;
  scheduledAt: string;
  durationMinutes?: number;
  location?: string;
}

export interface AddProposalRequest {
  title: string;
  description?: string;
  proposedBy: string;
  meetingId?: string;
}

export interface CircleMeetingResponse {
  id: string;
  circleId?: string;
  title: string;
  agenda?: string;
  scheduledAt: string;
  durationMinutes?: number;
  location?: string;
  status: string;
  minutes?: string;
  heldAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CircleProposalResponse {
  id: string;
  circleId?: string;
  meetingId?: string;
  title: string;
  description?: string;
  status: string;
  proposedBy?: string;
  validatedBy?: string;
  validatedAt?: string;
  implementedAt?: string;
  measuredAt?: string;
  impactNote?: string;
  rejectionReason?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CircleMemberResponse {
  id: string;
  userId: string;
  role: CircleRole;
  joinedAt: string;
}
