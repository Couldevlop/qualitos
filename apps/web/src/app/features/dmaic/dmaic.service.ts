import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  AddMeasureRequest,
  AssignPokaYokeRequest,
  AssignmentResponse,
  CapabilityResponse,
  CreateDmaicProjectRequest,
  DeviceDetail,
  DeviceSummary,
  DeviceSummaryPage,
  DmaicPhase,
  DmaicProjectPage,
  DmaicProjectResponse,
  DmaicStatus,
  MeasureResponse,
  PokaYokeMechanism,
  PokaYokeType,
  UpdateAssignmentRequest,
  UpdateDmaicProjectRequest
} from './dmaic.types';

@Injectable({ providedIn: 'root' })
export class DmaicService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/dmaic`;

  private readonly mockProjects: DmaicProjectResponse[] = this.seedMockProjects();
  private readonly mockMeasures: Record<string, MeasureResponse[]> = {};
  private readonly mockAssignments: Record<string, AssignmentResponse[]> = {};
  private readonly mockDevices: DeviceSummary[] = this.seedMockDevices();

  constructor(private readonly http: HttpClient) {}

  // ---------- Projects ----------

  listProjects(
    page = 0,
    size = 20,
    status?: DmaicStatus,
    phase?: DmaicPhase
  ): Observable<DmaicProjectPage> {
    if (environment.useMockApi) {
      const filtered = this.mockProjects
        .filter(p => !status || p.status === status)
        .filter(p => !phase  || p.phase  === phase);
      return of({
        content: filtered, totalElements: filtered.length,
        totalPages: 1, number: 0, size: filtered.length
      }).pipe(delay(120));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (phase)  params = params.set('phase',  phase);
    return this.http.get<DmaicProjectPage>(`${this.endpoint}/projects`, { params });
  }

  getProject(id: string): Observable<DmaicProjectResponse> {
    if (environment.useMockApi) {
      const found = this.mockProjects.find(p => p.id === id);
      return of(found ?? this.mockProjects[0]).pipe(delay(120));
    }
    return this.http.get<DmaicProjectResponse>(`${this.endpoint}/projects/${id}`);
  }

  createProject(input: CreateDmaicProjectRequest): Observable<DmaicProjectResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const p: DmaicProjectResponse = {
        id: 'dmaic-' + (this.mockProjects.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        title: input.title,
        problemStatement: input.problemStatement,
        goalStatement: input.goalStatement,
        phase: 'DEFINE',
        status: 'ACTIVE',
        championId: input.championId,
        blackBeltId: input.blackBeltId,
        targetCompletionDate: input.targetCompletionDate,
        specLowerLimit: input.specLowerLimit,
        specUpperLimit: input.specUpperLimit,
        specTarget: input.specTarget,
        specUnit: input.specUnit,
        estimatedSavingsEur: input.estimatedSavingsEur,
        measureCount: 0,
        pokaYokeCount: 0,
        startedAt: now,
        createdAt: now,
        updatedAt: now
      };
      this.mockProjects.unshift(p);
      return of(p).pipe(delay(150));
    }
    return this.http.post<DmaicProjectResponse>(`${this.endpoint}/projects`, input);
  }

  updateProject(id: string, input: UpdateDmaicProjectRequest): Observable<DmaicProjectResponse> {
    if (environment.useMockApi) {
      const p = this.mockProjects.find(x => x.id === id);
      if (p) {
        Object.assign(p, input);
        p.updatedAt = new Date().toISOString();
        return of(p).pipe(delay(120));
      }
      return of(this.mockProjects[0]).pipe(delay(120));
    }
    return this.http.patch<DmaicProjectResponse>(`${this.endpoint}/projects/${id}`, input);
  }

  advance(id: string):  Observable<DmaicProjectResponse> { return this.transition(id, 'advance');  }
  hold(id: string):     Observable<DmaicProjectResponse> { return this.transition(id, 'hold');     }
  resume(id: string):   Observable<DmaicProjectResponse> { return this.transition(id, 'resume');   }
  cancel(id: string):   Observable<DmaicProjectResponse> { return this.transition(id, 'cancel');   }

  private transition(
    id: string,
    op: 'advance' | 'hold' | 'resume' | 'cancel'
  ): Observable<DmaicProjectResponse> {
    if (environment.useMockApi) {
      const p = this.mockProjects.find(x => x.id === id);
      if (p) {
        const order: DmaicPhase[] = ['DEFINE', 'MEASURE', 'ANALYZE', 'IMPROVE', 'CONTROL'];
        if (op === 'advance') {
          const i = order.indexOf(p.phase);
          if (i >= 0 && i < order.length - 1) p.phase = order[i + 1];
          else if (i === order.length - 1) {
            p.status = 'COMPLETED';
            p.completedAt = new Date().toISOString();
          }
        }
        if (op === 'hold')   p.status = 'ON_HOLD';
        if (op === 'resume') p.status = 'ACTIVE';
        if (op === 'cancel') p.status = 'CANCELLED';
        p.updatedAt = new Date().toISOString();
        return of(p).pipe(delay(120));
      }
      return of(this.mockProjects[0]).pipe(delay(120));
    }
    return this.http.patch<DmaicProjectResponse>(`${this.endpoint}/projects/${id}/${op}`, {});
  }

  deleteProject(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockProjects.findIndex(p => p.id === id);
      if (i >= 0) this.mockProjects.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/projects/${id}`);
  }

  // ---------- Measures ----------

  addMeasure(projectId: string, input: AddMeasureRequest): Observable<MeasureResponse> {
    if (environment.useMockApi) {
      const m: MeasureResponse = {
        id: 'meas-' + Math.random().toString(36).slice(2, 9),
        projectId,
        value: input.value,
        subgroupId: input.subgroupId,
        sourceRef: input.sourceRef,
        recordedAt: input.recordedAt ?? new Date().toISOString(),
        operatorId: input.operatorId,
        note: input.note,
        createdAt: new Date().toISOString()
      };
      const arr = this.mockMeasures[projectId] ?? [];
      arr.push(m);
      this.mockMeasures[projectId] = arr;
      const p = this.mockProjects.find(x => x.id === projectId);
      if (p) p.measureCount = arr.length;
      return of(m).pipe(delay(120));
    }
    return this.http.post<MeasureResponse>(`${this.endpoint}/projects/${projectId}/measures`, input);
  }

  deleteMeasure(projectId: string, measureId: string): Observable<void> {
    if (environment.useMockApi) {
      const arr = this.mockMeasures[projectId] ?? [];
      const i = arr.findIndex(m => m.id === measureId);
      if (i >= 0) arr.splice(i, 1);
      const p = this.mockProjects.find(x => x.id === projectId);
      if (p) p.measureCount = arr.length;
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/projects/${projectId}/measures/${measureId}`);
  }

  capability(projectId: string): Observable<CapabilityResponse> {
    if (environment.useMockApi) {
      const arr = this.mockMeasures[projectId] ?? [];
      const n = arr.length;
      if (n < 2) {
        return of({ sampleSize: n, warnings: ['Au moins 2 mesures requises.'] }).pipe(delay(120));
      }
      const values = arr.map(m => m.value);
      const mean = values.reduce((a, b) => a + b, 0) / n;
      const variance = values.reduce((s, v) => s + (v - mean) ** 2, 0) / (n - 1);
      const stdDev = Math.sqrt(variance);
      const p = this.mockProjects.find(x => x.id === projectId);
      const usl = p?.specUpperLimit;
      const lsl = p?.specLowerLimit;
      const computeCp = usl !== undefined && lsl !== undefined && stdDev > 0
        ? (usl - lsl) / (6 * stdDev)
        : undefined;
      const cpu = usl !== undefined && stdDev > 0 ? (usl - mean) / (3 * stdDev) : undefined;
      const cpl = lsl !== undefined && stdDev > 0 ? (mean - lsl) / (3 * stdDev) : undefined;
      const cpk = (cpu !== undefined && cpl !== undefined) ? Math.min(cpu, cpl)
                : (cpu !== undefined ? cpu : cpl);
      return of({
        sampleSize: n, mean, stdDev,
        min: Math.min(...values), max: Math.max(...values),
        specLowerLimit: lsl, specUpperLimit: usl, specTarget: p?.specTarget,
        cp: computeCp, cpk, cpu, cpl,
        sigmaLevel: cpk !== undefined ? cpk * 3 : undefined,
        interpretation: cpk !== undefined && cpk >= 1.33 ? 'Processus capable.'
                       : cpk !== undefined && cpk >= 1.0 ? 'Processus marginalement capable.'
                       : 'Processus à améliorer.',
        warnings: n < 30 ? ['Échantillon < 30 — précision réduite.'] : []
      }).pipe(delay(150));
    }
    return this.http.get<CapabilityResponse>(`${this.endpoint}/projects/${projectId}/capability`);
  }

  listMeasures(projectId: string): MeasureResponse[] {
    return this.mockMeasures[projectId] ?? [];
  }

  // ---------- Poka-Yoke catalog ----------

  listDevices(
    page = 0,
    size = 50,
    type?: PokaYokeType,
    mechanism?: PokaYokeMechanism
  ): Observable<DeviceSummaryPage> {
    if (environment.useMockApi) {
      const filtered = this.mockDevices
        .filter(d => !type      || d.type      === type)
        .filter(d => !mechanism || d.mechanism === mechanism);
      return of({
        content: filtered, totalElements: filtered.length,
        totalPages: 1, number: 0, size: filtered.length
      }).pipe(delay(120));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (type)      params = params.set('type', type);
    if (mechanism) params = params.set('mechanism', mechanism);
    return this.http.get<DeviceSummaryPage>(`${this.endpoint}/pokayoke`, { params });
  }

  getDevice(id: string): Observable<DeviceDetail> {
    if (environment.useMockApi) {
      const d = this.mockDevices.find(x => x.id === id);
      const now = new Date().toISOString();
      return of({
        ...(d ?? this.mockDevices[0]),
        description: 'Dispositif Poka-Yoke. Détails démo.',
        examples: 'Vis détrompeur, guide de positionnement.',
        createdAt: now, updatedAt: now
      }).pipe(delay(120));
    }
    return this.http.get<DeviceDetail>(`${this.endpoint}/pokayoke/${id}`);
  }

  // ---------- Assignments ----------

  listAssignments(projectId: string): AssignmentResponse[] {
    return this.mockAssignments[projectId] ?? [];
  }

  assignDevice(projectId: string, input: AssignPokaYokeRequest): Observable<AssignmentResponse> {
    if (environment.useMockApi) {
      const dev = this.mockDevices.find(d => d.id === input.deviceId) ?? this.mockDevices[0];
      const a: AssignmentResponse = {
        id: 'assign-' + Math.random().toString(36).slice(2, 9),
        projectId,
        deviceId: dev.id, deviceCode: dev.code, deviceName: dev.name, deviceType: dev.type,
        status: 'PROPOSED',
        note: input.note,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
      const arr = this.mockAssignments[projectId] ?? [];
      arr.push(a);
      this.mockAssignments[projectId] = arr;
      const p = this.mockProjects.find(x => x.id === projectId);
      if (p) p.pokaYokeCount = arr.length;
      return of(a).pipe(delay(120));
    }
    return this.http.post<AssignmentResponse>(`${this.endpoint}/projects/${projectId}/pokayoke`, input);
  }

  updateAssignment(
    projectId: string, assignmentId: string, input: UpdateAssignmentRequest
  ): Observable<AssignmentResponse> {
    if (environment.useMockApi) {
      const arr = this.mockAssignments[projectId] ?? [];
      const a = arr.find(x => x.id === assignmentId);
      if (a) {
        if (input.status !== undefined) a.status = input.status;
        if (input.note   !== undefined) a.note   = input.note;
        if (input.defectReductionPct !== undefined) a.defectReductionPct = input.defectReductionPct;
        a.updatedAt = new Date().toISOString();
        if (a.status === 'IMPLEMENTED' && !a.implementedAt) a.implementedAt = a.updatedAt;
        if (a.status === 'VERIFIED'    && !a.verifiedAt)    a.verifiedAt    = a.updatedAt;
        return of(a).pipe(delay(120));
      }
      return of(arr[0]).pipe(delay(120));
    }
    return this.http.patch<AssignmentResponse>(
      `${this.endpoint}/projects/${projectId}/pokayoke/${assignmentId}`, input
    );
  }

  deleteAssignment(projectId: string, assignmentId: string): Observable<void> {
    if (environment.useMockApi) {
      const arr = this.mockAssignments[projectId] ?? [];
      const i = arr.findIndex(x => x.id === assignmentId);
      if (i >= 0) arr.splice(i, 1);
      const p = this.mockProjects.find(x => x.id === projectId);
      if (p) p.pokaYokeCount = arr.length;
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/projects/${projectId}/pokayoke/${assignmentId}`);
  }

  // ---------- Mock seeds ----------

  private seedMockProjects(): DmaicProjectResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'dmaic-1', tenantId: 'demo-tenant',
        title: 'Réduire le taux de rebut ligne d\'assemblage A',
        problemStatement: 'Taux de rebut 3.2 % au-dessus de la cible 1.5 % depuis 4 mois.',
        goalStatement: 'Atteindre 1.2 % de rebut d\'ici fin trimestre — économies estimées 84 k€/an.',
        phase: 'MEASURE', status: 'ACTIVE',
        blackBeltId: 'demo-user',
        targetCompletionDate: '2026-09-30',
        specLowerLimit: 9.95, specUpperLimit: 10.05, specTarget: 10.0, specUnit: 'mm',
        estimatedSavingsEur: 84000,
        measureCount: 0, pokaYokeCount: 0,
        startedAt: now, createdAt: now, updatedAt: now
      },
      {
        id: 'dmaic-2', tenantId: 'demo-tenant',
        title: 'Stabiliser le temps de cycle banc test 4',
        phase: 'ANALYZE', status: 'ACTIVE',
        blackBeltId: 'demo-user',
        specLowerLimit: 28, specUpperLimit: 35, specTarget: 31, specUnit: 's',
        measureCount: 0, pokaYokeCount: 0,
        startedAt: now, createdAt: now, updatedAt: now
      },
      {
        id: 'dmaic-3', tenantId: 'demo-tenant',
        title: 'CAPA récurrente déballage médicaments — pilote MEDPHARM',
        phase: 'CONTROL', status: 'ACTIVE',
        blackBeltId: 'demo-user',
        measureCount: 0, pokaYokeCount: 0,
        startedAt: now, createdAt: now, updatedAt: now
      }
    ];
  }

  private seedMockDevices(): DeviceSummary[] {
    return [
      { id: 'pk-1', code: 'PK-INT-001', name: 'Verrouillage capot machine (interlock)',
        type: 'PREVENTION', mechanism: 'INTERLOCK',
        applicableIndustries: 'Manufacturing, Pharma', implementationCost: 'LOW' },
      { id: 'pk-2', code: 'PK-VIS-002', name: 'Détection visuelle EPI absent (YOLO)',
        type: 'DETECTION', mechanism: 'VISION',
        applicableIndustries: 'Manufacturing, BTP, Pharma', implementationCost: 'MEDIUM' },
      { id: 'pk-3', code: 'PK-CHK-003', name: 'Checklist 5S obligatoire avant démarrage poste',
        type: 'PREVENTION', mechanism: 'CHECKLIST',
        applicableIndustries: 'All', implementationCost: 'LOW' },
      { id: 'pk-4', code: 'PK-COL-004', name: 'Codage couleur tuyauteries process',
        type: 'PREVENTION', mechanism: 'COLOR_CODING',
        applicableIndustries: 'Process, Pharma, Agro', implementationCost: 'LOW' },
      { id: 'pk-5', code: 'PK-SEN-005', name: 'Capteur poids palette (sous-charge)',
        type: 'DETECTION', mechanism: 'SENSOR',
        applicableIndustries: 'Logistics, Manufacturing', implementationCost: 'MEDIUM' },
      { id: 'pk-6', code: 'PK-POS-006', name: 'Gabarit positionnement pièce (Shingo)',
        type: 'PREVENTION', mechanism: 'POSITION_REFERENCE',
        applicableIndustries: 'Manufacturing, Auto', implementationCost: 'LOW' }
    ];
  }
}
