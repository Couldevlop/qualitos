import { HttpClient, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  AuditPage, AuditResponse,
  CertificatePage, CertificateResponse,
  CreateAuditRequest, CreateCertificateRequest,
  CreateNonConformityRequest, CreateSupplierRequest,
  NonConformityFilters, NonConformityPage, NonConformityResponse,
  StatusChangeRequest,
  SupplierDocument, SupplierDocumentCatalogPage, SupplierDocumentCategory,
  SupplierDocumentFilters, SupplierDocumentOwnerType,
  SupplierPage, SupplierResponse,
  SupplierStatistics, SupplierStatus, SupplierType,
  UpdateNonConformityRequest, UpdateSupplierRequest
} from './suppliers.types';

@Injectable({ providedIn: 'root' })
export class SuppliersService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/suppliers`;

  private readonly mockStore: SupplierResponse[]              = this.seedSuppliers();
  private readonly mockAudits: Record<string, AuditResponse[]> = this.seedAudits();
  private readonly mockNcs:    Record<string, NonConformityResponse[]> = {};
  private readonly mockCerts:  Record<string, CertificateResponse[]>   = this.seedCerts();
  private readonly mockDocs: SupplierDocument[] = [];

  constructor(private readonly http: HttpClient) {}

  // ---------- Suppliers ----------

  list(page = 0, size = 50, status?: SupplierStatus, type?: SupplierType): Observable<SupplierPage> {
    if (environment.useMockApi) {
      const f = this.mockStore
        .filter(s => !status || s.status === status)
        .filter(s => !type   || s.supplierType === type);
      return of({
        content: f, totalElements: f.length, totalPages: 1, number: 0, size: f.length
      }).pipe(delay(120));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (type)   params = params.set('type',   type);
    return this.http.get<SupplierPage>(this.endpoint, { params });
  }

  get(id: string): Observable<SupplierResponse> {
    if (environment.useMockApi) {
      return of(this.mockStore.find(s => s.id === id) ?? this.mockStore[0]).pipe(delay(120));
    }
    return this.http.get<SupplierResponse>(`${this.endpoint}/${id}`);
  }

  create(input: CreateSupplierRequest): Observable<SupplierResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const s: SupplierResponse = {
        id: 'sup-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        code: input.code, name: input.name,
        countryCode: input.countryCode, contactEmail: input.contactEmail,
        supplierType: input.supplierType,
        status: 'PROSPECT', score: 0,
        createdBy: input.createdBy,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(s);
      this.mockAudits[s.id] = []; this.mockNcs[s.id] = []; this.mockCerts[s.id] = [];
      return of(s).pipe(delay(150));
    }
    return this.http.post<SupplierResponse>(this.endpoint, input);
  }

  update(id: string, input: UpdateSupplierRequest): Observable<SupplierResponse> {
    if (environment.useMockApi) {
      const s = this.mockStore.find(x => x.id === id);
      if (s) { Object.assign(s, input); s.updatedAt = new Date().toISOString(); return of(s).pipe(delay(120)); }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<SupplierResponse>(`${this.endpoint}/${id}`, input);
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(s => s.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  changeStatus(id: string, target: SupplierStatus, body: StatusChangeRequest): Observable<SupplierResponse> {
    if (environment.useMockApi) {
      const s = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (s) {
        s.status = target; s.updatedAt = now;
        if (target === 'APPROVED') { s.approvedAt = now; s.approvedBy = body.actorUserId; }
        return of(s).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<SupplierResponse>(`${this.endpoint}/${id}/status/${target}`, body);
  }

  statistics(id: string): Observable<SupplierStatistics> {
    if (environment.useMockApi) {
      const ncs = this.mockNcs[id] ?? [];
      const certs = this.mockCerts[id] ?? [];
      const s = this.mockStore.find(x => x.id === id);
      return of({
        supplierId: id,
        score: s?.score ?? 0,
        status: s?.status ?? 'PROSPECT',
        openNonConformities:           ncs.filter(n => n.status === 'OPEN' || n.status === 'IN_REVIEW').length,
        resolvedNonConformitiesRecent: ncs.filter(n => n.status === 'RESOLVED').length,
        expiredCertificates:           certs.filter(c => c.expired).length,
        lastAuditAt: s?.lastAuditAt
      }).pipe(delay(120));
    }
    return this.http.get<SupplierStatistics>(`${this.endpoint}/${id}/statistics`);
  }

  // ---------- Audits ----------

  listAudits(supplierId: string, page = 0, size = 50): Observable<AuditPage> {
    if (environment.useMockApi) {
      const a = this.mockAudits[supplierId] ?? [];
      return of({ content: a, totalElements: a.length, totalPages: 1, number: 0, size: a.length }).pipe(delay(100));
    }
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<AuditPage>(`${this.endpoint}/${supplierId}/audits`, { params });
  }

  addAudit(supplierId: string, input: CreateAuditRequest): Observable<AuditResponse> {
    if (environment.useMockApi) {
      const a: AuditResponse = {
        id: 'sa-' + Math.random().toString(36).slice(2, 9),
        tenantId: 'demo-tenant',
        supplierId,
        auditedOn: input.auditedOn,
        score: input.score,
        auditorUserId: input.auditorUserId,
        findingsSummary: input.findingsSummary,
        criticalFindingsCount: input.criticalFindingsCount ?? 0,
        majorFindingsCount:    input.majorFindingsCount    ?? 0,
        minorFindingsCount:    input.minorFindingsCount    ?? 0,
        createdAt: new Date().toISOString()
      };
      const arr = this.mockAudits[supplierId] ?? [];
      arr.unshift(a); this.mockAudits[supplierId] = arr;
      const s = this.mockStore.find(x => x.id === supplierId);
      if (s) {
        s.lastAuditAt = input.auditedOn;
        // Simple recomputed score: weighted moving average towards the latest audit.
        s.score = Math.round((s.score * 0.4) + (input.score * 0.6));
        s.updatedAt = new Date().toISOString();
      }
      return of(a).pipe(delay(120));
    }
    return this.http.post<AuditResponse>(`${this.endpoint}/${supplierId}/audits`, input);
  }

  // ---------- Non-conformities ----------

  listNcs(supplierId: string, page = 0, size = 50): Observable<NonConformityPage> {
    if (environment.useMockApi) {
      const n = this.mockNcs[supplierId] ?? [];
      return of({ content: n, totalElements: n.length, totalPages: 1, number: 0, size: n.length }).pipe(delay(100));
    }
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<NonConformityPage>(`${this.endpoint}/${supplierId}/non-conformities`, { params });
  }

  addNc(supplierId: string, input: CreateNonConformityRequest): Observable<NonConformityResponse> {
    if (environment.useMockApi) {
      const nc: NonConformityResponse = {
        id: 'snc-' + Math.random().toString(36).slice(2, 9),
        tenantId: 'demo-tenant',
        supplierId,
        lotReference: input.lotReference,
        description: input.description,
        severity: input.severity,
        status: 'OPEN',
        detectedOn: input.detectedOn,
        partNumber: input.partNumber,
        partQuantity: input.partQuantity,
        eightDRequested: !!input.eightDRequested,
        eightDReceived: !!input.eightDReceived,
        documentCount: 0,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
      const arr = this.mockNcs[supplierId] ?? [];
      arr.unshift(nc); this.mockNcs[supplierId] = arr;
      return of(nc).pipe(delay(120));
    }
    return this.http.post<NonConformityResponse>(`${this.endpoint}/${supplierId}/non-conformities`, input);
  }

  updateNc(
    supplierId: string, ncId: string, input: UpdateNonConformityRequest
  ): Observable<NonConformityResponse> {
    if (environment.useMockApi) {
      const arr = this.mockNcs[supplierId] ?? [];
      const nc = arr.find(n => n.id === ncId);
      if (nc) {
        Object.assign(nc, input);
        nc.updatedAt = new Date().toISOString();
        return of(nc).pipe(delay(120));
      }
      return of(arr[0]).pipe(delay(120));
    }
    return this.http.patch<NonConformityResponse>(
      `${this.endpoint}/${supplierId}/non-conformities/${ncId}`, input
    );
  }

  /** Les NC de tous les fournisseurs, les plus récentes d'abord. */
  listAllNcs(filters: NonConformityFilters = {}, page = 0, size = 50): Observable<NonConformityPage> {
    if (environment.useMockApi) {
      const all = Object.values(this.mockNcs).flat()
        .filter(n => !filters.supplierId || n.supplierId === filters.supplierId)
        .filter(n => !filters.status || n.status === filters.status)
        .filter(n => !filters.severity || n.severity === filters.severity)
        .map(n => {
          const s = this.mockStore.find(x => x.id === n.supplierId);
          return { ...n, supplierCode: s?.code, supplierName: s?.name };
        });
      return of({ content: all, totalElements: all.length, totalPages: 1, number: 0, size: all.length }).pipe(delay(100));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters.supplierId) params = params.set('supplierId', filters.supplierId);
    if (filters.status)     params = params.set('status', filters.status);
    if (filters.severity)   params = params.set('severity', filters.severity);
    return this.http.get<NonConformityPage>(`${this.endpoint}/non-conformities`, { params });
  }

  /** La fiche PDF d'une NC, avec son nom de fichier dans l'en-tête. */
  ncReport(supplierId: string, ncId: string): Observable<HttpResponse<Blob>> {
    if (environment.useMockApi) {
      return of(new HttpResponse({
        body: new Blob(['%PDF-1.4\n%demo\n'], { type: 'application/pdf' }), status: 200,
        headers: new HttpHeaders({ 'Content-Disposition': `attachment; filename="nc-fournisseur-${ncId}.pdf"` })
      })).pipe(delay(100));
    }
    return this.http.get(`${this.endpoint}/${supplierId}/non-conformities/${ncId}/report`,
      { responseType: 'blob', observe: 'response' });
  }

  // ---------- Documents (NC et certificats) ----------

  listDocuments(supplierId: string, ownerType: SupplierDocumentOwnerType, ownerId: string): Observable<SupplierDocument[]> {
    if (environment.useMockApi) {
      return of(this.mockDocs.filter(d => d.ownerType === ownerType && d.ownerId === ownerId)).pipe(delay(80));
    }
    return this.http.get<SupplierDocument[]>(this.documentsUrl(supplierId, ownerType, ownerId));
  }

  uploadDocument(
    supplierId: string, ownerType: SupplierDocumentOwnerType, ownerId: string,
    file: File, category: SupplierDocumentCategory
  ): Observable<SupplierDocument> {
    if (environment.useMockApi) {
      const d: SupplierDocument = {
        id: 'sd-' + Math.random().toString(36).slice(2, 9), supplierId, ownerType, ownerId, category,
        contentType: file.type, sizeBytes: file.size, originalFilename: file.name,
        createdAt: new Date().toISOString(), url: URL.createObjectURL(file)
      };
      this.mockDocs.push(d);
      return of(d).pipe(delay(120));
    }
    const form = new FormData();
    form.append('file', file, file.name);
    form.append('category', category);
    return this.http.post<SupplierDocument>(this.documentsUrl(supplierId, ownerType, ownerId), form);
  }

  deleteDocument(
    supplierId: string, ownerType: SupplierDocumentOwnerType, ownerId: string, documentId: string
  ): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockDocs.findIndex(d => d.id === documentId);
      if (i >= 0) this.mockDocs.splice(i, 1);
      return of(undefined).pipe(delay(80));
    }
    return this.http.delete<void>(`${this.documentsUrl(supplierId, ownerType, ownerId)}/${documentId}`);
  }

  /** Tous les documents fournisseur — la vue « Documents fournisseurs » de la GED. */
  listAllDocuments(filters: SupplierDocumentFilters = {}, page = 0, size = 50): Observable<SupplierDocumentCatalogPage> {
    if (environment.useMockApi) {
      const q = filters.q?.trim().toLowerCase();
      const rows = this.mockDocs
        .filter(d => !filters.supplierId || d.supplierId === filters.supplierId)
        .filter(d => !filters.ownerType || d.ownerType === filters.ownerType)
        .filter(d => !filters.category || d.category === filters.category)
        .filter(d => !q || (d.originalFilename ?? '').toLowerCase().includes(q))
        .map(d => {
          const s = this.mockStore.find(x => x.id === d.supplierId);
          return { ...d, supplierCode: s?.code, supplierName: s?.name };
        });
      return of({ content: rows, totalElements: rows.length, totalPages: 1, number: 0, size: rows.length }).pipe(delay(100));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters.supplierId) params = params.set('supplierId', filters.supplierId);
    if (filters.ownerType)  params = params.set('ownerType', filters.ownerType);
    if (filters.category)   params = params.set('category', filters.category);
    if (filters.q?.trim())  params = params.set('q', filters.q.trim());
    return this.http.get<SupplierDocumentCatalogPage>(`${this.endpoint}/documents`, { params });
  }

  private documentsUrl(supplierId: string, ownerType: SupplierDocumentOwnerType, ownerId: string): string {
    const segment = ownerType === 'CERTIFICATE' ? 'certificates' : 'non-conformities';
    return `${this.endpoint}/${supplierId}/${segment}/${ownerId}/documents`;
  }

  // ---------- Certificates ----------

  listCerts(supplierId: string, page = 0, size = 50): Observable<CertificatePage> {
    if (environment.useMockApi) {
      const c = this.mockCerts[supplierId] ?? [];
      return of({ content: c, totalElements: c.length, totalPages: 1, number: 0, size: c.length }).pipe(delay(100));
    }
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<CertificatePage>(`${this.endpoint}/${supplierId}/certificates`, { params });
  }

  addCert(supplierId: string, input: CreateCertificateRequest): Observable<CertificateResponse> {
    if (environment.useMockApi) {
      const expires = new Date(input.expiresOn);
      const c: CertificateResponse = {
        id: 'sc-' + Math.random().toString(36).slice(2, 9),
        tenantId: 'demo-tenant',
        supplierId,
        standardCode: input.standardCode,
        reference: input.reference,
        issuedOn: input.issuedOn,
        expiresOn: input.expiresOn,
        documentUrl: input.documentUrl,
        expired: expires.getTime() < Date.now(),
        documentCount: 0,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
      const arr = this.mockCerts[supplierId] ?? [];
      arr.unshift(c); this.mockCerts[supplierId] = arr;
      return of(c).pipe(delay(120));
    }
    return this.http.post<CertificateResponse>(`${this.endpoint}/${supplierId}/certificates`, input);
  }

  deleteCert(supplierId: string, certId: string): Observable<void> {
    if (environment.useMockApi) {
      const arr = this.mockCerts[supplierId] ?? [];
      const i = arr.findIndex(c => c.id === certId);
      if (i >= 0) arr.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${supplierId}/certificates/${certId}`);
  }

  // ---------- Seeds ----------

  private seedSuppliers(): SupplierResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'sup-1', tenantId: 'demo-tenant',
        code: 'STEEL-FR-001', name: 'AcierFrance SA',
        countryCode: 'FR', contactEmail: 'qualite@acierfrance.fr',
        supplierType: 'RAW_MATERIAL', status: 'APPROVED', score: 92,
        lastAuditAt: '2026-03-15',
        approvedAt: now, approvedBy: 'demo-user',
        createdBy: 'demo-user', createdAt: now, updatedAt: now
      },
      {
        id: 'sup-2', tenantId: 'demo-tenant',
        code: 'EMS-DE-014', name: 'BavariaElectronics GmbH',
        countryCode: 'DE', contactEmail: 'quality@bavaria-em.de',
        supplierType: 'COMPONENT', status: 'CONDITIONAL', score: 68,
        lastAuditAt: '2026-02-08',
        approvedAt: now, approvedBy: 'demo-user',
        createdBy: 'demo-user', createdAt: now, updatedAt: now
      },
      {
        id: 'sup-3', tenantId: 'demo-tenant',
        code: 'CLOUD-IE-002', name: 'CloudHostIE Ltd',
        countryCode: 'IE', contactEmail: 'qsr@cloudhost.ie',
        supplierType: 'SOFTWARE', status: 'PROSPECT', score: 0,
        createdBy: 'demo-user', createdAt: now, updatedAt: now
      }
    ];
  }

  private seedAudits(): Record<string, AuditResponse[]> {
    const now = new Date().toISOString();
    return {
      'sup-1': [{
        id: 'sa-1', tenantId: 'demo-tenant', supplierId: 'sup-1',
        auditedOn: '2026-03-15', score: 92, auditorUserId: 'demo-user',
        findingsSummary: 'Aucune NC majeure, 2 observations sur traçabilité lot.',
        criticalFindingsCount: 0, majorFindingsCount: 0, minorFindingsCount: 2,
        createdAt: now
      }],
      'sup-2': [{
        id: 'sa-2', tenantId: 'demo-tenant', supplierId: 'sup-2',
        auditedOn: '2026-02-08', score: 68, auditorUserId: 'demo-user',
        findingsSummary: '1 NC majeure traçabilité firmware, 3 mineures.',
        criticalFindingsCount: 0, majorFindingsCount: 1, minorFindingsCount: 3,
        createdAt: now
      }]
    };
  }

  private seedCerts(): Record<string, CertificateResponse[]> {
    const now = new Date().toISOString();
    return {
      'sup-1': [{
        id: 'sc-1', tenantId: 'demo-tenant', supplierId: 'sup-1',
        standardCode: 'iso-9001', reference: 'AFAQ-2024-9001-FR-001',
        issuedOn: '2024-06-12', expiresOn: '2027-06-11',
        expired: false, documentCount: 0, createdAt: now, updatedAt: now
      }],
      'sup-2': [{
        id: 'sc-2', tenantId: 'demo-tenant', supplierId: 'sup-2',
        standardCode: 'iatf-16949', reference: 'TUV-2023-IATF-0042',
        issuedOn: '2023-05-04', expiresOn: '2026-05-03',
        expired: true, documentCount: 0, createdAt: now, updatedAt: now
      }]
    };
  }
}
