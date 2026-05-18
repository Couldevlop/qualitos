/**
 * HTTP adapter — implements the domain port via api-quality-engine endpoint
 * `/api/v1/dashboards/custom`. Tenant id is propagated by the global JWT
 * interceptor; never sent in the body (CLAUDE.md §18.2).
 */
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';

import { DashboardLayout, Widget } from '../domain/dashboard.model';
import { DashboardLayoutRepository } from '../domain/dashboard-layout.repository';
import { environment } from '../../../../environments/environment';

interface DashboardLayoutPayload {
  id: string;
  tenantId: string;
  userId: string;
  name: string;
  description?: string;
  layoutJson: string;
  shared: boolean;
  signatureHash?: string;
  version: number;
}

interface SaveRequest {
  name: string;
  description?: string;
  layoutJson: string;
  shared: boolean;
}

@Injectable()
export class DashboardHttpRepository implements DashboardLayoutRepository {

  private readonly baseUrl = environment.apiBaseUrl + '/api/v1/dashboards/custom';

  constructor(private readonly http: HttpClient) {}

  list(): Observable<DashboardLayout[]> {
    return this.http.get<DashboardLayoutPayload[]>(this.baseUrl).pipe(
      map(rows => rows.map(p => this.toDomain(p)))
    );
  }

  get(id: string): Observable<DashboardLayout> {
    return this.http.get<DashboardLayoutPayload>(`${this.baseUrl}/${id}`).pipe(
      map(p => this.toDomain(p))
    );
  }

  save(layout: DashboardLayout): Observable<DashboardLayout> {
    return this.http.post<DashboardLayoutPayload>(this.baseUrl, this.toRequest(layout)).pipe(
      map(p => this.toDomain(p))
    );
  }

  update(id: string, layout: DashboardLayout): Observable<DashboardLayout> {
    return this.http.put<DashboardLayoutPayload>(`${this.baseUrl}/${id}`, this.toRequest(layout)).pipe(
      map(p => this.toDomain(p))
    );
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  private toDomain(p: DashboardLayoutPayload): DashboardLayout {
    let widgets: Widget[] = [];
    try {
      const parsed = JSON.parse(p.layoutJson);
      widgets = Array.isArray(parsed?.widgets) ? parsed.widgets : [];
    } catch {
      widgets = [];
    }
    return {
      id: p.id,
      tenantId: p.tenantId,
      userId: p.userId,
      name: p.name,
      description: p.description,
      widgets,
      shared: p.shared,
      signatureHash: p.signatureHash,
      version: p.version
    };
  }

  private toRequest(layout: DashboardLayout): SaveRequest {
    return {
      name: layout.name,
      description: layout.description,
      layoutJson: JSON.stringify({ widgets: layout.widgets }),
      shared: layout.shared
    };
  }
}
