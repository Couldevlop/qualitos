import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  FriaApproveRequest,
  FriaArchiveRequest,
  FriaDraftRequest,
  FriaEditRequest,
  FriaReturnRequest,
  FriaStatus,
  FriaSubmitRequest,
  FriaView
} from './fria.types';

@Injectable({ providedIn: 'root' })
export class FriaService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ai-act/fria`;
  private readonly mockStore: FriaView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: FriaStatus): Observable<FriaView[]> {
    if (environment.useMockApi) {
      const f = status ? this.mockStore.filter(x => x.status === status) : this.mockStore;
      return of([...f]).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<FriaView[]>(this.endpoint, { params });
  }

  listByAiSystem(aiSystemId: string): Observable<FriaView[]> {
    if (environment.useMockApi) {
      return of(this.mockStore.filter(f => f.aiSystemId === aiSystemId)).pipe(delay(120));
    }
    return this.http.get<FriaView[]>(`${this.endpoint}/by-system`, {
      params: new HttpParams().set('aiSystemId', aiSystemId)
    });
  }

  get(id: string): Observable<FriaView> {
    if (environment.useMockApi) {
      const f = this.mockStore.find(x => x.id === id);
      return f ? of(structuredClone(f)).pipe(delay(80))
               : throwError(() => this.err(404, 'FRIA introuvable.'));
    }
    return this.http.get<FriaView>(`${this.endpoint}/${id}`);
  }

  getByReference(reference: string): Observable<FriaView> {
    if (environment.useMockApi) {
      const f = this.mockStore.find(x => x.reference === reference);
      return f ? of(structuredClone(f)).pipe(delay(80))
               : throwError(() => this.err(404, 'Référence introuvable.'));
    }
    return this.http.get<FriaView>(`${this.endpoint}/by-reference`, {
      params: new HttpParams().set('reference', reference)
    });
  }

  draft(req: FriaDraftRequest): Observable<FriaView> {
    if (environment.useMockApi) {
      if (this.mockStore.some(f => f.reference === req.reference)) {
        return throwError(() => this.err(409, 'Référence déjà utilisée.'));
      }
      const now = new Date().toISOString();
      const f: FriaView = {
        id: this.uuid(),
        tenantId: '00000000-0000-0000-0000-000000000001',
        reference: req.reference, aiSystemId: req.aiSystemId,
        processDescription: req.processDescription,
        deploymentDurationDescription: req.deploymentDurationDescription ?? null,
        affectedPersonsCategories: req.affectedPersonsCategories,
        specificRisks: req.specificRisks,
        mitigationMeasures: req.mitigationMeasures ?? null,
        humanOversightMeasures: req.humanOversightMeasures ?? null,
        complaintMechanismDescription: req.complaintMechanismDescription ?? null,
        status: 'DRAFT',
        submittedAt: null, submittedByUserId: null,
        approvedAt: null, approvedByUserId: null, approvalNotes: null,
        effectiveTo: null, archivedReason: null,
        createdByUserId: req.createdByUserId,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(f);
      return of(structuredClone(f)).pipe(delay(140));
    }
    return this.http.post<FriaView>(this.endpoint, req);
  }

  edit(id: string, req: FriaEditRequest): Observable<FriaView> {
    if (environment.useMockApi) {
      const f = this.mockStore.find(x => x.id === id);
      if (!f) return throwError(() => this.err(404, 'FRIA introuvable.'));
      if (f.status !== 'DRAFT') return throwError(() => this.err(409, 'Édition possible uniquement en DRAFT.'));
      f.processDescription = req.processDescription;
      f.deploymentDurationDescription = req.deploymentDurationDescription ?? null;
      f.affectedPersonsCategories = req.affectedPersonsCategories;
      f.specificRisks = req.specificRisks;
      f.mitigationMeasures = req.mitigationMeasures ?? null;
      f.humanOversightMeasures = req.humanOversightMeasures ?? null;
      f.complaintMechanismDescription = req.complaintMechanismDescription ?? null;
      f.updatedAt = new Date().toISOString();
      return of(structuredClone(f)).pipe(delay(120));
    }
    return this.http.put<FriaView>(`${this.endpoint}/${id}`, req);
  }

  submit(id: string, req: FriaSubmitRequest): Observable<FriaView> {
    if (environment.useMockApi) {
      const f = this.mockStore.find(x => x.id === id);
      if (!f) return throwError(() => this.err(404, 'FRIA introuvable.'));
      if (f.status !== 'DRAFT') return throwError(() => this.err(409, 'Soumission impossible — statut courant : ' + f.status + '.'));
      const now = new Date().toISOString();
      f.status = 'SUBMITTED';
      f.submittedAt = now; f.submittedByUserId = req.submittedByUserId;
      f.updatedAt = now;
      return of(structuredClone(f)).pipe(delay(120));
    }
    return this.http.post<FriaView>(`${this.endpoint}/${id}/submit`, req);
  }

  approve(id: string, req: FriaApproveRequest): Observable<FriaView> {
    if (environment.useMockApi) {
      const f = this.mockStore.find(x => x.id === id);
      if (!f) return throwError(() => this.err(404, 'FRIA introuvable.'));
      if (f.status !== 'SUBMITTED') return throwError(() => this.err(409, 'Approbation possible uniquement depuis SUBMITTED.'));
      const now = new Date().toISOString();
      f.status = 'APPROVED';
      f.approvedAt = now; f.approvedByUserId = req.approvedByUserId;
      f.approvalNotes = req.approvalNotes ?? null;
      f.updatedAt = now;
      return of(structuredClone(f)).pipe(delay(120));
    }
    return this.http.post<FriaView>(`${this.endpoint}/${id}/approve`, req);
  }

  returnToDraft(id: string, req: FriaReturnRequest): Observable<FriaView> {
    if (environment.useMockApi) {
      const f = this.mockStore.find(x => x.id === id);
      if (!f) return throwError(() => this.err(404, 'FRIA introuvable.'));
      if (f.status !== 'SUBMITTED') return throwError(() => this.err(409, 'Retour en brouillon possible uniquement depuis SUBMITTED.'));
      f.status = 'DRAFT';
      f.approvalNotes = req.reason;
      f.submittedAt = null; f.submittedByUserId = null;
      f.updatedAt = new Date().toISOString();
      return of(structuredClone(f)).pipe(delay(120));
    }
    return this.http.post<FriaView>(`${this.endpoint}/${id}/return`, req);
  }

  archive(id: string, req: FriaArchiveRequest): Observable<FriaView> {
    if (environment.useMockApi) {
      const f = this.mockStore.find(x => x.id === id);
      if (!f) return throwError(() => this.err(404, 'FRIA introuvable.'));
      if (f.status !== 'APPROVED') return throwError(() => this.err(409, 'Archivage possible uniquement depuis APPROVED.'));
      const now = new Date().toISOString();
      f.status = 'ARCHIVED';
      f.effectiveTo = now; f.archivedReason = req.reason;
      f.updatedAt = now;
      return of(structuredClone(f)).pipe(delay(120));
    }
    return this.http.post<FriaView>(`${this.endpoint}/${id}/archive`, req);
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(f => f.id === id);
      if (i < 0) return throwError(() => this.err(404, 'FRIA introuvable.'));
      this.mockStore.splice(i, 1);
      return of(void 0).pipe(delay(100));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  private err(status: number, title: string) {
    return { status, error: { type: 'about:blank', title, status, detail: title } };
  }

  private uuid(): string {
    return 'fria-' + Math.random().toString(16).slice(2, 10) + '-' + Date.now().toString(16);
  }

  private seed(): FriaView[] {
    const now = Date.now();
    const day = 86400000;
    const iso = (n: number) => new Date(now - n * day).toISOString();
    return [
      {
        id: 'fria-seed-001', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'FRIA-2026-DIAG-001',
        aiSystemId: '11111111-1111-1111-1111-111111111111',
        processDescription: 'Aide au diagnostic radiologique — détection lésions pulmonaires sur radiographies. Décision finale médicale conservée par le radiologue. Système IA fournit un score et localise la zone suspecte.',
        deploymentDurationDescription: 'Déploiement permanent prévu pour 3 ans, renouvelable.',
        affectedPersonsCategories: 'Patients hospitalisés et patients externes ayant une prescription de radiographie pulmonaire. Estimation : 12 000 patients/an.',
        specificRisks: 'Faux négatifs (lésion non détectée), faux positifs (anxiété, examens complémentaires inutiles), biais démographiques (sous-représentation de certaines populations dans le dataset d\'entraînement).',
        mitigationMeasures: 'Système jamais autonome : décision toujours validée par radiologue. Audit qualité trimestriel sur 500 cas. Plan de re-training annuel.',
        humanOversightMeasures: 'Radiologue lit l\'image et valide ou rejette la suggestion IA. Logging exhaustif des écarts entre suggestion et décision finale.',
        complaintMechanismDescription: 'Réclamations patients via service relations patients (CHU) + procédure RGPD Art. 22 si décision automatisée invoquée.',
        status: 'APPROVED',
        submittedAt: iso(45), submittedByUserId: '00000000-0000-0000-0000-000000000999',
        approvedAt: iso(30), approvedByUserId: '00000000-0000-0000-0000-000000000888',
        approvalNotes: 'Évaluation conforme aux exigences Art. 27. Recommandation : revue annuelle de la composition du dataset.',
        effectiveTo: null, archivedReason: null,
        createdByUserId: '00000000-0000-0000-0000-000000000999',
        createdAt: iso(60), updatedAt: iso(30)
      },
      {
        id: 'fria-seed-002', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'FRIA-2026-HRGD-002',
        aiSystemId: '22222222-2222-2222-2222-222222222222',
        processDescription: 'Tri automatisé de CV pour préqualification de candidatures (rôles techniques uniquement).',
        deploymentDurationDescription: 'Phase pilote de 6 mois.',
        affectedPersonsCategories: 'Candidats à des postes techniques (estimation 2 500 candidats sur la phase pilote).',
        specificRisks: 'Discrimination indirecte (genre, origine, âge). Opacité du scoring. Risque d\'écarter de bons profils atypiques.',
        mitigationMeasures: 'Tri IA est seulement consultatif : recruteur RH revoit 100 % des dossiers, IA fournit un classement et des arguments. Audit biais mensuel.',
        humanOversightMeasures: 'Décision finale toujours humaine. Documentation des écarts entre rang IA et décision RH.',
        complaintMechanismDescription: 'Procédure de réclamation candidat (Art. 22 RGPD) avec révision humaine garantie sous 5 jours.',
        status: 'SUBMITTED',
        submittedAt: iso(3), submittedByUserId: '00000000-0000-0000-0000-000000000999',
        approvedAt: null, approvedByUserId: null, approvalNotes: null,
        effectiveTo: null, archivedReason: null,
        createdByUserId: '00000000-0000-0000-0000-000000000999',
        createdAt: iso(20), updatedAt: iso(3)
      }
    ];
  }
}
