/**
 * Domain port — repository contract.
 * No Angular imports. The HTTP adapter binds to this via InjectionToken.
 */
import { Observable } from 'rxjs';

import {
  DashboardExportResult,
  DashboardLayout,
  ExportWidgetSnapshot
} from './dashboard.model';

export interface DashboardLayoutRepository {
  list(): Observable<DashboardLayout[]>;
  get(id: string): Observable<DashboardLayout>;
  save(layout: DashboardLayout): Observable<DashboardLayout>;
  update(id: string, layout: DashboardLayout): Observable<DashboardLayout>;
  delete(id: string): Observable<void>;

  /**
   * Request a signed (ML-DSA) + blockchain-anchored PDF export of a dashboard,
   * with a verification QR code (§7.3/§7.4). Returns the PDF blob plus the
   * integrity metadata surfaced in the response headers.
   */
  exportPdf(id: string, widgets: ReadonlyArray<ExportWidgetSnapshot>): Observable<DashboardExportResult>;
}
