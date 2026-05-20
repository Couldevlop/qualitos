import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  AcknowledgeRequest,
  AcknowledgmentResponse,
  ApprovalRequest,
  CreateDocumentRequest,
  CreateVersionRequest,
  DocumentPage,
  DocumentResponse,
  DocumentStatus,
  DocumentVersionResponse,
  UpdateDocumentRequest,
  UpdateVersionRequest
} from './documents.types';

@Injectable({ providedIn: 'root' })
export class DocumentsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/documents`;

  private readonly mockStore: DocumentResponse[] = this.seedMockDocuments();

  constructor(private readonly http: HttpClient) {}

  // ---------- Documents ----------

  list(page = 0, size = 20, status?: DocumentStatus): Observable<DocumentPage> {
    if (environment.useMockApi) {
      const filtered = status ? this.mockStore.filter(d => d.status === status) : this.mockStore;
      return of({
        content: filtered, totalElements: filtered.length, totalPages: 1,
        number: 0, size: filtered.length
      }).pipe(delay(120));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<DocumentPage>(this.endpoint, { params });
  }

  get(id: string): Observable<DocumentResponse> {
    if (environment.useMockApi) {
      const d = this.mockStore.find(x => x.id === id);
      return of(d ?? this.mockStore[0]).pipe(delay(120));
    }
    return this.http.get<DocumentResponse>(`${this.endpoint}/${id}`);
  }

  create(input: CreateDocumentRequest): Observable<DocumentResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const docId = 'doc-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7);
      const versionId = 'ver-' + Math.random().toString(36).slice(2, 9);
      const initialVersion: DocumentVersionResponse = {
        id: versionId,
        documentId: docId,
        versionNumber: 1,
        content: input.initialContent,
        contentUri: input.initialContentUri,
        changeNote: input.initialChangeNote ?? 'Création initiale',
        status: 'DRAFT',
        authorId: input.ownerId,
        createdAt: now,
        updatedAt: now
      };
      const d: DocumentResponse = {
        id: docId, tenantId: 'demo-tenant',
        code: input.code, title: input.title, description: input.description,
        type: input.type, status: 'ACTIVE',
        ownerId: input.ownerId, currentVersionId: versionId,
        mandatoryRead: input.mandatoryRead,
        createdAt: now, updatedAt: now,
        versions: [initialVersion]
      };
      this.mockStore.unshift(d);
      return of(d).pipe(delay(150));
    }
    return this.http.post<DocumentResponse>(this.endpoint, input);
  }

  update(id: string, input: UpdateDocumentRequest): Observable<DocumentResponse> {
    if (environment.useMockApi) {
      const d = this.mockStore.find(x => x.id === id);
      if (d) {
        Object.assign(d, input);
        d.updatedAt = new Date().toISOString();
        return of(d).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<DocumentResponse>(`${this.endpoint}/${id}`, input);
  }

  archive(id: string): Observable<DocumentResponse> {
    if (environment.useMockApi) {
      const d = this.mockStore.find(x => x.id === id);
      if (d) {
        d.status = 'ARCHIVED';
        d.updatedAt = new Date().toISOString();
        return of(d).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<DocumentResponse>(`${this.endpoint}/${id}/archive`, {});
  }

  // ---------- Versions ----------

  createVersion(documentId: string, input: CreateVersionRequest): Observable<DocumentVersionResponse> {
    if (environment.useMockApi) {
      const d = this.mockStore.find(x => x.id === documentId);
      const now = new Date().toISOString();
      const v: DocumentVersionResponse = {
        id: 'ver-' + Math.random().toString(36).slice(2, 9),
        documentId,
        versionNumber: (d?.versions.length ?? 0) + 1,
        content: input.content,
        contentUri: input.contentUri,
        changeNote: input.changeNote,
        status: 'DRAFT',
        authorId: input.authorId,
        createdAt: now, updatedAt: now
      };
      if (d) {
        d.versions = [...d.versions, v];
        d.updatedAt = now;
      }
      return of(v).pipe(delay(120));
    }
    return this.http.post<DocumentVersionResponse>(`${this.endpoint}/${documentId}/versions`, input);
  }

  updateVersion(documentId: string, versionId: string, input: UpdateVersionRequest): Observable<DocumentVersionResponse> {
    if (environment.useMockApi) {
      const v = this.findVersion(documentId, versionId);
      if (v) {
        if (input.content    !== undefined) v.content    = input.content;
        if (input.contentUri !== undefined) v.contentUri = input.contentUri;
        if (input.changeNote !== undefined) v.changeNote = input.changeNote;
        v.updatedAt = new Date().toISOString();
        return of(v).pipe(delay(120));
      }
      return of(this.mockStore[0].versions[0]).pipe(delay(120));
    }
    return this.http.patch<DocumentVersionResponse>(`${this.endpoint}/${documentId}/versions/${versionId}`, input);
  }

  submit(documentId: string, versionId: string): Observable<DocumentVersionResponse> {
    return this.transitionVersion(documentId, versionId, 'submit', 'IN_REVIEW');
  }

  approve(documentId: string, versionId: string, body: ApprovalRequest): Observable<DocumentVersionResponse> {
    if (environment.useMockApi) {
      const v = this.findVersion(documentId, versionId);
      if (v) {
        v.status = 'APPROVED';
        v.approvedBy = body.approverId;
        v.approvedAt = new Date().toISOString();
        v.updatedAt = v.approvedAt;
        return of(v).pipe(delay(120));
      }
      return of(this.mockStore[0].versions[0]).pipe(delay(120));
    }
    return this.http.patch<DocumentVersionResponse>(
      `${this.endpoint}/${documentId}/versions/${versionId}/approve`, body
    );
  }

  publish(documentId: string, versionId: string): Observable<DocumentVersionResponse> {
    if (environment.useMockApi) {
      const d = this.mockStore.find(x => x.id === documentId);
      const v = this.findVersion(documentId, versionId);
      const now = new Date().toISOString();
      if (v) {
        v.status = 'PUBLISHED';
        v.publishedAt = now;
        v.updatedAt = now;
        if (d) {
          d.currentVersionId = v.id;
          d.versions.filter(x => x.id !== v.id && x.status === 'PUBLISHED')
            .forEach(o => { o.status = 'OBSOLETE'; o.updatedAt = now; });
        }
        return of(v).pipe(delay(120));
      }
      return of(this.mockStore[0].versions[0]).pipe(delay(120));
    }
    return this.http.patch<DocumentVersionResponse>(
      `${this.endpoint}/${documentId}/versions/${versionId}/publish`, {}
    );
  }

  setBlockchainTx(documentId: string, versionId: string, txHash: string): Observable<DocumentVersionResponse> {
    if (environment.useMockApi) {
      const v = this.findVersion(documentId, versionId);
      if (v) {
        v.blockchainTxHash = txHash;
        v.updatedAt = new Date().toISOString();
        return of(v).pipe(delay(120));
      }
      return of(this.mockStore[0].versions[0]).pipe(delay(120));
    }
    return this.http.patch<DocumentVersionResponse>(
      `${this.endpoint}/${documentId}/versions/${versionId}/blockchain`, { txHash }
    );
  }

  acknowledge(documentId: string, versionId: string, body: AcknowledgeRequest): Observable<AcknowledgmentResponse> {
    if (environment.useMockApi) {
      const a: AcknowledgmentResponse = {
        id: 'ack-' + Math.random().toString(36).slice(2, 9),
        versionId, userId: body.userId,
        acknowledgedAt: new Date().toISOString()
      };
      return of(a).pipe(delay(120));
    }
    return this.http.post<AcknowledgmentResponse>(
      `${this.endpoint}/${documentId}/versions/${versionId}/acknowledge`, body
    );
  }

  countAcknowledgments(documentId: string, versionId: string): Observable<{ count: number }> {
    if (environment.useMockApi) {
      return of({ count: 0 }).pipe(delay(60));
    }
    return this.http.get<{ count: number }>(
      `${this.endpoint}/${documentId}/versions/${versionId}/acknowledgments/count`
    );
  }

  private transitionVersion(
    documentId: string, versionId: string, op: 'submit',
    target: 'IN_REVIEW'
  ): Observable<DocumentVersionResponse> {
    if (environment.useMockApi) {
      const v = this.findVersion(documentId, versionId);
      if (v) {
        v.status = target;
        v.updatedAt = new Date().toISOString();
        return of(v).pipe(delay(120));
      }
      return of(this.mockStore[0].versions[0]).pipe(delay(120));
    }
    return this.http.patch<DocumentVersionResponse>(
      `${this.endpoint}/${documentId}/versions/${versionId}/${op}`, {}
    );
  }

  private findVersion(documentId: string, versionId: string): DocumentVersionResponse | undefined {
    return this.mockStore.find(x => x.id === documentId)?.versions.find(v => v.id === versionId);
  }

  private seedMockDocuments(): DocumentResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'doc-1', tenantId: 'demo-tenant',
        code: 'POL-QUAL-001', title: 'Politique Qualité 2026',
        description: 'Politique générale, engagements direction, axes stratégiques.',
        type: 'POLICY', status: 'ACTIVE',
        ownerId: 'demo-user', mandatoryRead: true,
        currentVersionId: 'ver-1',
        createdAt: now, updatedAt: now,
        versions: [{
          id: 'ver-1', documentId: 'doc-1', versionNumber: 2,
          content: 'Engagement direction… (extrait démo)',
          status: 'PUBLISHED',
          authorId: 'demo-user', approvedBy: 'demo-user',
          approvedAt: now, publishedAt: now,
          changeNote: 'Mise à jour annuelle 2026',
          createdAt: now, updatedAt: now
        }]
      },
      {
        id: 'doc-2', tenantId: 'demo-tenant',
        code: 'PROC-AUD-001', title: 'Procédure audits internes',
        description: 'Méthodologie + grille d\'audit ISO 9001 §9.2.',
        type: 'PROCEDURE', status: 'ACTIVE',
        ownerId: 'demo-user', mandatoryRead: false,
        currentVersionId: 'ver-2',
        createdAt: now, updatedAt: now,
        versions: [{
          id: 'ver-2', documentId: 'doc-2', versionNumber: 1,
          status: 'IN_REVIEW',
          authorId: 'demo-user',
          changeNote: 'Création — aligne ISO 19011',
          createdAt: now, updatedAt: now
        }]
      },
      {
        id: 'doc-3', tenantId: 'demo-tenant',
        code: 'WI-5S-014', title: 'Mode opératoire — préparation poste atelier A',
        type: 'WORK_INSTRUCTION', status: 'ACTIVE',
        ownerId: 'demo-user', mandatoryRead: true,
        currentVersionId: 'ver-3',
        createdAt: now, updatedAt: now,
        versions: [{
          id: 'ver-3', documentId: 'doc-3', versionNumber: 1,
          status: 'DRAFT',
          authorId: 'demo-user',
          changeNote: 'Brouillon initial',
          createdAt: now, updatedAt: now
        }]
      }
    ];
  }
}
