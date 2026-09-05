import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ControlPlanDetail,
  ControlPlanLineRequest,
  ControlPlanLineView,
  ControlPlanView,
  CreateControlPlanRequest,
  CreateProductRequest,
  FailureModeSuggestion,
  ProductComponentRequest,
  ProductComponentResponse,
  ProductOperationRequest,
  ProductOperationResponse,
  ProductResponse,
  RevisionRequestView,
  UpdateProductRequest
} from './products.types';

/**
 * Référentiel Produit et documents qui s'y rattachent.
 *
 * <p>Le tenant vient du jeton côté serveur : aucune méthode ne le prend en
 * paramètre (§18.2 #2). Les erreurs remontent telles quelles — c'est le composant,
 * qui connaît le geste de l'utilisateur, qui décide du message à afficher.
 */
@Injectable({ providedIn: 'root' })
export class ProductsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/products`;

  constructor(private readonly http: HttpClient) {}

  // ---------- produit ----------

  list(): Observable<ProductResponse[]> {
    return this.http.get<ProductResponse[]>(this.endpoint);
  }

  get(id: string): Observable<ProductResponse> {
    return this.http.get<ProductResponse>(`${this.endpoint}/${id}`);
  }

  /**
   * Le classeur Excel du produit : PFMEA et plan de surveillance, deux
   * feuilles, un fichier.
   *
   * <p>`responseType: 'blob'` — sans lui, Angular tenterait de lire le .xlsx
   * comme du JSON et échouerait sur le premier octet, avec une erreur d'analyse
   * qui ne dirait rien du vrai problème.
   *
   * <p>`observe: 'response'` pour lire `Content-Disposition` : c'est le SERVEUR
   * qui nomme le fichier. Le refabriquer côté navigateur ferait diverger les
   * deux noms à la première évolution.
   */
  exportXlsx(id: string): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.endpoint}/${id}/export/xlsx`, {
      responseType: 'blob',
      observe: 'response'
    });
  }

  create(input: CreateProductRequest): Observable<ProductResponse> {
    return this.http.post<ProductResponse>(this.endpoint, input);
  }

  update(id: string, input: UpdateProductRequest): Observable<ProductResponse> {
    return this.http.put<ProductResponse>(`${this.endpoint}/${id}`, input);
  }

  activate(id: string): Observable<ProductResponse> {
    return this.http.post<ProductResponse>(`${this.endpoint}/${id}/activate`, {});
  }

  markObsolete(id: string): Observable<ProductResponse> {
    return this.http.post<ProductResponse>(`${this.endpoint}/${id}/obsolete`, {});
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  // ---------- nomenclature ----------

  components(productId: string): Observable<ProductComponentResponse[]> {
    return this.http.get<ProductComponentResponse[]>(`${this.endpoint}/${productId}/components`);
  }

  addComponent(productId: string, input: ProductComponentRequest): Observable<ProductComponentResponse> {
    return this.http.post<ProductComponentResponse>(
      `${this.endpoint}/${productId}/components`, input);
  }

  updateComponent(productId: string, componentId: string,
                  input: ProductComponentRequest): Observable<ProductComponentResponse> {
    return this.http.put<ProductComponentResponse>(
      `${this.endpoint}/${productId}/components/${componentId}`, input);
  }

  deleteComponent(productId: string, componentId: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${productId}/components/${componentId}`);
  }

  // ---------- gamme ----------

  operations(productId: string): Observable<ProductOperationResponse[]> {
    return this.http.get<ProductOperationResponse[]>(`${this.endpoint}/${productId}/operations`);
  }

  addOperation(productId: string, input: ProductOperationRequest): Observable<ProductOperationResponse> {
    return this.http.post<ProductOperationResponse>(
      `${this.endpoint}/${productId}/operations`, input);
  }

  updateOperation(productId: string, operationId: string,
                  input: ProductOperationRequest): Observable<ProductOperationResponse> {
    return this.http.put<ProductOperationResponse>(
      `${this.endpoint}/${productId}/operations/${operationId}`, input);
  }

  deleteOperation(productId: string, operationId: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${productId}/operations/${operationId}`);
  }

  // ---------- control plan ----------

  controlPlans(productId: string): Observable<ControlPlanView[]> {
    return this.http.get<ControlPlanView[]>(`${this.endpoint}/${productId}/control-plans`);
  }

  controlPlan(productId: string, planId: string): Observable<ControlPlanDetail> {
    return this.http.get<ControlPlanDetail>(`${this.endpoint}/${productId}/control-plans/${planId}`);
  }

  createControlPlan(productId: string, input: CreateControlPlanRequest): Observable<ControlPlanView> {
    return this.http.post<ControlPlanView>(`${this.endpoint}/${productId}/control-plans`, input);
  }

  openRevision(productId: string, planId: string): Observable<ControlPlanView> {
    return this.http.post<ControlPlanView>(
      `${this.endpoint}/${productId}/control-plans/${planId}/revision`, {});
  }

  approveControlPlan(productId: string, planId: string): Observable<ControlPlanView> {
    return this.http.post<ControlPlanView>(
      `${this.endpoint}/${productId}/control-plans/${planId}/approve`, {});
  }

  addLine(productId: string, planId: string,
          input: ControlPlanLineRequest): Observable<ControlPlanLineView> {
    return this.http.post<ControlPlanLineView>(
      `${this.endpoint}/${productId}/control-plans/${planId}/lines`, input);
  }

  updateLine(productId: string, planId: string, lineId: string,
             input: ControlPlanLineRequest): Observable<ControlPlanLineView> {
    return this.http.put<ControlPlanLineView>(
      `${this.endpoint}/${productId}/control-plans/${planId}/lines/${lineId}`, input);
  }

  deleteLine(productId: string, planId: string, lineId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.endpoint}/${productId}/control-plans/${planId}/lines/${lineId}`);
  }

  // ---------- propositions de révision ----------

  revisionRequests(productId: string): Observable<RevisionRequestView[]> {
    return this.http.get<RevisionRequestView[]>(`${this.endpoint}/${productId}/revision-requests`);
  }

  revisionRequestsForTrigger(triggerRefId: string): Observable<RevisionRequestView[]> {
    return this.http.get<RevisionRequestView[]>(
      `${environment.apiBaseUrl}/api/v1/revision-requests`,
      { params: new HttpParams().set('triggerRefId', triggerRefId) });
  }

  acceptRevision(id: string): Observable<RevisionRequestView> {
    return this.http.post<RevisionRequestView>(
      `${environment.apiBaseUrl}/api/v1/revision-requests/${id}/accept`, {});
  }

  rejectRevision(id: string, note: string): Observable<RevisionRequestView> {
    return this.http.post<RevisionRequestView>(
      `${environment.apiBaseUrl}/api/v1/revision-requests/${id}/reject`, { note });
  }

  /** Modes de défaillance déjà analysés qui ressemblent au texte d'une NC. */
  failureModeSuggestions(productId: string, text: string): Observable<FailureModeSuggestion[]> {
    return this.http.get<FailureModeSuggestion[]>(
      `${this.endpoint}/${productId}/failure-mode-suggestions`,
      { params: new HttpParams().set('text', text) });
  }
}
