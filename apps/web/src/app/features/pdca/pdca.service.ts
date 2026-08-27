import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CreatePdcaCycleRequest,
  CreatePdcaStepRequest,
  PdcaCycleResponse,
  PdcaStepEvidence,
  PdcaStepResponse,
  SpringPage
} from './pdca.types';

@Injectable({ providedIn: 'root' })
export class PdcaService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/pdca/cycles`;

  private readonly mockStore: PdcaCycleResponse[] = this.seedMockCycles();

  /** Preuves d'étapes du mode démonstration, rangées par cycle. */
  private readonly mockStepEvidences = new Map<string, PdcaStepEvidence[]>();

  constructor(private readonly http: HttpClient) {}

  /**
   * Copie défensive d'un cycle du store de démonstration.
   *
   * Sans elle, le mock rend l'objet VIVANT du store : l'appelant reçoit une
   * référence qui change sous ses pieds à chaque écriture (impossible de
   * comparer un avant/après, et n'importe quel consommateur peut corrompre le
   * store). Un vrai backend rend toujours un instantané — le mock doit s'aligner.
   */
  private snapshotCycle(cycle: PdcaCycleResponse): PdcaCycleResponse {
    return { ...cycle, steps: cycle.steps.map(s => ({ ...s })) };
  }

  listCycles(page = 0, size = 20, status?: string): Observable<SpringPage<PdcaCycleResponse>> {
    if (environment.useMockApi) {
      return of(this.mockPage(status)).pipe(delay(200));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<SpringPage<PdcaCycleResponse>>(this.endpoint, { params });
  }

  getCycle(id: string): Observable<PdcaCycleResponse> {
    if (environment.useMockApi) {
      const cycle = this.mockStore.find(c => c.id === id);
      return of(this.snapshotCycle(cycle ?? this.mockStore[0])).pipe(delay(150));
    }
    return this.http.get<PdcaCycleResponse>(`${this.endpoint}/${id}`);
  }

  addStep(cycleId: string, input: CreatePdcaStepRequest): Observable<PdcaStepResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const step: PdcaStepResponse = {
        id: 'step-' + Math.random().toString(36).slice(2, 9),
        cycleId,
        phase: input.phase,
        title: input.title,
        description: input.description,
        status: input.status ?? 'PENDING',
        assigneeId: input.assigneeId,
        dueDate: input.dueDate,
        createdAt: now,
        updatedAt: now
      };
      const cycle = this.mockStore.find(c => c.id === cycleId);
      if (cycle) {
        cycle.steps = [...cycle.steps, step];
        cycle.updatedAt = now;
      }
      return of(step).pipe(delay(150));
    }
    return this.http.post<PdcaStepResponse>(`${this.endpoint}/${cycleId}/steps`, input);
  }

  advanceCycle(cycleId: string): Observable<PdcaCycleResponse> {
    if (environment.useMockApi) {
      const cycle = this.mockStore.find(c => c.id === cycleId);
      if (cycle) {
        const order: Array<PdcaCycleResponse['status']> =
          ['PLAN', 'DO', 'CHECK', 'ACT', 'COMPLETED'];
        const idx = order.indexOf(cycle.status);
        if (idx >= 0 && idx < order.length - 1) {
          cycle.status = order[idx + 1];
          cycle.updatedAt = new Date().toISOString();
          if (cycle.status === 'COMPLETED') {
            cycle.completedAt = cycle.updatedAt;
          }
        }
        return of(this.snapshotCycle(cycle)).pipe(delay(150));
      }
      return of(this.snapshotCycle(this.mockStore[0])).pipe(delay(150));
    }
    return this.http.patch<PdcaCycleResponse>(`${this.endpoint}/${cycleId}/advance`, {});
  }

  cancelCycle(cycleId: string): Observable<PdcaCycleResponse> {
    if (environment.useMockApi) {
      const cycle = this.mockStore.find(c => c.id === cycleId);
      if (cycle) {
        cycle.status = 'CANCELLED';
        cycle.updatedAt = new Date().toISOString();
        return of(this.snapshotCycle(cycle)).pipe(delay(150));
      }
      return of(this.snapshotCycle(this.mockStore[0])).pipe(delay(150));
    }
    return this.http.patch<PdcaCycleResponse>(`${this.endpoint}/${cycleId}/cancel`, {});
  }

  createCycle(input: CreatePdcaCycleRequest): Observable<PdcaCycleResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const cycle: PdcaCycleResponse = {
        id: 'demo-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        title: input.title,
        description: input.description,
        status: 'PLAN',
        ownerId: input.ownerId,
        createdAt: now,
        updatedAt: now,
        steps: []
      };
      this.mockStore.unshift(cycle);
      return of(this.snapshotCycle(cycle)).pipe(delay(200));
    }
    return this.http.post<PdcaCycleResponse>(this.endpoint, input);
  }

  // ---- preuves d'ÉTAPE (§3.1, ADR 0061) ---------------------------------------
  // Une étape déclarée faite sans document ne prouve rien : elle affirme. Ces
  // trois appels alimentent la colonne « Preuve » du tableau des étapes. Une
  // étape ne porte qu'UNE pièce, parce qu'une cellule de tableau montre un
  // document, pas une liste.

  /**
   * Toutes les pièces d'étapes du cycle, en un appel : le tableau les range
   * ensuite par étape. Une requête par ligne ferait autant d'allers et retours
   * que d'étapes pour remplir une seule colonne.
   */
  listStepEvidences(cycleId: string): Observable<PdcaStepEvidence[]> {
    if (environment.useMockApi) {
      return of([...this.mockStepEvidenceStore(cycleId)]).pipe(delay(120));
    }
    return this.http.get<PdcaStepEvidence[]>(`${this.endpoint}/${cycleId}/step-evidences`);
  }

  uploadStepEvidence(cycleId: string, stepId: string, file: File): Observable<PdcaStepEvidence> {
    if (environment.useMockApi) {
      const evidence: PdcaStepEvidence = {
        id: 'evd-step-' + Math.random().toString(36).slice(2, 9),
        cycleId,
        stepId,
        contentType: file.type || 'application/octet-stream',
        sizeBytes: file.size,
        originalFilename: file.name,
        createdAt: new Date().toISOString(),
        url: URL.createObjectURL(file)
      };
      this.mockStepEvidenceStore(cycleId).push(evidence);
      return of(evidence).pipe(delay(250));
    }
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<PdcaStepEvidence>(
      `${this.endpoint}/${cycleId}/steps/${stepId}/evidences`, form);
  }

  deleteStepEvidence(cycleId: string, stepId: string, evidenceId: string): Observable<void> {
    if (environment.useMockApi) {
      const store = this.mockStepEvidenceStore(cycleId);
      const idx = store.findIndex(e => e.id === evidenceId);
      if (idx >= 0) {
        // Libère l'URL d'objet : sans cela le binaire reste en mémoire jusqu'au
        // rechargement de la page.
        const url = store[idx].url;
        if (url) URL.revokeObjectURL(url);
        store.splice(idx, 1);
      }
      return of(void 0).pipe(delay(120));
    }
    return this.http.delete<void>(
      `${this.endpoint}/${cycleId}/steps/${stepId}/evidences/${evidenceId}`);
  }

  private mockStepEvidenceStore(cycleId: string): PdcaStepEvidence[] {
    let store = this.mockStepEvidences.get(cycleId);
    if (!store) {
      store = [];
      this.mockStepEvidences.set(cycleId, store);
    }
    return store;
  }

  private mockPage(status?: string): SpringPage<PdcaCycleResponse> {
    const filtered = (status ? this.mockStore.filter(c => c.status === status) : this.mockStore)
      .map(c => this.snapshotCycle(c));
    return {
      content: filtered,
      totalElements: filtered.length,
      totalPages: 1,
      number: 0,
      size: filtered.length
    };
  }

  private seedMockCycles(): PdcaCycleResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'demo-1', tenantId: 'demo-tenant',
        title: 'Réduction des défauts de soudure — ligne 3',
        description: 'Pilote: équipe production. Objectif: -30% NC en 90j.',
        status: 'DO',
        ownerId: 'demo-user',
        createdAt: now, updatedAt: now,
        steps: [
          { id: 's1', cycleId: 'demo-1', phase: 'PLAN', title: 'Analyse Pareto',
            status: 'DONE', createdAt: now, updatedAt: now },
          { id: 's2', cycleId: 'demo-1', phase: 'DO', title: 'Mise en place Poka-Yoke',
            status: 'IN_PROGRESS', createdAt: now, updatedAt: now }
        ]
      },
      {
        id: 'demo-2', tenantId: 'demo-tenant',
        title: 'Amélioration satisfaction patient — service ambulatoire',
        description: 'CHU. Objectif: NPS +10 points en 6 mois.',
        status: 'PLAN',
        ownerId: 'demo-user',
        createdAt: now, updatedAt: now,
        steps: []
      },
      {
        id: 'demo-3', tenantId: 'demo-tenant',
        title: 'Réduction MTTR incidents P1',
        description: 'SRE. Objectif: < 30 min médiane.',
        status: 'CHECK',
        ownerId: 'demo-user',
        createdAt: now, updatedAt: now,
        steps: []
      }
    ];
  }
}
