import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ActionRequest, CapaLink, CapaRequest, OpportunityAction, OpportunityRequest, OpportunitySheet,
  OpportunityView, RegisterSuggestions, RiskDraft, RiskRequest, RiskSheet, RiskSourceOrigin, RiskView
} from './risk-register.types';

/** Le registre des risques et opportunités. Le tenant vient du jeton, jamais d'ici. */
@Injectable({ providedIn: 'root' })
export class RiskRegisterService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/risk-register`;

  constructor(private readonly http: HttpClient) {}

  // ---------- risques ----------

  risks(): Observable<RiskView[]> {
    return this.http.get<RiskView[]>(`${this.endpoint}/risks`);
  }

  risk(id: string): Observable<RiskSheet> {
    return this.http.get<RiskSheet>(`${this.endpoint}/risks/${encodeURIComponent(id)}`);
  }

  createRisk(input: RiskRequest): Observable<RiskView> {
    return this.http.post<RiskView>(`${this.endpoint}/risks`, input);
  }

  reviseRisk(id: string, input: RiskRequest): Observable<RiskView> {
    return this.http.put<RiskView>(`${this.endpoint}/risks/${encodeURIComponent(id)}`, input);
  }

  openCapa(riskId: string, input: CapaRequest): Observable<CapaLink> {
    return this.http.post<CapaLink>(`${this.endpoint}/risks/${encodeURIComponent(riskId)}/capa`, input);
  }

  /** Le brouillon d'un risque issu d'une AMDEC, d'une NC, d'un audit ou d'un changement. */
  draft(origin: RiskSourceOrigin, sourceId: string): Observable<RiskDraft> {
    return this.http.get<RiskDraft>(
      `${this.endpoint}/sources/${encodeURIComponent(origin)}/${encodeURIComponent(sourceId)}`);
  }

  // ---------- opportunités ----------

  opportunities(): Observable<OpportunityView[]> {
    return this.http.get<OpportunityView[]>(`${this.endpoint}/opportunities`);
  }

  opportunity(id: string): Observable<OpportunitySheet> {
    return this.http.get<OpportunitySheet>(`${this.endpoint}/opportunities/${encodeURIComponent(id)}`);
  }

  createOpportunity(input: OpportunityRequest): Observable<OpportunityView> {
    return this.http.post<OpportunityView>(`${this.endpoint}/opportunities`, input);
  }

  reviseOpportunity(id: string, input: OpportunityRequest): Observable<OpportunityView> {
    return this.http.put<OpportunityView>(`${this.endpoint}/opportunities/${encodeURIComponent(id)}`, input);
  }

  addAction(opportunityId: string, input: ActionRequest): Observable<OpportunityAction> {
    return this.http.post<OpportunityAction>(
      `${this.endpoint}/opportunities/${encodeURIComponent(opportunityId)}/actions`, input);
  }

  reviseAction(opportunityId: string, actionId: string, input: ActionRequest): Observable<OpportunityAction> {
    return this.http.put<OpportunityAction>(
      `${this.endpoint}/opportunities/${encodeURIComponent(opportunityId)}/actions/${encodeURIComponent(actionId)}`,
      input);
  }

  deleteAction(opportunityId: string, actionId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.endpoint}/opportunities/${encodeURIComponent(opportunityId)}/actions/${encodeURIComponent(actionId)}`);
  }

  // ---------- saisie assistée ----------

  suggestions(): Observable<RegisterSuggestions> {
    return this.http.get<RegisterSuggestions>(`${this.endpoint}/suggestions`);
  }
}
