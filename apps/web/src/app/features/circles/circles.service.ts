import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay, map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  AddMeetingRequest,
  AddMemberRequest,
  AddProposalRequest,
  ApproveProposalRequest,
  CircleMeetingResponse,
  CircleMemberResponse,
  CircleProposalResponse,
  CircleResponse,
  CircleStatus,
  CirclesPage,
  CreateCircleRequest,
  GenerateMinutesRequest,
  MeetingMinutes,
  RecordImpactRequest,
  RejectProposalRequest,
  UpdateCircleRequest
} from './circles.types';

@Injectable({ providedIn: 'root' })
export class CirclesService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/circles`;

  private readonly mockStore: CircleResponse[] = this.seedMockCircles();

  constructor(private readonly http: HttpClient) {}

  listCircles(page = 0, size = 50, status?: CircleStatus): Observable<CirclesPage> {
    if (environment.useMockApi) return of(this.mockPage(status)).pipe(delay(150));
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<CirclesPage>(this.endpoint, { params });
  }

  getCircle(id: string): Observable<CircleResponse> {
    if (environment.useMockApi) {
      const found = this.mockStore.find(c => c.id === id);
      return of(found ?? this.mockStore[0]).pipe(delay(120));
    }
    return this.http.get<CircleResponse>(`${this.endpoint}/${id}`);
  }

  createCircle(input: CreateCircleRequest): Observable<CircleResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const circle: CircleResponse = {
        id: 'c-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        name: input.name,
        description: input.description,
        topic: input.topic,
        status: 'ACTIVE',
        memberCount: 0,
        createdAt: now,
        updatedAt: now,
        members: [],
        meetings: [],
        proposals: []
      };
      this.mockStore.unshift(circle);
      return of(circle).pipe(delay(200));
    }
    return this.http.post<CircleResponse>(this.endpoint, input);
  }

  deleteCircle(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(c => c.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  updateCircle(id: string, input: UpdateCircleRequest): Observable<CircleResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      if (c) {
        if (input.name !== undefined) c.name = input.name;
        if (input.description !== undefined) c.description = input.description;
        if (input.topic !== undefined) c.topic = input.topic;
        c.updatedAt = new Date().toISOString();
        return of(c).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<CircleResponse>(`${this.endpoint}/${id}`, input);
  }

  addMeeting(circleId: string, input: AddMeetingRequest): Observable<CircleMeetingResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const meeting: CircleMeetingResponse = {
        id: 'mt-' + Math.random().toString(36).slice(2, 9),
        circleId, title: input.title, agenda: input.agenda,
        scheduledAt: input.scheduledAt, durationMinutes: input.durationMinutes,
        location: input.location, status: 'SCHEDULED',
        createdAt: now, updatedAt: now
      };
      const c = this.mockStore.find(x => x.id === circleId);
      if (c) {
        c.meetings = [...c.meetings, meeting];
        c.updatedAt = now;
      }
      return of(meeting).pipe(delay(120));
    }
    return this.http.post<CircleMeetingResponse>(`${this.endpoint}/${circleId}/meetings`, input);
  }

  addProposal(circleId: string, input: AddProposalRequest): Observable<CircleProposalResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const proposal: CircleProposalResponse = {
        id: 'p-' + Math.random().toString(36).slice(2, 9),
        circleId, title: input.title, description: input.description,
        status: 'PROPOSED', proposedBy: input.proposedBy,
        meetingId: input.meetingId,
        createdAt: now, updatedAt: now
      };
      const c = this.mockStore.find(x => x.id === circleId);
      if (c) {
        c.proposals = [...c.proposals, proposal];
        c.updatedAt = now;
      }
      return of(proposal).pipe(delay(120));
    }
    return this.http.post<CircleProposalResponse>(`${this.endpoint}/${circleId}/proposals`, input);
  }

  addMember(circleId: string, input: AddMemberRequest): Observable<CircleMemberResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const member: CircleMemberResponse = {
        id: 'm-' + Math.random().toString(36).slice(2, 9),
        userId: input.userId,
        role: input.role,
        joinedAt: now
      };
      const c = this.mockStore.find(x => x.id === circleId);
      if (c) {
        c.members = [...c.members, member];
        c.memberCount = c.members.length;
        c.updatedAt = now;
      }
      return of(member).pipe(delay(120));
    }
    return this.http.post<CircleMemberResponse>(`${this.endpoint}/${circleId}/members`, input);
  }

  generateMinutes(circleId: string, meetingId: string, req: GenerateMinutesRequest): Observable<MeetingMinutes> {
    if (environment.useMockApi) {
      return of({
        summary: `Compte-rendu simulé de la réunion « ${meetingId.slice(0, 8)} ». Discussions productives. Trois axes d'amélioration identifiés.`,
        decisions: ["Adoption du plan d'action qualité", 'Revue mensuelle des indicateurs'],
        actions: [
          { label: 'Mettre à jour la procédure P-001', suggestedAssignee: 'Animateur' },
          { label: 'Former les nouveaux membres', suggestedAssignee: 'Secrétaire' }
        ]
      } as MeetingMinutes).pipe(delay(800));
    }
    return this.http.post<MeetingMinutes>(
      `${this.endpoint}/${circleId}/meetings/${meetingId}/minutes/generate`, req
    );
  }

  reviewProposal(circleId: string, proposalId: string): Observable<CircleProposalResponse> {
    if (environment.useMockApi) return this.mockProposalTransition(circleId, proposalId, 'UNDER_REVIEW');
    return this.http.patch<CircleProposalResponse>(`${this.endpoint}/${circleId}/proposals/${proposalId}/review`, {});
  }

  approveProposal(circleId: string, proposalId: string, body: ApproveProposalRequest): Observable<CircleProposalResponse> {
    if (environment.useMockApi) return this.mockProposalTransition(circleId, proposalId, 'APPROVED');
    return this.http.patch<CircleProposalResponse>(`${this.endpoint}/${circleId}/proposals/${proposalId}/approve`, body);
  }

  rejectProposal(circleId: string, proposalId: string, body: RejectProposalRequest): Observable<CircleProposalResponse> {
    if (environment.useMockApi) return this.mockProposalTransition(circleId, proposalId, 'REJECTED', { rejectionReason: body.reason });
    return this.http.patch<CircleProposalResponse>(`${this.endpoint}/${circleId}/proposals/${proposalId}/reject`, body);
  }

  implementProposal(circleId: string, proposalId: string): Observable<CircleProposalResponse> {
    if (environment.useMockApi) return this.mockProposalTransition(circleId, proposalId, 'IMPLEMENTED');
    return this.http.patch<CircleProposalResponse>(`${this.endpoint}/${circleId}/proposals/${proposalId}/implement`, {});
  }

  recordImpact(circleId: string, proposalId: string, body: RecordImpactRequest): Observable<CircleProposalResponse> {
    if (environment.useMockApi) return this.mockProposalTransition(circleId, proposalId, 'MEASURED', { impactNote: body.impactNote });
    return this.http.patch<CircleProposalResponse>(`${this.endpoint}/${circleId}/proposals/${proposalId}/impact`, body);
  }

  private mockProposalTransition(
    circleId: string, proposalId: string, newStatus: string,
    extra: Partial<CircleProposalResponse> = {}
  ): Observable<CircleProposalResponse> {
    const c = this.mockStore.find(x => x.id === circleId);
    const now = new Date().toISOString();
    if (c) {
      const p = c.proposals.find(x => x.id === proposalId) as CircleProposalResponse | undefined;
      if (p) {
        p.status = newStatus;
        p.updatedAt = now;
        Object.assign(p, extra);
        c.updatedAt = now;
        return of({ ...p }).pipe(delay(120));
      }
    }
    const fallback: CircleProposalResponse = { id: proposalId, circleId, title: '', status: newStatus, ...extra, createdAt: now, updatedAt: now };
    return of(fallback).pipe(delay(120));
  }

  pauseCircle(id: string): Observable<CircleResponse> {
    return this.transition(id, 'PAUSED', 'pause');
  }

  resumeCircle(id: string): Observable<CircleResponse> {
    return this.transition(id, 'ACTIVE', 'resume');
  }

  archiveCircle(id: string): Observable<CircleResponse> {
    return this.transition(id, 'ARCHIVED', 'archive');
  }

  private transition(
    id: string,
    targetStatus: CircleStatus,
    pathSegment: 'pause' | 'resume' | 'archive'
  ): Observable<CircleResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      if (c) {
        c.status = targetStatus;
        c.updatedAt = new Date().toISOString();
        return of(c).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<CircleResponse>(`${this.endpoint}/${id}/${pathSegment}`, {});
  }

  private mockPage(status?: CircleStatus): CirclesPage {
    const f = status ? this.mockStore.filter(c => c.status === status) : this.mockStore;
    return { content: f, totalElements: f.length, totalPages: 1, number: 0, size: f.length };
  }

  private seedMockCircles(): CircleResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'c1', tenantId: 't', name: 'Cercle production ligne 3',
        description: 'Amélioration continue du process soudure',
        topic: 'production-soudure', status: 'ACTIVE', memberCount: 7,
        createdAt: now, updatedAt: now,
        members: [
          { id: 'm1', userId: 'u1', role: 'FACILITATOR', joinedAt: now },
          { id: 'm2', userId: 'u2', role: 'SECRETARY', joinedAt: now }
        ],
        meetings: [
          { id: 'mt1', title: 'Réunion mensuelle Avril', status: 'HELD', scheduledAt: now }
        ],
        proposals: [
          { id: 'p1', title: 'Ajout poka-yoke positionnement pièce', status: 'PROPOSED', description: 'Idée soumise par l\'équipe' },
          { id: 'p2', title: 'Optimisation réglage machine', status: 'UNDER_REVIEW' },
          { id: 'p3', title: 'Maintenance préventive cobot-3', status: 'APPROVED' },
          { id: 'p4', title: 'Réduction cycle nettoyage', status: 'IMPLEMENTED' },
          { id: 'p5', title: 'Formation opérateurs tri sélectif', status: 'MEASURED' }
        ]
      },
      {
        id: 'c2', tenantId: 't', name: 'Cercle qualité fournisseurs',
        description: 'Notation et amélioration relation fournisseurs',
        topic: 'fournisseurs', status: 'PAUSED', memberCount: 5,
        createdAt: now, updatedAt: now,
        members: [], meetings: [], proposals: []
      }
    ];
  }
}
