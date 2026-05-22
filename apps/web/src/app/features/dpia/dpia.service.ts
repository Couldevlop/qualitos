import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CreateDpiaRequest,
  DpiaStatus,
  DpiaView,
  EditDpiaRequest,
  OpinionRequest,
  RiskLevel,
  StartDpiaRequest
} from './dpia.types';

@Injectable({ providedIn: 'root' })
export class DpiaService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/gdpr/dpias`;
  private readonly mockStore: DpiaView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  static requiresPriorConsultation(level: RiskLevel): boolean {
    return level === 'HIGH' || level === 'SEVERE';
  }

  list(status?: DpiaStatus): Observable<DpiaView[]> {
    if (environment.useMockApi) {
      const f = status ? this.mockStore.filter(d => d.status === status) : this.mockStore;
      return of(f).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<DpiaView[]>(this.endpoint, { params });
  }

  requiringConsultation(): Observable<DpiaView[]> {
    if (environment.useMockApi) {
      const arr = this.mockStore.filter(d =>
        d.consultationRequired
        && (d.status === 'APPROVED' || d.status === 'DPO_REVIEW' || d.status === 'IN_PROGRESS'));
      return of(arr).pipe(delay(120));
    }
    return this.http.get<DpiaView[]>(`${this.endpoint}/requiring-consultation`);
  }

  get(id: string): Observable<DpiaView> {
    if (environment.useMockApi) {
      return of(this.mockStore.find(d => d.id === id) ?? this.mockStore[0]).pipe(delay(100));
    }
    return this.http.get<DpiaView>(`${this.endpoint}/${id}`);
  }

  getByReference(reference: string): Observable<DpiaView> {
    if (environment.useMockApi) {
      const d = this.mockStore.find(x => x.reference === reference);
      return of(d ?? this.mockStore[0]).pipe(delay(100));
    }
    const params = new HttpParams().set('reference', reference);
    return this.http.get<DpiaView>(`${this.endpoint}/by-reference`, { params });
  }

  create(input: CreateDpiaRequest): Observable<DpiaView> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const d: DpiaView = {
        id: 'dpia-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        reference: input.reference, title: input.title, description: input.description,
        linkedProcessingActivityIds: input.linkedProcessingActivityIds ?? [],
        overallRiskLevel: input.initialRiskLevel,
        consultationRequired: DpiaService.requiresPriorConsultation(input.initialRiskLevel),
        status: 'DRAFT',
        createdByUserId: input.createdByUserId,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(d);
      return of(d).pipe(delay(150));
    }
    return this.http.post<DpiaView>(this.endpoint, input);
  }

  edit(id: string, input: EditDpiaRequest): Observable<DpiaView> {
    if (environment.useMockApi) {
      const d = this.mockStore.find(x => x.id === id);
      if (d) {
        Object.assign(d, input, {
          linkedProcessingActivityIds: input.linkedProcessingActivityIds ?? []
        });
        d.updatedAt = new Date().toISOString();
        return of(d).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.put<DpiaView>(`${this.endpoint}/${id}`, input);
  }

  start(id: string, body: StartDpiaRequest): Observable<DpiaView> {
    return this.transition(id, 'start', 'IN_PROGRESS', body);
  }

  returnToDraft(id: string): Observable<DpiaView> {
    return this.transition(id, 'return-to-draft', 'DRAFT');
  }

  submitToDpo(id: string): Observable<DpiaView> {
    return this.transition(id, 'submit-to-dpo', 'DPO_REVIEW');
  }

  approve(id: string, body: OpinionRequest): Observable<DpiaView> {
    if (environment.useMockApi) {
      const d = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (d) {
        d.status = 'APPROVED';
        d.dpoUserId = body.dpoUserId;
        d.dpoOpinion = body.dpoOpinion;
        d.dpoOpinionAt = now;
        d.effectiveFrom = now;
        d.updatedAt = now;
        return of(d).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<DpiaView>(`${this.endpoint}/${id}/approve`, body);
  }

  reject(id: string, body: OpinionRequest): Observable<DpiaView> {
    if (environment.useMockApi) {
      const d = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (d) {
        d.status = 'REJECTED';
        d.dpoUserId = body.dpoUserId;
        d.dpoOpinion = body.dpoOpinion;
        d.dpoOpinionAt = now;
        d.updatedAt = now;
        return of(d).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<DpiaView>(`${this.endpoint}/${id}/reject`, body);
  }

  archive(id: string): Observable<DpiaView> {
    return this.transition(id, 'archive', 'ARCHIVED');
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(d => d.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  // ---- Internals ----

  private transition(
    id: string, op: string, target: DpiaStatus,
    body?: StartDpiaRequest
  ): Observable<DpiaView> {
    if (environment.useMockApi) {
      const d = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (d) {
        d.status = target;
        if (target === 'IN_PROGRESS' && body?.handledByUserId) d.handledByUserId = body.handledByUserId;
        if (target === 'ARCHIVED') d.effectiveTo = now;
        d.updatedAt = now;
        return of(d).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<DpiaView>(`${this.endpoint}/${id}/${op}`, body ?? {});
  }

  private seed(): DpiaView[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'dpia-1', tenantId: 'demo-tenant',
        reference: 'TELEMEDICINE-V1',
        title: 'Plateforme téléconsultation patients chroniques',
        description: 'Évaluation d\'impact pour le nouveau service de téléconsultation incluant échange vidéo, partage d\'imagerie et notes médicales.',
        linkedProcessingActivityIds: [],
        necessityAndProportionalityNotes: 'Le traitement est nécessaire à la prestation médicale (Art. 9§2.h). Données minimisées au strict besoin clinique.',
        risksToRightsAndFreedoms: 'Risque d\'accès non autorisé aux données de santé (Art. 9). Risque de fuite via le canal vidéo. Risque de re-identification par croisement.',
        mitigationMeasures: 'Chiffrement E2E des flux vidéo, MFA obligatoire patient+médecin, audit logs immutables, hébergement HDS, formation RGPD trimestrielle des médecins.',
        overallRiskLevel: 'HIGH',
        consultationRequired: true,
        consultationNotes: 'CNIL contactée le 2026-04-12 — réponse attendue sous 8 semaines (Art. 36§2).',
        status: 'APPROVED',
        dpoUserId: 'demo-user',
        dpoOpinion: 'Avis favorable sous réserve de la consultation préalable CNIL en cours et du déploiement effectif des mesures de mitigation listées.',
        dpoOpinionAt: now, effectiveFrom: now,
        createdByUserId: 'demo-user', handledByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'dpia-2', tenantId: 'demo-tenant',
        reference: 'EMPLOYEE-AI-MONITORING',
        title: 'IA d\'analyse de qualité des appels SAV',
        description: 'Système d\'analyse vocale automatisée des appels du SAV à des fins de mesure de qualité du service.',
        linkedProcessingActivityIds: [],
        necessityAndProportionalityNotes: 'Nécessité partielle. Une approche par échantillonnage humain a été écartée pour des raisons opérationnelles.',
        risksToRightsAndFreedoms: 'Surveillance disproportionnée des salariés (Art. 88 RGPD + L1121-1 Code du travail). Risque de scoring algorithmique.',
        mitigationMeasures: 'En cours d\'analyse. Pistes : anonymisation des locuteurs, opt-in salarié, exclusion des appels personnels.',
        overallRiskLevel: 'SEVERE',
        consultationRequired: true,
        status: 'DPO_REVIEW',
        createdByUserId: 'demo-user', handledByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'dpia-3', tenantId: 'demo-tenant',
        reference: 'CRM-CONSOLIDATION-V2',
        title: 'Consolidation CRM multi-tenant',
        description: 'Migration des CRM filiales vers une instance unique mutualisée.',
        linkedProcessingActivityIds: [],
        overallRiskLevel: 'MEDIUM',
        consultationRequired: false,
        status: 'DRAFT',
        createdByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      }
    ];
  }
}
