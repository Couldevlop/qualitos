/**
 * Domain port — repository contract.
 * No Angular imports. The HTTP adapter binds to this via InjectionToken.
 */
import { Observable } from 'rxjs';

import { DashboardLayout } from './dashboard.model';

export interface DashboardLayoutRepository {
  list(): Observable<DashboardLayout[]>;
  get(id: string): Observable<DashboardLayout>;
  save(layout: DashboardLayout): Observable<DashboardLayout>;
  update(id: string, layout: DashboardLayout): Observable<DashboardLayout>;
  delete(id: string): Observable<void>;
}
