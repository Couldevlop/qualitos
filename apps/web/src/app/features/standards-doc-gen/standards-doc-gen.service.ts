import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  DossierDocumentView, DossierFinalizeRequest, DossierStartRequest, DossierView,
  NormDocApproveRequest, NormDocRejectRequest, NormDocView
} from './standards-doc-gen.types';

/**
 * Accès API — génération documentaire IA AVANCÉE multi-documents (§8.8).
 * Endpoints : /api/v1/standards/doc-dossiers.
 */
@Injectable({ providedIn: 'root' })
export class StandardsDocGenService {

  private readonly baseEndpoint = `${environment.apiBaseUrl}/api/v1/standards/doc-dossiers`;

  constructor(private readonly http: HttpClient) {}

  /** Catalogue des pièces générables (pour l'UI de sélection). */
  catalog(): Observable<DossierDocumentView[]> {
    return this.http.get<DossierDocumentView[]>(`${this.baseEndpoint}/catalog`);
  }

  list(): Observable<DossierView[]> {
    return this.http.get<DossierView[]>(this.baseEndpoint);
  }

  get(id: string): Observable<DossierView> {
    return this.http.get<DossierView>(`${this.baseEndpoint}/${id}`);
  }

  /** Démarre la génération en lot d'un dossier complet. */
  start(req: DossierStartRequest): Observable<DossierView> {
    return this.http.post<DossierView>(this.baseEndpoint, req);
  }

  /** Relance la génération des pièces en attente / en échec. */
  retry(id: string): Observable<DossierView> {
    return this.http.post<DossierView>(`${this.baseEndpoint}/${id}/retry`, {});
  }

  /** Finalise : exige toutes les pièces approuvées, scelle et ancre. */
  finalize(id: string, req: DossierFinalizeRequest): Observable<DossierView> {
    return this.http.post<DossierView>(`${this.baseEndpoint}/${id}/finalize`, req);
  }

  // ---- Pièces : workflow de validation humaine (ADR 0032) ----

  private readonly normDocEndpoint = `${environment.apiBaseUrl}/api/v1/standards/norm-documents`;

  getNormDoc(id: string): Observable<NormDocView> {
    return this.http.get<NormDocView>(`${this.normDocEndpoint}/${id}`);
  }

  submitNormDoc(id: string): Observable<NormDocView> {
    return this.http.post<NormDocView>(`${this.normDocEndpoint}/${id}/submit`, {});
  }

  approveNormDoc(id: string, req: NormDocApproveRequest): Observable<NormDocView> {
    return this.http.post<NormDocView>(`${this.normDocEndpoint}/${id}/approve`, req);
  }

  rejectNormDoc(id: string, req: NormDocRejectRequest): Observable<NormDocView> {
    return this.http.post<NormDocView>(`${this.normDocEndpoint}/${id}/reject`, req);
  }
}
