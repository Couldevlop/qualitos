import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  BillingTier, ModuleActivation, ModuleCatalogEntry, ModuleRow, TenantModuleSummary
} from './admin.types';

/**
 * Activation des modules par tenant (§10.4).
 *
 * L'API (`/api/v1/tenant-modules`, 15 routes) n'avait aucun consommateur : la promesse
 * « un tenant active uniquement les modules dont il a besoin, désactivation à chaud »
 * n'était pilotable que par appel HTTP direct. Ce service l'expose à la console
 * d'administration.
 *
 * Le tenant est dérivé du JWT côté serveur (règle §18.2 #2) : aucune méthode ne le prend
 * en paramètre.
 */
@Injectable({ providedIn: 'root' })
export class TenantModulesService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/tenant-modules`;

  constructor(private readonly http: HttpClient) {}

  /** Catalogue complet des modules de la plateforme. */
  catalog(): Observable<ModuleCatalogEntry[]> {
    return this.http.get<ModuleCatalogEntry[]>(`${this.endpoint}/catalog`);
  }

  /** Synthèse du tenant courant (compteurs + activations). */
  summary(): Observable<TenantModuleSummary> {
    return this.http.get<TenantModuleSummary>(`${this.endpoint}/summary`);
  }

  /**
   * Vue consolidée de l'écran : catalogue joint aux activations du tenant.
   *
   * Le catalogue est la source de vérité de la liste (un module jamais activé doit
   * apparaître, sinon on ne peut pas l'activer). L'activation, quand elle existe, vient
   * s'y greffer. Tri par catégorie puis par nom pour un regroupement lisible.
   */
  overview(): Observable<{ summary: TenantModuleSummary; rows: ModuleRow[] }> {
    return combineLatest([this.catalog(), this.summary()]).pipe(
      map(([catalog, summary]) => {
        const byCode = new Map(summary.activations.map(a => [a.moduleCode, a]));
        const rows = [...catalog]
          .sort((a, b) => (a.category + a.name).localeCompare(b.category + b.name))
          .map(entry => ({ entry, activation: byCode.get(entry.code) ?? null }));
        return { summary, rows };
      })
    );
  }

  activate(moduleCode: string, expiresAt?: string): Observable<ModuleActivation> {
    return this.http.post<ModuleActivation>(`${this.endpoint}/activations`,
      { moduleCode, expiresAt: expiresAt ?? null });
  }

  startTrial(moduleCode: string, trialEndsAt: string): Observable<ModuleActivation> {
    return this.http.post<ModuleActivation>(`${this.endpoint}/activations/trial`,
      { moduleCode, trialEndsAt });
  }

  /** Convertit un essai en activation payante. */
  convertTrial(id: string, expiresAt?: string): Observable<ModuleActivation> {
    return this.http.post<ModuleActivation>(`${this.endpoint}/activations/${id}/convert`,
      { expiresAt: expiresAt ?? null });
  }

  suspend(id: string): Observable<ModuleActivation> {
    return this.http.post<ModuleActivation>(`${this.endpoint}/activations/${id}/suspend`, {});
  }

  resume(id: string): Observable<ModuleActivation> {
    return this.http.post<ModuleActivation>(`${this.endpoint}/activations/${id}/resume`, {});
  }

  disable(id: string): Observable<ModuleActivation> {
    return this.http.post<ModuleActivation>(`${this.endpoint}/activations/${id}/disable`, {});
  }

  changeTier(id: string, newTier: BillingTier): Observable<ModuleActivation> {
    return this.http.post<ModuleActivation>(`${this.endpoint}/activations/${id}/tier`, { newTier });
  }
}
