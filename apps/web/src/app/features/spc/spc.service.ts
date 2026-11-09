import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  KpiOption,
  KpiSpcForecastResponse,
  KpiSpcResponse,
  SpcAnalyzeRequest,
  SpcAnalyzeResponse,
  SpcForecastResponse
} from './spc.types';

interface KpiPage {
  content: Array<{ id: string; code: string; name: string; unit?: string }>;
}

/**
 * Service SPC (§3.4) : relaie la série vers l'engine (`POST /api/v1/ai/spc/analyze`),
 * qui applique les garde-fous IA (OWASP LLM04) et relaie vers ai-service. Le jeton
 * d'auth est attaché par l'intercepteur HTTP ; le tenant est dérivé du JWT côté serveur
 * (jamais envoyé dans le body).
 */
@Injectable({ providedIn: 'root' })
export class SpcService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ai/spc/analyze`;

  constructor(private readonly http: HttpClient) {}

  analyze(req: SpcAnalyzeRequest): Observable<SpcAnalyzeResponse> {
    return this.http.post<SpcAnalyzeResponse>(this.endpoint, req);
  }

  /** Liste des KPI (catalogue) pour le sélecteur du mode « depuis un KPI ». */
  listKpis(): Observable<KpiOption[]> {
    const params = new HttpParams().set('size', 200);
    return this.http.get<KpiPage>(`${environment.apiBaseUrl}/api/v1/kpis`, { params })
      .pipe(map(page => (page.content ?? []).map(k =>
        ({ id: k.id, code: k.code, name: k.name, unit: k.unit }))));
  }

  /**
   * Carte de contrôle SPC d'un KPI : série tirée de kpi_measurements. Si openCapa,
   * ouvre une CAPA corrective sur procédé hors-contrôle (anti-spam côté serveur).
   */
  analyzeKpi(kpiId: string, limit: number, openCapa: boolean): Observable<KpiSpcResponse> {
    const params = new HttpParams().set('limit', limit).set('openCapa', openCapa);
    return this.http.post<KpiSpcResponse>(
      `${this.endpoint.replace('/analyze', '')}/kpi/${kpiId}/analyze`, {}, { params });
  }

  /**
   * Prévision de sortie des limites sur une série saisie (§6.5).
   *
   * <p>La carte dit si le procédé EST hors contrôle ; celle-ci dit quand il VA
   * en sortir — la seule des deux réponses sur laquelle on peut encore agir.
   */
  forecast(values: number[], horizon?: number, center?: number, sigma?: number,
           threshold?: number): Observable<SpcForecastResponse> {
    const body: Record<string, unknown> = { values };
    if (horizon != null) body['horizon'] = horizon;
    if (center != null) body['center'] = center;
    if (sigma != null) body['sigma'] = sigma;
    if (threshold != null) body['threshold'] = threshold;
    return this.http.post<SpcForecastResponse>(
      `${this.endpoint.replace('/analyze', '')}/forecast`, body);
  }

  /**
   * Prévision de dérive d'un KPI : même série que la carte, projetée. Si
   * `openCapa`, ouvre une CAPA PRÉVENTIVE sur sortie annoncée — l'ecart n'est
   * pas survenu, et l'appeler correctif laisserait croire qu'il l'est.
   */
  forecastKpi(kpiId: string, limit: number, horizon: number,
              openCapa: boolean): Observable<KpiSpcForecastResponse> {
    const params = new HttpParams()
      .set('limit', limit).set('horizon', horizon).set('openCapa', openCapa);
    return this.http.post<KpiSpcForecastResponse>(
      `${this.endpoint.replace('/analyze', '')}/kpi/${kpiId}/forecast`, {}, { params });
  }
}
