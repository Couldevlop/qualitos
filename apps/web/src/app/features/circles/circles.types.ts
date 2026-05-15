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
  meetings: { id: string; title: string; status: string; scheduledAt: string }[];
  proposals: { id: string; title: string; status: string }[];
}

export type CirclesPage = SpringPage<CircleResponse>;
