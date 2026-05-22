import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  ActivateDpoRequest,
  CancelDpoRequest,
  DpoAppointmentStatus,
  DpoAppointmentView,
  EditDpoRequest,
  EndDpoRequest,
  ProposeDpoRequest
} from './dpo-appointments.types';

@Injectable({ providedIn: 'root' })
export class DpoAppointmentsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/gdpr/dpo-appointments`;
  private readonly mockStore: DpoAppointmentView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: DpoAppointmentStatus): Observable<DpoAppointmentView[]> {
    if (environment.useMockApi) {
      const f = status ? this.mockStore.filter(a => a.status === status) : this.mockStore;
      return of(f).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<DpoAppointmentView[]>(this.endpoint, { params });
  }

  get(id: string): Observable<DpoAppointmentView> {
    if (environment.useMockApi) {
      return of(this.mockStore.find(a => a.id === id) ?? this.mockStore[0]).pipe(delay(100));
    }
    return this.http.get<DpoAppointmentView>(`${this.endpoint}/${id}`);
  }

  getByReference(reference: string): Observable<DpoAppointmentView> {
    if (environment.useMockApi) {
      const a = this.mockStore.find(x => x.reference === reference);
      return of(a ?? this.mockStore[0]).pipe(delay(100));
    }
    const params = new HttpParams().set('reference', reference);
    return this.http.get<DpoAppointmentView>(`${this.endpoint}/by-reference`, { params });
  }

  findActiveByScope(scope: string): Observable<DpoAppointmentView | null> {
    if (environment.useMockApi) {
      const a = this.mockStore.find(x => x.scope === scope && x.status === 'ACTIVE');
      return of(a ?? null).pipe(delay(100));
    }
    const params = new HttpParams().set('scope', scope);
    return this.http.get<DpoAppointmentView | null>(`${this.endpoint}/active`, { params });
  }

  propose(input: ProposeDpoRequest): Observable<DpoAppointmentView> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const a: DpoAppointmentView = {
        id: 'dpo-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        reference: input.reference,
        dpoFullName: input.dpoFullName, dpoEmail: input.dpoEmail, dpoPhone: input.dpoPhone,
        dpoType: input.dpoType,
        externalCompanyName: input.externalCompanyName,
        qualifications: input.qualifications,
        scope: input.scope,
        linkedProcessingActivityIds: input.linkedProcessingActivityIds ?? [],
        status: 'PROPOSED',
        createdByUserId: input.createdByUserId,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(a);
      return of(a).pipe(delay(150));
    }
    return this.http.post<DpoAppointmentView>(this.endpoint, input);
  }

  edit(id: string, input: EditDpoRequest): Observable<DpoAppointmentView> {
    if (environment.useMockApi) {
      const a = this.mockStore.find(x => x.id === id);
      if (a) {
        Object.assign(a, input, {
          linkedProcessingActivityIds: input.linkedProcessingActivityIds ?? []
        });
        a.updatedAt = new Date().toISOString();
        return of(a).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.put<DpoAppointmentView>(`${this.endpoint}/${id}`, input);
  }

  activate(id: string, body: ActivateDpoRequest): Observable<DpoAppointmentView> {
    if (environment.useMockApi) {
      const a = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (a) {
        // OWASP A04 — mirror backend invariant : on activation, end any other
        // currently ACTIVE appointment with the same scope (one active DPO
        // per scope at a time).
        for (const other of this.mockStore) {
          if (other.id !== a.id && other.scope === a.scope && other.status === 'ACTIVE') {
            other.status = 'ENDED';
            other.effectiveTo = body.effectiveFrom;
            other.endReason = 'Remplacé par ' + a.reference;
            other.updatedAt = now;
          }
        }
        a.status = 'ACTIVE';
        a.effectiveFrom = body.effectiveFrom;
        a.regulatorNotifiedAt = body.regulatorNotifiedAt;
        a.regulatorNotificationReference = body.regulatorNotificationReference;
        a.updatedAt = now;
        return of(a).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<DpoAppointmentView>(`${this.endpoint}/${id}/activate`, body);
  }

  end(id: string, body: EndDpoRequest): Observable<DpoAppointmentView> {
    if (environment.useMockApi) {
      const a = this.mockStore.find(x => x.id === id);
      if (a) {
        a.status = 'ENDED';
        a.effectiveTo = body.effectiveTo;
        a.endReason = body.reason;
        a.updatedAt = new Date().toISOString();
        return of(a).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<DpoAppointmentView>(`${this.endpoint}/${id}/end`, body);
  }

  cancel(id: string, body: CancelDpoRequest): Observable<DpoAppointmentView> {
    if (environment.useMockApi) {
      const a = this.mockStore.find(x => x.id === id);
      if (a) {
        a.status = 'CANCELLED';
        a.endReason = body.reason;
        a.updatedAt = new Date().toISOString();
        return of(a).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<DpoAppointmentView>(`${this.endpoint}/${id}/cancel`, body);
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(a => a.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  // ---- Mock seed ----

  private seed(): DpoAppointmentView[] {
    const now = new Date().toISOString();
    const past = (d: number) => new Date(Date.now() - d * 86400000).toISOString();
    return [
      {
        id: 'dpo-1', tenantId: 'demo-tenant',
        reference: 'DPO-GROUPE-2026',
        dpoFullName: 'Marie Dubois',
        dpoEmail: 'dpo@qualitos.io',
        dpoPhone: '+33 1 23 45 67 89',
        dpoType: 'INTERNAL',
        qualifications: 'Certification AFCDP. Master 2 droit numérique (Paris II Panthéon-Assas). 8 ans d\'expérience.',
        scope: 'GROUPE',
        effectiveFrom: past(180),
        regulatorNotifiedAt: past(190),
        regulatorNotificationReference: 'CNIL-DPO-2025-DESIG-0042',
        linkedProcessingActivityIds: [],
        status: 'ACTIVE',
        createdByUserId: 'demo-user',
        createdAt: past(195), updatedAt: past(180)
      },
      {
        id: 'dpo-2', tenantId: 'demo-tenant',
        reference: 'DPO-FILIALE-HOPITAL',
        dpoFullName: 'Jean-Pierre Laurent',
        dpoEmail: 'dpo-hopital@qualitos.io',
        dpoType: 'EXTERNAL',
        externalCompanyName: 'PrivacyMD Consulting',
        qualifications: 'Médecin spécialiste avec formation RGPD santé. Référencé Joint Commission.',
        scope: 'HOPITAL_SUD',
        linkedProcessingActivityIds: [],
        status: 'PROPOSED',
        createdByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'dpo-3', tenantId: 'demo-tenant',
        reference: 'DPO-GROUPE-2024',
        dpoFullName: 'François Dupont',
        dpoEmail: 'francois.dupont@previous.example',
        dpoType: 'INTERNAL',
        scope: 'GROUPE',
        effectiveFrom: past(900), effectiveTo: past(180),
        regulatorNotifiedAt: past(910),
        regulatorNotificationReference: 'CNIL-DPO-2023-DESIG-0014',
        linkedProcessingActivityIds: [],
        status: 'ENDED',
        endReason: 'Départ retraite. Remplacé par DPO-GROUPE-2026.',
        createdByUserId: 'demo-user',
        createdAt: past(920), updatedAt: past(180)
      }
    ];
  }
}
