import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  AssessCompetencyRequest,
  AttachSkillRequirementRequest,
  CompetencyMatrix,
  CompetencyResponse,
  CompleteRequest,
  CreatePathRequest,
  CreateSkillRequest,
  EnrollRequest,
  EnrollmentPage,
  EnrollmentResponse,
  EnrollmentStatus,
  PathPage,
  PathResponse,
  ProgressUpdateRequest,
  RoleGapAnalysis,
  SkillPage,
  SkillRequirementResponse,
  SkillResponse,
  TrainingPathStatus,
  UpdatePathRequest,
  UpdateSkillRequest
} from './training.types';

@Injectable({ providedIn: 'root' })
export class TrainingService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/training`;

  private readonly mockSkills: SkillResponse[]      = this.seedSkills();
  private readonly mockPaths:  PathResponse[]       = this.seedPaths();
  private readonly mockEnrollments: EnrollmentResponse[] = this.seedEnrollments();
  private readonly mockRequirements: Record<string, SkillRequirementResponse[]> = this.seedRequirements();
  private readonly mockCompetencies: Record<string, CompetencyResponse[]> = {};

  constructor(private readonly http: HttpClient) {}

  // ---------- Skills ----------

  listSkills(page = 0, size = 50, category?: string): Observable<SkillPage> {
    if (environment.useMockApi) {
      const f = category
        ? this.mockSkills.filter(s => s.category === category)
        : this.mockSkills;
      return of({ content: f, totalElements: f.length, totalPages: 1, number: 0, size: f.length }).pipe(delay(120));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (category) params = params.set('category', category);
    return this.http.get<SkillPage>(`${this.endpoint}/skills`, { params });
  }

  getSkill(id: string): Observable<SkillResponse> {
    if (environment.useMockApi) {
      return of(this.mockSkills.find(s => s.id === id) ?? this.mockSkills[0]).pipe(delay(100));
    }
    return this.http.get<SkillResponse>(`${this.endpoint}/skills/${id}`);
  }

  createSkill(input: CreateSkillRequest): Observable<SkillResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const s: SkillResponse = {
        id: 'skill-' + (this.mockSkills.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        code: input.code, name: input.name, description: input.description, category: input.category,
        createdAt: now, updatedAt: now
      };
      this.mockSkills.unshift(s);
      return of(s).pipe(delay(150));
    }
    return this.http.post<SkillResponse>(`${this.endpoint}/skills`, input);
  }

  updateSkill(id: string, input: UpdateSkillRequest): Observable<SkillResponse> {
    if (environment.useMockApi) {
      const s = this.mockSkills.find(x => x.id === id);
      if (s) { Object.assign(s, input); s.updatedAt = new Date().toISOString(); return of(s).pipe(delay(120)); }
      return of(this.mockSkills[0]).pipe(delay(120));
    }
    return this.http.patch<SkillResponse>(`${this.endpoint}/skills/${id}`, input);
  }

  deleteSkill(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockSkills.findIndex(s => s.id === id);
      if (i >= 0) this.mockSkills.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/skills/${id}`);
  }

  // ---------- Competencies ----------

  assessCompetency(input: AssessCompetencyRequest): Observable<CompetencyResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const names: ('NONE' | 'AWARE' | 'PRACTITIONER' | 'COMPETENT' | 'EXPERT')[] =
        ['NONE', 'AWARE', 'PRACTITIONER', 'COMPETENT', 'EXPERT'];
      const c: CompetencyResponse = {
        id: 'comp-' + Math.random().toString(36).slice(2, 9),
        tenantId: 'demo-tenant',
        userId: input.userId, skillId: input.skillId,
        level: input.level, levelName: names[input.level],
        source: input.source, assessedBy: input.assessedBy,
        assessedAt: now.slice(0, 10), expiresOn: input.expiresOn,
        expired: input.expiresOn ? new Date(input.expiresOn).getTime() < Date.now() : false,
        createdAt: now, updatedAt: now
      };
      const arr = this.mockCompetencies[input.userId] ?? [];
      // replace existing competency for the same skill
      const i = arr.findIndex(x => x.skillId === input.skillId);
      if (i >= 0) arr[i] = c; else arr.push(c);
      this.mockCompetencies[input.userId] = arr;
      return of(c).pipe(delay(120));
    }
    return this.http.post<CompetencyResponse>(`${this.endpoint}/competencies/assess`, input);
  }

  getMatrix(userId: string): Observable<CompetencyMatrix> {
    if (environment.useMockApi) {
      return of({ userId, competencies: this.mockCompetencies[userId] ?? [] }).pipe(delay(100));
    }
    return this.http.get<CompetencyMatrix>(`${this.endpoint}/competencies/users/${userId}`);
  }

  getGap(userId: string, pathId: string): Observable<RoleGapAnalysis> {
    if (environment.useMockApi) {
      const reqs = this.mockRequirements[pathId] ?? [];
      const myComps = this.mockCompetencies[userId] ?? [];
      const path = this.mockPaths.find(p => p.id === pathId);
      const gaps = reqs.map(r => {
        const my = myComps.find(c => c.skillId === r.skillId);
        const cur = my?.level ?? 0;
        const skill = this.mockSkills.find(s => s.id === r.skillId);
        return {
          skillId: r.skillId,
          skillCode: skill?.code ?? '',
          currentLevel: cur,
          targetLevel: r.targetLevel,
          gap: Math.max(0, r.targetLevel - cur)
        };
      });
      return of({
        userId, pathId, pathCode: path?.code ?? '',
        totalRequirements: reqs.length,
        satisfied: gaps.filter(g => g.gap === 0).length,
        gaps: gaps.filter(g => g.gap > 0)
      }).pipe(delay(120));
    }
    const params = new HttpParams().set('pathId', pathId);
    return this.http.get<RoleGapAnalysis>(`${this.endpoint}/competencies/users/${userId}/gap`, { params });
  }

  // ---------- Paths ----------

  listPaths(page = 0, size = 50, status?: TrainingPathStatus, targetRole?: string): Observable<PathPage> {
    if (environment.useMockApi) {
      const f = this.mockPaths
        .filter(p => !status     || p.status     === status)
        .filter(p => !targetRole || p.targetRole === targetRole);
      return of({ content: f, totalElements: f.length, totalPages: 1, number: 0, size: f.length }).pipe(delay(120));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status)     params = params.set('status', status);
    if (targetRole) params = params.set('targetRole', targetRole);
    return this.http.get<PathPage>(`${this.endpoint}/paths`, { params });
  }

  getPath(id: string): Observable<PathResponse> {
    if (environment.useMockApi) {
      return of(this.mockPaths.find(p => p.id === id) ?? this.mockPaths[0]).pipe(delay(100));
    }
    return this.http.get<PathResponse>(`${this.endpoint}/paths/${id}`);
  }

  createPath(input: CreatePathRequest): Observable<PathResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const p: PathResponse = {
        id: 'path-' + (this.mockPaths.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        code: input.code, name: input.name, description: input.description,
        targetRole: input.targetRole,
        durationHours: input.durationHours,
        passingScore: input.passingScore ?? 70,
        validityMonths: input.validityMonths,
        status: 'DRAFT', createdBy: input.createdBy,
        createdAt: now, updatedAt: now
      };
      this.mockPaths.unshift(p);
      this.mockRequirements[p.id] = [];
      return of(p).pipe(delay(150));
    }
    return this.http.post<PathResponse>(`${this.endpoint}/paths`, input);
  }

  updatePath(id: string, input: UpdatePathRequest): Observable<PathResponse> {
    if (environment.useMockApi) {
      const p = this.mockPaths.find(x => x.id === id);
      if (p) { Object.assign(p, input); p.updatedAt = new Date().toISOString(); return of(p).pipe(delay(120)); }
      return of(this.mockPaths[0]).pipe(delay(120));
    }
    return this.http.patch<PathResponse>(`${this.endpoint}/paths/${id}`, input);
  }

  deletePath(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockPaths.findIndex(p => p.id === id);
      if (i >= 0) this.mockPaths.splice(i, 1);
      delete this.mockRequirements[id];
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/paths/${id}`);
  }

  activatePath(id: string): Observable<PathResponse> { return this.pathTransition(id, 'activate', 'ACTIVE'); }
  reopenPath(id: string):   Observable<PathResponse> { return this.pathTransition(id, 'reopen',   'DRAFT'); }
  archivePath(id: string):  Observable<PathResponse> { return this.pathTransition(id, 'archive',  'ARCHIVED'); }

  private pathTransition(
    id: string, op: 'activate' | 'reopen' | 'archive',
    target: TrainingPathStatus
  ): Observable<PathResponse> {
    if (environment.useMockApi) {
      const p = this.mockPaths.find(x => x.id === id);
      if (p) { p.status = target; p.updatedAt = new Date().toISOString(); return of(p).pipe(delay(120)); }
      return of(this.mockPaths[0]).pipe(delay(120));
    }
    return this.http.post<PathResponse>(`${this.endpoint}/paths/${id}/${op}`, {});
  }

  listRequirements(pathId: string): Observable<SkillRequirementResponse[]> {
    if (environment.useMockApi) {
      return of(this.mockRequirements[pathId] ?? []).pipe(delay(100));
    }
    return this.http.get<SkillRequirementResponse[]>(`${this.endpoint}/paths/${pathId}/requirements`);
  }

  attachRequirement(pathId: string, input: AttachSkillRequirementRequest): Observable<SkillRequirementResponse> {
    if (environment.useMockApi) {
      const r: SkillRequirementResponse = {
        id: 'req-' + Math.random().toString(36).slice(2, 9),
        pathId, skillId: input.skillId, targetLevel: input.targetLevel,
        createdAt: new Date().toISOString()
      };
      const arr = this.mockRequirements[pathId] ?? [];
      // replace if same skill already required
      const i = arr.findIndex(x => x.skillId === input.skillId);
      if (i >= 0) arr[i] = r; else arr.push(r);
      this.mockRequirements[pathId] = arr;
      return of(r).pipe(delay(120));
    }
    return this.http.post<SkillRequirementResponse>(`${this.endpoint}/paths/${pathId}/requirements`, input);
  }

  detachRequirement(pathId: string, skillId: string): Observable<void> {
    if (environment.useMockApi) {
      const arr = this.mockRequirements[pathId] ?? [];
      const i = arr.findIndex(x => x.skillId === skillId);
      if (i >= 0) arr.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/paths/${pathId}/requirements/${skillId}`);
  }

  // ---------- Enrollments ----------

  listEnrollments(
    page = 0, size = 50,
    filter?: { userId?: string; pathId?: string; status?: EnrollmentStatus }
  ): Observable<EnrollmentPage> {
    if (environment.useMockApi) {
      let arr = this.mockEnrollments;
      if (filter?.userId) arr = arr.filter(e => e.userId === filter.userId);
      if (filter?.pathId) arr = arr.filter(e => e.pathId === filter.pathId);
      if (filter?.status) arr = arr.filter(e => e.status === filter.status);
      return of({ content: arr, totalElements: arr.length, totalPages: 1, number: 0, size: arr.length }).pipe(delay(100));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (filter?.userId) params = params.set('userId', filter.userId);
    if (filter?.pathId) params = params.set('pathId', filter.pathId);
    if (filter?.status) params = params.set('status', filter.status);
    return this.http.get<EnrollmentPage>(`${this.endpoint}/enrollments`, { params });
  }

  enroll(input: EnrollRequest): Observable<EnrollmentResponse> {
    if (environment.useMockApi) {
      const now = new Date();
      const e: EnrollmentResponse = {
        id: 'enr-' + Math.random().toString(36).slice(2, 9),
        tenantId: 'demo-tenant',
        userId: input.userId, pathId: input.pathId,
        status: 'ENROLLED', progressPct: 0,
        enrolledOn: now.toISOString().slice(0, 10),
        createdAt: now.toISOString(), updatedAt: now.toISOString()
      };
      this.mockEnrollments.unshift(e);
      return of(e).pipe(delay(120));
    }
    return this.http.post<EnrollmentResponse>(`${this.endpoint}/enrollments`, input);
  }

  startEnrollment(id: string): Observable<EnrollmentResponse> {
    if (environment.useMockApi) {
      const e = this.mockEnrollments.find(x => x.id === id);
      if (e) {
        e.status = 'IN_PROGRESS';
        e.startedOn = new Date().toISOString().slice(0, 10);
        e.updatedAt = new Date().toISOString();
        return of(e).pipe(delay(120));
      }
      return of(this.mockEnrollments[0]).pipe(delay(120));
    }
    return this.http.post<EnrollmentResponse>(`${this.endpoint}/enrollments/${id}/start`, {});
  }

  updateProgress(id: string, input: ProgressUpdateRequest): Observable<EnrollmentResponse> {
    if (environment.useMockApi) {
      const e = this.mockEnrollments.find(x => x.id === id);
      if (e) { e.progressPct = input.progressPct; e.updatedAt = new Date().toISOString(); return of(e).pipe(delay(120)); }
      return of(this.mockEnrollments[0]).pipe(delay(120));
    }
    return this.http.post<EnrollmentResponse>(`${this.endpoint}/enrollments/${id}/progress`, input);
  }

  completeEnrollment(id: string, input: CompleteRequest): Observable<EnrollmentResponse> {
    if (environment.useMockApi) {
      const e = this.mockEnrollments.find(x => x.id === id);
      const path = e ? this.mockPaths.find(p => p.id === e.pathId) : undefined;
      const passing = path?.passingScore ?? 70;
      if (e) {
        e.finalScore = input.finalScore;
        e.status = input.finalScore >= passing ? 'COMPLETED' : 'FAILED';
        e.completedOn = new Date().toISOString().slice(0, 10);
        e.progressPct = 100;
        if (e.status === 'COMPLETED') {
          e.certificateCode = 'CERT-' + Date.now().toString(36).toUpperCase();
          if (path?.validityMonths) {
            const d = new Date();
            d.setMonth(d.getMonth() + path.validityMonths);
            e.expiresOn = d.toISOString().slice(0, 10);
          }
        }
        e.updatedAt = new Date().toISOString();
        return of(e).pipe(delay(120));
      }
      return of(this.mockEnrollments[0]).pipe(delay(120));
    }
    return this.http.post<EnrollmentResponse>(`${this.endpoint}/enrollments/${id}/complete`, input);
  }

  cancelEnrollment(id: string): Observable<EnrollmentResponse> {
    if (environment.useMockApi) {
      const e = this.mockEnrollments.find(x => x.id === id);
      if (e) { e.status = 'CANCELLED'; e.updatedAt = new Date().toISOString(); return of(e).pipe(delay(120)); }
      return of(this.mockEnrollments[0]).pipe(delay(120));
    }
    return this.http.post<EnrollmentResponse>(`${this.endpoint}/enrollments/${id}/cancel`, {});
  }

  // ---------- Seeds ----------

  private seedSkills(): SkillResponse[] {
    const now = new Date().toISOString();
    return [
      { id: 'skill-1', tenantId: 'demo-tenant', code: 'iso-9001-internal-auditor',
        name: 'Auditeur interne ISO 9001', description: 'Maîtrise ISO 19011 + clauses 9001.',
        category: 'Audit', createdAt: now, updatedAt: now },
      { id: 'skill-2', tenantId: 'demo-tenant', code: 'spc-control-charts',
        name: 'Cartes de contrôle SPC', description: 'X-R, X-S, p, np, EWMA, CUSUM.',
        category: 'DMAIC', createdAt: now, updatedAt: now },
      { id: 'skill-3', tenantId: 'demo-tenant', code: 'ishikawa-facilitation',
        name: 'Animation Ishikawa', description: 'Facilitation atelier 6M.',
        category: 'Méthodes', createdAt: now, updatedAt: now },
      { id: 'skill-4', tenantId: 'demo-tenant', code: 'capa-rca-5whys',
        name: 'Root cause analysis 5 Whys / CAPA', description: 'Application méthode 5 pourquoi sur NC.',
        category: 'CAPA', createdAt: now, updatedAt: now }
    ];
  }

  private seedPaths(): PathResponse[] {
    const now = new Date().toISOString();
    return [
      { id: 'path-1', tenantId: 'demo-tenant',
        code: 'yellow-belt-quality', name: 'Yellow Belt Qualité',
        description: 'Bases qualité — PDCA, 5S, Ishikawa, CAPA.',
        targetRole: 'Opérateur qualifié',
        durationHours: 14, passingScore: 70, validityMonths: 36,
        status: 'ACTIVE', createdBy: 'demo-user',
        createdAt: now, updatedAt: now },
      { id: 'path-2', tenantId: 'demo-tenant',
        code: 'green-belt-six-sigma', name: 'Green Belt Six Sigma',
        description: 'DMAIC complet + Poka-Yoke + outils statistiques.',
        targetRole: 'Pilote projet qualité',
        durationHours: 80, passingScore: 75, validityMonths: 36,
        status: 'ACTIVE', createdBy: 'demo-user',
        createdAt: now, updatedAt: now },
      { id: 'path-3', tenantId: 'demo-tenant',
        code: 'iso-9001-lead-auditor', name: 'Lead Auditor ISO 9001',
        description: 'Audit interne + externe ISO 9001:2015 selon ISO 19011.',
        targetRole: 'Auditeur',
        durationHours: 40, passingScore: 80, validityMonths: 36,
        status: 'DRAFT', createdBy: 'demo-user',
        createdAt: now, updatedAt: now }
    ];
  }

  private seedRequirements(): Record<string, SkillRequirementResponse[]> {
    const now = new Date().toISOString();
    return {
      'path-1': [
        { id: 'req-1', pathId: 'path-1', skillId: 'skill-3', targetLevel: 2, createdAt: now },
        { id: 'req-2', pathId: 'path-1', skillId: 'skill-4', targetLevel: 2, createdAt: now }
      ],
      'path-2': [
        { id: 'req-3', pathId: 'path-2', skillId: 'skill-2', targetLevel: 3, createdAt: now },
        { id: 'req-4', pathId: 'path-2', skillId: 'skill-4', targetLevel: 3, createdAt: now }
      ],
      'path-3': [
        { id: 'req-5', pathId: 'path-3', skillId: 'skill-1', targetLevel: 4, createdAt: now }
      ]
    };
  }

  private seedEnrollments(): EnrollmentResponse[] {
    const now = new Date().toISOString();
    return [
      { id: 'enr-1', tenantId: 'demo-tenant', userId: 'demo-user', pathId: 'path-1',
        status: 'IN_PROGRESS', progressPct: 40,
        enrolledOn: '2026-04-12', startedOn: '2026-04-15',
        createdAt: now, updatedAt: now },
      { id: 'enr-2', tenantId: 'demo-tenant', userId: 'demo-user', pathId: 'path-2',
        status: 'ENROLLED', progressPct: 0,
        enrolledOn: '2026-05-04',
        createdAt: now, updatedAt: now }
    ];
  }
}
