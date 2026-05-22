import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CreateKpiRequest,
  KpiCurrentStatus,
  KpiDirection,
  KpiHealth,
  KpiPage,
  KpiResponse,
  KpiStatus,
  KpiTrend,
  MeasurementPage,
  MeasurementResponse,
  RecordMeasurementRequest,
  UpdateKpiRequest
} from './kpis.types';

@Injectable({ providedIn: 'root' })
export class KpisService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/kpis`;

  private readonly mockKpis: KpiResponse[] = this.seedKpis();
  private readonly mockMeasurements: Record<string, MeasurementResponse[]> = this.seedMeasurements();

  constructor(private readonly http: HttpClient) {}

  // ---------- Definitions ----------

  list(page = 0, size = 50, status?: KpiStatus, category?: string): Observable<KpiPage> {
    if (environment.useMockApi) {
      const f = this.mockKpis
        .filter(k => !status   || k.status   === status)
        .filter(k => !category || k.category === category);
      return of({ content: f, totalElements: f.length, totalPages: 1, number: 0, size: f.length }).pipe(delay(120));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status)   params = params.set('status',   status);
    if (category) params = params.set('category', category);
    return this.http.get<KpiPage>(this.endpoint, { params });
  }

  get(id: string): Observable<KpiResponse> {
    if (environment.useMockApi) {
      return of(this.mockKpis.find(k => k.id === id) ?? this.mockKpis[0]).pipe(delay(100));
    }
    return this.http.get<KpiResponse>(`${this.endpoint}/${id}`);
  }

  create(input: CreateKpiRequest): Observable<KpiResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const k: KpiResponse = {
        id: 'kpi-' + (this.mockKpis.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        code: input.code, name: input.name, description: input.description,
        category: input.category, unit: input.unit,
        direction: input.direction, frequency: input.frequency,
        targetValue: input.targetValue,
        thresholdWarning: input.thresholdWarning,
        thresholdCritical: input.thresholdCritical,
        status: 'DRAFT',
        applicableIndustriesCsv: input.applicableIndustriesCsv,
        ownerUserId: input.ownerUserId,
        createdBy: input.createdBy,
        createdAt: now, updatedAt: now
      };
      this.mockKpis.unshift(k);
      this.mockMeasurements[k.id] = [];
      return of(k).pipe(delay(150));
    }
    return this.http.post<KpiResponse>(this.endpoint, input);
  }

  update(id: string, input: UpdateKpiRequest): Observable<KpiResponse> {
    if (environment.useMockApi) {
      const k = this.mockKpis.find(x => x.id === id);
      if (k) { Object.assign(k, input); k.updatedAt = new Date().toISOString(); return of(k).pipe(delay(120)); }
      return of(this.mockKpis[0]).pipe(delay(120));
    }
    return this.http.patch<KpiResponse>(`${this.endpoint}/${id}`, input);
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockKpis.findIndex(k => k.id === id);
      if (i >= 0) this.mockKpis.splice(i, 1);
      delete this.mockMeasurements[id];
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  activate(id: string): Observable<KpiResponse> { return this.transition(id, 'activate', 'ACTIVE'); }
  reopen(id: string):   Observable<KpiResponse> { return this.transition(id, 'reopen',   'DRAFT'); }
  archive(id: string):  Observable<KpiResponse> { return this.transition(id, 'archive',  'ARCHIVED'); }

  private transition(id: string, op: 'activate' | 'reopen' | 'archive', target: KpiStatus): Observable<KpiResponse> {
    if (environment.useMockApi) {
      const k = this.mockKpis.find(x => x.id === id);
      if (k) { k.status = target; k.updatedAt = new Date().toISOString(); return of(k).pipe(delay(120)); }
      return of(this.mockKpis[0]).pipe(delay(120));
    }
    return this.http.post<KpiResponse>(`${this.endpoint}/${id}/${op}`, {});
  }

  // ---------- Status / trend ----------

  currentStatus(id: string): Observable<KpiCurrentStatus> {
    if (environment.useMockApi) {
      const k = this.mockKpis.find(x => x.id === id);
      const ms = this.mockMeasurements[id] ?? [];
      const latest = ms[ms.length - 1];
      return of({
        kpiId: id,
        code: k?.code ?? '',
        name: k?.name ?? '',
        definitionStatus: k?.status ?? 'DRAFT',
        direction: k?.direction ?? 'HIGHER_IS_BETTER',
        latestValue: latest?.value,
        unit: k?.unit,
        latestPeriodStart: latest?.periodStart,
        latestPeriodEnd: latest?.periodEnd,
        health: latest?.health ?? 'UNKNOWN',
        targetValue: k?.targetValue,
        thresholdWarning: k?.thresholdWarning,
        thresholdCritical: k?.thresholdCritical
      }).pipe(delay(100));
    }
    return this.http.get<KpiCurrentStatus>(`${this.endpoint}/${id}/status`);
  }

  trend(id: string): Observable<KpiTrend> {
    if (environment.useMockApi) {
      const k = this.mockKpis.find(x => x.id === id);
      const ms = this.mockMeasurements[id] ?? [];
      return of({
        kpiId: id, code: k?.code ?? '',
        sampleCount: ms.length,
        points: ms.map(m => ({
          periodStart: m.periodStart, periodEnd: m.periodEnd,
          value: m.value, health: m.health
        }))
      }).pipe(delay(100));
    }
    return this.http.get<KpiTrend>(`${this.endpoint}/${id}/trend`);
  }

  // ---------- Measurements ----------

  listMeasurements(kpiId: string, page = 0, size = 100): Observable<MeasurementPage> {
    if (environment.useMockApi) {
      const arr = this.mockMeasurements[kpiId] ?? [];
      return of({ content: arr, totalElements: arr.length, totalPages: 1, number: 0, size: arr.length }).pipe(delay(100));
    }
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<MeasurementPage>(`${this.endpoint}/${kpiId}/measurements`, { params });
  }

  record(kpiId: string, input: RecordMeasurementRequest): Observable<MeasurementResponse> {
    if (environment.useMockApi) {
      const k = this.mockKpis.find(x => x.id === kpiId);
      const m: MeasurementResponse = {
        id: 'mes-' + Math.random().toString(36).slice(2, 9),
        tenantId: 'demo-tenant',
        kpiId,
        periodStart: input.periodStart,
        periodEnd: input.periodEnd,
        value: input.value,
        unit: input.unit ?? k?.unit,
        source: input.source ?? 'MANUAL',
        recordedByUserId: input.recordedByUserId,
        notes: input.notes,
        health: this.computeHealth(input.value, k),
        createdAt: new Date().toISOString()
      };
      const arr = this.mockMeasurements[kpiId] ?? [];
      arr.push(m);
      this.mockMeasurements[kpiId] = arr;
      return of(m).pipe(delay(120));
    }
    return this.http.post<MeasurementResponse>(`${this.endpoint}/${kpiId}/measurements`, input);
  }

  deleteMeasurement(kpiId: string, id: string): Observable<void> {
    if (environment.useMockApi) {
      const arr = this.mockMeasurements[kpiId] ?? [];
      const i = arr.findIndex(m => m.id === id);
      if (i >= 0) arr.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${kpiId}/measurements/${id}`);
  }

  // ---------- Helpers ----------

  private computeHealth(value: number, k?: KpiResponse): KpiHealth {
    if (!k || (k.thresholdWarning === undefined && k.thresholdCritical === undefined)) return 'UNKNOWN';
    const w = k.thresholdWarning, c = k.thresholdCritical;
    if (k.direction === 'HIGHER_IS_BETTER') {
      // higher is better → critical when value below critical threshold
      if (c !== undefined && value <= c) return 'CRITICAL';
      if (w !== undefined && value <= w) return 'WARNING';
      return 'OK';
    } else {
      // lower is better
      if (c !== undefined && value >= c) return 'CRITICAL';
      if (w !== undefined && value >= w) return 'WARNING';
      return 'OK';
    }
  }

  // ---------- Seeds ----------

  private seedKpis(): KpiResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'kpi-1', tenantId: 'demo-tenant',
        code: 'capa-closure-time', name: 'Délai moyen de clôture CAPA',
        description: 'Délai entre ouverture et clôture des cas CAPA HIGH/CRITICAL.',
        category: 'CAPA', unit: 'jours',
        direction: 'LOWER_IS_BETTER', frequency: 'MONTHLY',
        targetValue: 30, thresholdWarning: 45, thresholdCritical: 60,
        status: 'ACTIVE',
        applicableIndustriesCsv: 'all',
        ownerUserId: 'demo-user', createdBy: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'kpi-2', tenantId: 'demo-tenant',
        code: 'first-pass-yield', name: 'First Pass Yield',
        description: 'Taux de produits bons du premier coup.',
        category: 'Qualité', unit: '%',
        direction: 'HIGHER_IS_BETTER', frequency: 'WEEKLY',
        targetValue: 98, thresholdWarning: 95, thresholdCritical: 90,
        status: 'ACTIVE',
        applicableIndustriesCsv: 'Manufacturing,Pharma',
        ownerUserId: 'demo-user', createdBy: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'kpi-3', tenantId: 'demo-tenant',
        code: 'dpmo', name: 'DPMO — Defects Per Million Opportunities',
        description: 'Indicateur Six Sigma. Cible niveau 6 σ ≤ 3.4 DPMO.',
        category: 'DMAIC', unit: 'DPMO',
        direction: 'LOWER_IS_BETTER', frequency: 'MONTHLY',
        targetValue: 3.4, thresholdWarning: 100, thresholdCritical: 1000,
        status: 'DRAFT',
        applicableIndustriesCsv: 'Manufacturing',
        createdBy: 'demo-user',
        createdAt: now, updatedAt: now
      }
    ];
  }

  private seedMeasurements(): Record<string, MeasurementResponse[]> {
    const now = Date.now();
    const days = (n: number) => new Date(now - n * 86400000).toISOString();
    return {
      'kpi-1': [
        { id: 'mes-1', tenantId: 'demo-tenant', kpiId: 'kpi-1',
          periodStart: days(120), periodEnd: days(90), value: 52,
          unit: 'jours', source: 'COMPUTED', health: 'WARNING',
          createdAt: days(89) },
        { id: 'mes-2', tenantId: 'demo-tenant', kpiId: 'kpi-1',
          periodStart: days(90), periodEnd: days(60), value: 41,
          unit: 'jours', source: 'COMPUTED', health: 'WARNING',
          createdAt: days(59) },
        { id: 'mes-3', tenantId: 'demo-tenant', kpiId: 'kpi-1',
          periodStart: days(60), periodEnd: days(30), value: 33,
          unit: 'jours', source: 'COMPUTED', health: 'OK',
          createdAt: days(29) },
        { id: 'mes-4', tenantId: 'demo-tenant', kpiId: 'kpi-1',
          periodStart: days(30), periodEnd: days(0), value: 28,
          unit: 'jours', source: 'COMPUTED', health: 'OK',
          createdAt: days(0) }
      ],
      'kpi-2': [
        { id: 'mes-5', tenantId: 'demo-tenant', kpiId: 'kpi-2',
          periodStart: days(28), periodEnd: days(21), value: 96.2,
          unit: '%', source: 'COMPUTED', health: 'OK',
          createdAt: days(20) },
        { id: 'mes-6', tenantId: 'demo-tenant', kpiId: 'kpi-2',
          periodStart: days(21), periodEnd: days(14), value: 94.8,
          unit: '%', source: 'COMPUTED', health: 'WARNING',
          createdAt: days(13) },
        { id: 'mes-7', tenantId: 'demo-tenant', kpiId: 'kpi-2',
          periodStart: days(14), periodEnd: days(7), value: 93.1,
          unit: '%', source: 'COMPUTED', health: 'WARNING',
          createdAt: days(6) },
        { id: 'mes-8', tenantId: 'demo-tenant', kpiId: 'kpi-2',
          periodStart: days(7), periodEnd: days(0), value: 89.4,
          unit: '%', source: 'COMPUTED', health: 'CRITICAL',
          createdAt: days(0) }
      ],
      'kpi-3': []
    };
  }
}
