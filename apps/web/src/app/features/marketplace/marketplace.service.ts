import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  InstallationView, MarketplacePackView, SubmitRequest
} from './marketplace.types';

/**
 * Accès à l'API Marketplace de packs normatifs (/api/v1/marketplace/packs).
 * Aucun tenant_id n'est jamais envoyé : il est dérivé du JWT côté serveur.
 */
@Injectable({ providedIn: 'root' })
export class MarketplaceService {

  private readonly base = `${environment.apiBaseUrl}/api/v1/marketplace/packs`;

  constructor(private readonly http: HttpClient) {}

  // ---- Catalogue public ----

  listPublished(sector?: string): Observable<MarketplacePackView[]> {
    let params = new HttpParams();
    if (sector && sector.trim()) {
      params = params.set('sector', sector.trim());
    }
    return this.http.get<MarketplacePackView[]>(this.base, { params });
  }

  get(id: string): Observable<MarketplacePackView> {
    return this.http.get<MarketplacePackView>(`${this.base}/${encodeURIComponent(id)}`);
  }

  // ---- Soumission partenaire ----

  submit(req: SubmitRequest): Observable<MarketplacePackView> {
    return this.http.post<MarketplacePackView>(this.base, req);
  }

  // ---- Modération éditeur (SUPER_ADMIN) ----

  moderationQueue(): Observable<MarketplacePackView[]> {
    return this.http.get<MarketplacePackView[]>(`${this.base}/moderation/queue`);
  }

  takeForReview(id: string): Observable<MarketplacePackView> {
    return this.http.post<MarketplacePackView>(`${this.base}/${encodeURIComponent(id)}/take-review`, {});
  }

  publish(id: string): Observable<MarketplacePackView> {
    return this.http.post<MarketplacePackView>(`${this.base}/${encodeURIComponent(id)}/publish`, {});
  }

  reject(id: string, reason: string): Observable<MarketplacePackView> {
    return this.http.post<MarketplacePackView>(
      `${this.base}/${encodeURIComponent(id)}/reject`, { reason });
  }

  deprecate(id: string): Observable<MarketplacePackView> {
    return this.http.post<MarketplacePackView>(`${this.base}/${encodeURIComponent(id)}/deprecate`, {});
  }

  // ---- Installation tenant (ADMIN_TENANT) ----

  install(id: string): Observable<InstallationView> {
    return this.http.post<InstallationView>(`${this.base}/${encodeURIComponent(id)}/install`, {});
  }

  uninstall(installationId: string): Observable<InstallationView> {
    return this.http.delete<InstallationView>(
      `${this.base}/installations/${encodeURIComponent(installationId)}`);
  }

  myInstallations(): Observable<InstallationView[]> {
    return this.http.get<InstallationView[]>(`${this.base}/installations/my`);
  }

  myInstallationHistory(): Observable<InstallationView[]> {
    return this.http.get<InstallationView[]>(`${this.base}/installations/my/history`);
  }

  // ---- Notation ----

  rate(id: string, stars: number): Observable<MarketplacePackView> {
    return this.http.post<MarketplacePackView>(
      `${this.base}/${encodeURIComponent(id)}/rate`, { stars });
  }
}
