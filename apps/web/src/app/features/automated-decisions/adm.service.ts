import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  AdmCreateRequest,
  AdmEditRequest,
  AdmStatus,
  AdmView
} from './adm.types';

@Injectable({ providedIn: 'root' })
export class AdmService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/gdpr/automated-decisions`;
  private readonly mockStore: AdmView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: AdmStatus): Observable<AdmView[]> {
    if (environment.useMockApi) {
      const f = status ? this.mockStore.filter(r => r.status === status) : this.mockStore;
      return of([...f]).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<AdmView[]>(this.endpoint, { params });
  }

  get(id: string): Observable<AdmView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      return r ? of(structuredClone(r)).pipe(delay(80))
               : throwError(() => this.err(404, 'Décision automatisée introuvable.'));
    }
    return this.http.get<AdmView>(`${this.endpoint}/${id}`);
  }

  getByReference(reference: string): Observable<AdmView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.reference === reference);
      return r ? of(structuredClone(r)).pipe(delay(80))
               : throwError(() => this.err(404, 'Référence introuvable.'));
    }
    return this.http.get<AdmView>(`${this.endpoint}/by-reference`, {
      params: new HttpParams().set('reference', reference)
    });
  }

  create(req: AdmCreateRequest): Observable<AdmView> {
    if (environment.useMockApi) {
      if (this.mockStore.some(r => r.reference === req.reference)) {
        return throwError(() => this.err(409, 'Référence déjà utilisée.'));
      }
      this.checkArt22Invariants(req);
      const now = new Date().toISOString();
      const r: AdmView = {
        id: this.uuid(),
        tenantId: '00000000-0000-0000-0000-000000000001',
        reference: req.reference,
        name: req.name,
        description: req.description ?? null,
        decisionType: req.decisionType,
        art22LawfulBasis: req.art22LawfulBasis ?? null,
        lawfulBasisDetails: req.lawfulBasisDetails ?? null,
        inputDataCategories: req.inputDataCategories ?? [],
        linkedProcessingActivityIds: req.linkedProcessingActivityIds ?? [],
        linkedDpiaId: req.linkedDpiaId ?? null,
        algorithmDescription: req.algorithmDescription ?? null,
        significanceForSubject: req.significanceForSubject ?? null,
        humanReviewMechanism: req.humanReviewMechanism ?? null,
        objectionMechanism: req.objectionMechanism ?? null,
        status: 'DRAFT',
        effectiveFrom: null, effectiveTo: null,
        createdByUserId: req.createdByUserId,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(r);
      return of(structuredClone(r)).pipe(delay(140));
    }
    return this.http.post<AdmView>(this.endpoint, req);
  }

  edit(id: string, req: AdmEditRequest): Observable<AdmView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      if (!r) return throwError(() => this.err(404, 'Décision automatisée introuvable.'));
      if (r.status === 'ARCHIVED') return throwError(() => this.err(409, 'Archive — édition impossible.'));
      this.checkArt22Invariants(req);
      r.name = req.name;
      r.description = req.description ?? null;
      r.decisionType = req.decisionType;
      r.art22LawfulBasis = req.art22LawfulBasis ?? null;
      r.lawfulBasisDetails = req.lawfulBasisDetails ?? null;
      r.inputDataCategories = req.inputDataCategories ?? [];
      r.linkedProcessingActivityIds = req.linkedProcessingActivityIds ?? [];
      r.linkedDpiaId = req.linkedDpiaId ?? null;
      r.algorithmDescription = req.algorithmDescription ?? null;
      r.significanceForSubject = req.significanceForSubject ?? null;
      r.humanReviewMechanism = req.humanReviewMechanism ?? null;
      r.objectionMechanism = req.objectionMechanism ?? null;
      r.updatedAt = new Date().toISOString();
      return of(structuredClone(r)).pipe(delay(120));
    }
    return this.http.put<AdmView>(`${this.endpoint}/${id}`, req);
  }

  activate(id: string): Observable<AdmView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      if (!r) return throwError(() => this.err(404, 'Décision automatisée introuvable.'));
      if (r.status !== 'DRAFT') return throwError(() => this.err(409, 'Activation possible uniquement depuis DRAFT.'));
      this.checkArt22Invariants(r);
      const now = new Date().toISOString();
      r.status = 'ACTIVE';
      r.effectiveFrom = now;
      r.updatedAt = now;
      return of(structuredClone(r)).pipe(delay(120));
    }
    return this.http.post<AdmView>(`${this.endpoint}/${id}/activate`, {});
  }

  deprecate(id: string): Observable<AdmView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      if (!r) return throwError(() => this.err(404, 'Décision automatisée introuvable.'));
      if (r.status !== 'ACTIVE') return throwError(() => this.err(409, 'Dépréciation possible uniquement depuis ACTIVE.'));
      r.status = 'DEPRECATED';
      r.updatedAt = new Date().toISOString();
      return of(structuredClone(r)).pipe(delay(120));
    }
    return this.http.post<AdmView>(`${this.endpoint}/${id}/deprecate`, {});
  }

  archive(id: string): Observable<AdmView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      if (!r) return throwError(() => this.err(404, 'Décision automatisée introuvable.'));
      if (r.status !== 'DEPRECATED') return throwError(() => this.err(409, 'Archivage possible uniquement depuis DEPRECATED.'));
      const now = new Date().toISOString();
      r.status = 'ARCHIVED';
      r.effectiveTo = now;
      r.updatedAt = now;
      return of(structuredClone(r)).pipe(delay(120));
    }
    return this.http.post<AdmView>(`${this.endpoint}/${id}/archive`, {});
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      if (!r) return throwError(() => this.err(404, 'Décision automatisée introuvable.'));
      if (r.status !== 'DRAFT') return throwError(() => this.err(409, 'Suppression possible uniquement en DRAFT.'));
      const i = this.mockStore.indexOf(r);
      this.mockStore.splice(i, 1);
      return of(void 0).pipe(delay(100));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  private checkArt22Invariants(r: { decisionType: string;
                                     art22LawfulBasis?: string | null;
                                     humanReviewMechanism?: string | null }): void {
    if (r.decisionType === 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT') {
      if (!r.art22LawfulBasis) {
        throw this.err(422, 'Art. 22.2 — base légale obligatoire pour une décision automatisée à effet juridique.');
      }
      if (!r.humanReviewMechanism?.trim()) {
        throw this.err(422, 'Art. 22.3 — mécanisme de révision humaine obligatoire.');
      }
    }
  }

  private err(status: number, title: string) {
    return { status, error: { type: 'about:blank', title, status, detail: title } };
  }

  private uuid(): string {
    return 'adm-' + Math.random().toString(16).slice(2, 10) + '-' + Date.now().toString(16);
  }

  private seed(): AdmView[] {
    const now = Date.now();
    const day = 86400000;
    const iso = (n: number) => new Date(now - n * day).toISOString();
    return [
      {
        id: 'adm-seed-001', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'ADM-CREDIT-001',
        name: 'Scoring crédit consommateur',
        description: 'Décision automatisée d\'octroi de crédit conso jusqu\'à 5 000 €.',
        decisionType: 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT',
        art22LawfulBasis: 'CONTRACTUAL_NECESSITY',
        lawfulBasisDetails: 'Nécessaire à l\'instruction de la demande de crédit du client (Art. 22.2.a).',
        inputDataCategories: ['identité', 'revenus déclarés', 'historique bancaire 12 mois', 'fichier FICP'],
        linkedProcessingActivityIds: [], linkedDpiaId: null,
        algorithmDescription: 'Modèle gradient boosting (XGBoost) entraîné sur cohorte 5 ans. Score [0;1], seuil acceptation 0.65. Variables : 12 features financières + 4 features comportementales.',
        significanceForSubject: 'Décision détermine l\'octroi ou le refus d\'un prêt. Refus motivé renvoyé au client avec explication des critères principaux.',
        humanReviewMechanism: 'Le client peut demander une révision humaine par un conseiller dans les 15 jours. Réponse motivée garantie sous 30 jours.',
        objectionMechanism: 'Le client peut s\'opposer à la décision automatisée et demander un traitement entièrement humain (procédure plus longue, 5 jours ouvrés).',
        status: 'ACTIVE',
        effectiveFrom: iso(120), effectiveTo: null,
        createdByUserId: '00000000-0000-0000-0000-000000000999',
        createdAt: iso(150), updatedAt: iso(30)
      },
      {
        id: 'adm-seed-002', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'ADM-ABTEST-002',
        name: 'Profilage d\'audience pour campagnes marketing',
        description: 'Segmentation des visiteurs e-commerce pour personnalisation d\'affichage.',
        decisionType: 'PROFILING_ONLY',
        art22LawfulBasis: null,
        lawfulBasisDetails: 'Pas applicable — profilage sans décision automatisée à effet juridique. Base légale : consentement marketing (Art. 6.1.a).',
        inputDataCategories: ['historique de navigation', 'panier', 'localisation IP'],
        linkedProcessingActivityIds: [], linkedDpiaId: null,
        algorithmDescription: 'Clustering K-means en 8 segments. Affectation = segment le plus proche du vecteur comportemental.',
        significanceForSubject: 'Affichage personnalisé des promotions. Aucun impact juridique ou financier.',
        humanReviewMechanism: 'Non applicable (pas de décision juridique).',
        objectionMechanism: 'Le visiteur peut désactiver le profilage marketing à tout moment depuis le bandeau cookies et son espace personnel.',
        status: 'ACTIVE',
        effectiveFrom: iso(200), effectiveTo: null,
        createdByUserId: '00000000-0000-0000-0000-000000000999',
        createdAt: iso(210), updatedAt: iso(60)
      },
      {
        id: 'adm-seed-003', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'ADM-FRAUD-003',
        name: 'Détection fraude paiement temps réel',
        description: 'Brouillon — bloquer/laisser passer une transaction selon score de fraude.',
        decisionType: 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT',
        art22LawfulBasis: null,
        lawfulBasisDetails: null,
        inputDataCategories: ['carte', 'montant', 'merchant', 'device fingerprint', 'historique 30j'],
        linkedProcessingActivityIds: [], linkedDpiaId: null,
        algorithmDescription: 'Réseau de neurones LSTM. À documenter.',
        significanceForSubject: null,
        humanReviewMechanism: null,
        objectionMechanism: null,
        status: 'DRAFT',
        effectiveFrom: null, effectiveTo: null,
        createdByUserId: '00000000-0000-0000-0000-000000000999',
        createdAt: iso(3), updatedAt: iso(3)
      }
    ];
  }
}
