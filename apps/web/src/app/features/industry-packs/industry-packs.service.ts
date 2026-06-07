import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ActivateRequest, ActivationResponse, IshikawaBranch, IshikawaTemplateView,
  ManifestIshikawaTemplate, ManifestKpi, ManifestPokaYoke, PackManifest,
  PackResponse, PacksPage
} from './industry-packs.types';

/**
 * Accès à l'API Industry Packs (/api/v1/industry-packs) + parsing tolérant du
 * manifeste embarqué (string JSON → PackManifest typé). Aucun appel additionnel :
 * tout le détail riche vient du `manifestJson` du catalogue (ADR 0019 Phase 2).
 */
@Injectable({ providedIn: 'root' })
export class IndustryPacksService {

  private readonly baseEndpoint = `${environment.apiBaseUrl}/api/v1/industry-packs`;

  constructor(private readonly http: HttpClient) {}

  // ---- Catalogue ----

  list(page = 0, size = 50): Observable<PacksPage> {
    return this.http.get<PacksPage>(this.baseEndpoint,
      { params: new HttpParams().set('page', page).set('size', size) });
  }

  get(code: string): Observable<PackResponse> {
    return this.http.get<PackResponse>(`${this.baseEndpoint}/${encodeURIComponent(code)}`);
  }

  // ---- Activation par tenant ----

  activate(code: string, req: ActivateRequest): Observable<ActivationResponse> {
    return this.http.post<ActivationResponse>(
      `${this.baseEndpoint}/${encodeURIComponent(code)}/activate`, req);
  }

  deactivate(code: string, deactivatedBy: string): Observable<ActivationResponse> {
    return this.http.delete<ActivationResponse>(
      `${this.baseEndpoint}/${encodeURIComponent(code)}/activate`,
      { params: new HttpParams().set('deactivatedBy', deactivatedBy) });
  }

  /** Packs actuellement actifs pour le tenant (status ACTIVE). */
  myActivations(): Observable<ActivationResponse[]> {
    return this.http.get<ActivationResponse[]>(`${this.baseEndpoint}/my`);
  }

  myHistory(): Observable<ActivationResponse[]> {
    return this.http.get<ActivationResponse[]>(`${this.baseEndpoint}/my/history`);
  }

  // -------------------------------------------------------------------------
  // Parsing tolérant du manifeste
  // -------------------------------------------------------------------------

  /**
   * Parse `manifestJson` en PackManifest normalisé. Ne lève jamais : un JSON
   * absent, vide ou corrompu retourne un manifeste vide (`rich=false`).
   */
  parseManifest(manifestJson?: string | null): PackManifest {
    const empty: PackManifest = {
      rich: false, sectors: [], standards: [], kpiSlugs: [], kpis: [],
      ishikawaTemplates: [], pokaYoke: [], glossary: [], connectors: []
    };
    if (!manifestJson || !manifestJson.trim()) return empty;

    let raw: Record<string, unknown>;
    try {
      const parsed = JSON.parse(manifestJson);
      if (!parsed || typeof parsed !== 'object') return empty;
      raw = parsed as Record<string, unknown>;
    } catch {
      return empty;
    }

    const strList = (v: unknown): string[] =>
      Array.isArray(v) ? v.filter(x => typeof x === 'string') as string[] : [];

    const sectors = strList(raw['sectors']);
    const standards = strList(raw['standards']);
    const connectors = strList(raw['connectors']);

    // KPIs : schéma plat (string[]) vs riche (richKpis = objets).
    const kpiSlugs = strList(raw['kpis']);
    const kpis: ManifestKpi[] = Array.isArray(raw['richKpis'])
      ? (raw['richKpis'] as ManifestKpi[]).filter(k => k && typeof k === 'object')
      : [];

    const ishikawaTemplates = this.parseIshikawa(raw['ishikawaTemplates']);
    const pokaYoke: ManifestPokaYoke[] = Array.isArray(raw['pokaYokeLibrary'])
      ? (raw['pokaYokeLibrary'] as ManifestPokaYoke[]).filter(p => p && typeof p === 'object')
      : [];
    const glossary = this.parseGlossary(raw['glossary']);

    const rich = kpis.length > 0 || ishikawaTemplates.length > 0 || pokaYoke.length > 0;

    return {
      rich, sectors, standards, kpiSlugs, kpis,
      ishikawaTemplates, pokaYoke, glossary, connectors
    };
  }

  private parseIshikawa(v: unknown): IshikawaTemplateView[] {
    if (!Array.isArray(v)) return [];
    return (v as ManifestIshikawaTemplate[])
      .filter(t => t && typeof t === 'object')
      .map(t => {
        const branches: IshikawaBranch[] = [];
        const b = t.branches;
        if (b && typeof b === 'object') {
          for (const label of Object.keys(b)) {
            const causes = Array.isArray((b as Record<string, unknown>)[label])
              ? ((b as Record<string, string[]>)[label]).filter(c => typeof c === 'string')
              : [];
            branches.push({ label, causes });
          }
        }
        return {
          id: t.id, name: t.name, problemArchetype: t.problemArchetype, branches
        };
      });
  }

  private parseGlossary(v: unknown): { term: string; definition: string }[] {
    if (!v || typeof v !== 'object' || Array.isArray(v)) return [];
    const out: { term: string; definition: string }[] = [];
    for (const term of Object.keys(v as Record<string, unknown>)) {
      const def = (v as Record<string, unknown>)[term];
      out.push({ term, definition: typeof def === 'string' ? def : String(def ?? '') });
    }
    return out;
  }
}
