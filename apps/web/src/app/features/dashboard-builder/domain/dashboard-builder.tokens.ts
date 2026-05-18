/**
 * Injection tokens — keep the domain port decoupled from any concrete adapter.
 * Angular is imported here ONLY for InjectionToken<T>; the contract itself
 * (DashboardLayoutRepository) stays Angular-free.
 */
import { InjectionToken } from '@angular/core';

import { DashboardLayoutRepository } from './dashboard-layout.repository';

export const DASHBOARD_LAYOUT_REPOSITORY =
  new InjectionToken<DashboardLayoutRepository>('DashboardLayoutRepository');
