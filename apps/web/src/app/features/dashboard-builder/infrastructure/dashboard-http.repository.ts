/**
 * HTTP adapter — implements the domain port via api-quality-engine endpoint
 * `/api/v1/dashboards/custom`. Tenant id is propagated by the global JWT
 * interceptor; never sent in the body (CLAUDE.md §18.2).
 */
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';

import {
  DashboardExportResult,
  DashboardLayout,
  ExportWidgetSnapshot,
  Widget
} from '../domain/dashboard.model';
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

  exportPdf(id: string, widgets: ReadonlyArray<ExportWidgetSnapshot>): Observable<DashboardExportResult> {
    return this.http.post(`${this.baseUrl}/${id}/export/pdf`, { widgets },
      { observe: 'response', responseType: 'blob' }).pipe(
      map(resp => ({
        blob: resp.body ?? new Blob([], { type: 'application/pdf' }),
        fileName: this.fileNameFromDisposition(resp.headers.get('Content-Disposition'))
          ?? `dashboard-${id}.pdf`,
        verificationCode: resp.headers.get('X-Export-Verification-Code') ?? '',
        sha256: resp.headers.get('X-Export-Sha256') ?? '',
        anchorRef: resp.headers.get('X-Export-Anchor-Ref') ?? ''
      }))
    );
  }

  private fileNameFromDisposition(value: string | null): string | null {
    if (!value) {
      return null;
    }
    const match = /filename="?([^"]+)"?/.exec(value);
    return match ? match[1] : null;
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
