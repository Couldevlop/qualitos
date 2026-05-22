import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CreateProcessingActivityRequest,
  EditProcessingActivityRequest,
  ProcessingActivityStatus,
  ProcessingActivityView
} from './ropa.types';

/**
 * Single HTTP boundary for the ROPA feature.
 * Mock branch returns the same shape as the backend so pages never need
 * to know whether they speak to a real or in-memory backend.
 */
@Injectable({ providedIn: 'root' })
export class RopaService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/gdpr/processing-activities`;

  private readonly mockStore: ProcessingActivityView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: ProcessingActivityStatus): Observable<ProcessingActivityView[]> {
    if (environment.useMockApi) {
      const f = status ? this.mockStore.filter(a => a.status === status) : this.mockStore;
      return of(f).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<ProcessingActivityView[]>(this.endpoint, { params });
  }

  get(id: string): Observable<ProcessingActivityView> {
    if (environment.useMockApi) {
      return of(this.mockStore.find(a => a.id === id) ?? this.mockStore[0]).pipe(delay(100));
    }
    return this.http.get<ProcessingActivityView>(`${this.endpoint}/${id}`);
  }

  getByReference(reference: string): Observable<ProcessingActivityView> {
    if (environment.useMockApi) {
      const a = this.mockStore.find(x => x.reference === reference);
      return of(a ?? this.mockStore[0]).pipe(delay(100));
    }
    const params = new HttpParams().set('reference', reference);
    return this.http.get<ProcessingActivityView>(`${this.endpoint}/by-reference`, { params });
  }

  create(input: CreateProcessingActivityRequest): Observable<ProcessingActivityView> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const v: ProcessingActivityView = {
        id: 'ropa-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        reference: input.reference,
        name: input.name,
        purposes: input.purposes,
        lawfulBasis: input.lawfulBasis,
        lawfulBasisDetails: input.lawfulBasisDetails,
        controllerName: input.controllerName,
        controllerContact: input.controllerContact,
        dpoContact: input.dpoContact,
        jointControllerName: input.jointControllerName,
        jointControllerContact: input.jointControllerContact,
        dataSubjectCategories: input.dataSubjectCategories ?? [],
        dataCategories: input.dataCategories ?? [],
        specialCategoriesProcessed: input.specialCategoriesProcessed,
        specialCategoriesJustification: input.specialCategoriesJustification,
        recipientCategories: input.recipientCategories ?? [],
        thirdCountryTransfers: input.thirdCountryTransfers ?? [],
        transferSafeguards: input.transferSafeguards,
        linkedRetentionRuleIds: input.linkedRetentionRuleIds ?? [],
        technicalMeasures: input.technicalMeasures,
        organizationalMeasures: input.organizationalMeasures,
        status: 'DRAFT',
        createdByUserId: input.createdByUserId,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(v);
      return of(v).pipe(delay(150));
    }
    return this.http.post<ProcessingActivityView>(this.endpoint, input);
  }

  edit(id: string, input: EditProcessingActivityRequest): Observable<ProcessingActivityView> {
    if (environment.useMockApi) {
      const a = this.mockStore.find(x => x.id === id);
      if (a) {
        Object.assign(a, input, {
          dataSubjectCategories: input.dataSubjectCategories  ?? [],
          dataCategories:        input.dataCategories         ?? [],
          recipientCategories:   input.recipientCategories    ?? [],
          thirdCountryTransfers: input.thirdCountryTransfers  ?? [],
          linkedRetentionRuleIds: input.linkedRetentionRuleIds ?? []
        });
        a.updatedAt = new Date().toISOString();
        return of(a).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.put<ProcessingActivityView>(`${this.endpoint}/${id}`, input);
  }

  activate(id: string): Observable<ProcessingActivityView> { return this.transition(id, 'activate', 'ACTIVE'); }
  archive(id: string):  Observable<ProcessingActivityView> { return this.transition(id, 'archive',  'ARCHIVED'); }

  private transition(
    id: string, op: 'activate' | 'archive',
    target: ProcessingActivityStatus
  ): Observable<ProcessingActivityView> {
    if (environment.useMockApi) {
      const a = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (a) {
        a.status = target; a.updatedAt = now;
        if (target === 'ACTIVE')   a.effectiveFrom = now;
        if (target === 'ARCHIVED') a.effectiveTo   = now;
        return of(a).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<ProcessingActivityView>(`${this.endpoint}/${id}/${op}`, {});
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

  private seed(): ProcessingActivityView[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'ropa-1', tenantId: 'demo-tenant',
        reference: 'RH-PAYROLL-FR',
        name: 'Gestion de la paie — collaborateurs France',
        purposes: 'Établir et verser les rémunérations mensuelles, déclarer les cotisations sociales (URSSAF, retraite, prévoyance), produire les bulletins de paie.',
        lawfulBasis: 'CONTRACT',
        lawfulBasisDetails: 'Exécution du contrat de travail ; obligations légales en matière de paie et de sécurité sociale.',
        controllerName: 'QualitOS SAS', controllerContact: 'rh@qualitos.io',
        dpoContact: 'dpo@qualitos.io',
        dataSubjectCategories: ['Salariés', 'Stagiaires', 'Apprentis'],
        dataCategories: ['Identité', 'NIR (SSN)', 'Coordonnées bancaires', 'Données salariales', 'Données fiscales'],
        specialCategoriesProcessed: false,
        recipientCategories: ['Cabinet comptable externe', 'URSSAF', 'Caisses de retraite', 'Médecine du travail'],
        thirdCountryTransfers: [],
        linkedRetentionRuleIds: [],
        technicalMeasures: 'Chiffrement au repos AES-256-GCM, TLS 1.3 en transit, MFA obligatoire, audit logs immutables.',
        organizationalMeasures: 'Habilitations RBAC ABAC, séparation des rôles RH/IT, formation RGPD annuelle.',
        status: 'ACTIVE', effectiveFrom: now,
        createdByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'ropa-2', tenantId: 'demo-tenant',
        reference: 'CRM-PROSPECTS',
        name: 'Prospection commerciale B2B',
        purposes: 'Identifier des décideurs cibles, qualifier des leads, organiser des rendez-vous commerciaux.',
        lawfulBasis: 'LEGITIMATE_INTERESTS',
        lawfulBasisDetails: 'Intérêt légitime à développer l\'activité économique. Test LIA documenté : impact mineur sur la vie privée, opt-out présent dans chaque communication.',
        controllerName: 'QualitOS SAS', controllerContact: 'sales@qualitos.io',
        dpoContact: 'dpo@qualitos.io',
        dataSubjectCategories: ['Décideurs B2B', 'Contacts professionnels'],
        dataCategories: ['Identité professionnelle', 'Coordonnées professionnelles', 'Historique des interactions'],
        specialCategoriesProcessed: false,
        recipientCategories: ['Équipe commerciale interne', 'Outil CRM SaaS UE'],
        thirdCountryTransfers: [],
        linkedRetentionRuleIds: [],
        technicalMeasures: 'TLS 1.3, chiffrement disque, sauvegardes chiffrées, segmentation réseau.',
        organizationalMeasures: 'Charte commerciale, opt-out 1-clic dans tout e-mail, purge automatique 3 ans après dernier contact.',
        status: 'ACTIVE', effectiveFrom: now,
        createdByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'ropa-3', tenantId: 'demo-tenant',
        reference: 'CCTV-SIEGE',
        name: 'Vidéosurveillance — siège social',
        purposes: 'Sécurité des biens et des personnes, prévention des intrusions sur le site du siège social.',
        lawfulBasis: 'LEGITIMATE_INTERESTS',
        controllerName: 'QualitOS SAS', controllerContact: 'security@qualitos.io',
        dpoContact: 'dpo@qualitos.io',
        dataSubjectCategories: ['Salariés', 'Visiteurs', 'Prestataires'],
        dataCategories: ['Images vidéo', 'Horodatages'],
        specialCategoriesProcessed: false,
        recipientCategories: ['Responsable sécurité', 'Autorités sur réquisition'],
        thirdCountryTransfers: [],
        linkedRetentionRuleIds: [],
        technicalMeasures: 'Enregistreur sécurisé, accès limité par badge, chiffrement disque.',
        organizationalMeasures: 'Affichage information conforme CNIL, registre des accès aux enregistrements, conservation 30 jours.',
        status: 'DRAFT',
        createdByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      }
    ];
  }
}
