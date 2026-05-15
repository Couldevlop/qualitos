import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { AlignmentBar, KpiCard } from './dashboard.types';

/**
 * Service du dashboard exécutif.
 *
 * Pour le MVP, les KPIs sont mockés. Étape suivante: agrégations Kafka Streams
 * + endpoint `/api/v1/kpis/executive` (cf. CLAUDE.md §6.4 — TimescaleDB pour
 * les KPIs time-series, Redis pour le cache des KPIs précalculés).
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {

  getExecutiveKpis(): Observable<KpiCard[]> {
    return of([
      {
        id: 'coq',
        label: 'Coût d\'Obtention de la Qualité',
        value: 2.8, unit: '% du CA',
        target: 3.2, trend: -0.4,
        description: 'Prévention + détection + défaillances internes + externes',
        icon: 'paid',
        state: 'good'
      },
      {
        id: 'nc',
        label: 'Taux de non-conformités',
        value: 12, unit: '/ 1000 unités',
        target: 15, trend: -1.8,
        description: 'NC ouvertes sur les 30 derniers jours',
        icon: 'warning',
        state: 'good'
      },
      {
        id: 'audits',
        label: 'Audits réalisés vs planifiés',
        value: 87, unit: '%',
        target: 90, trend: +5.0,
        description: 'Sur l\'année en cours',
        icon: 'fact_check',
        state: 'warn'
      },
      {
        id: 'iso9001',
        label: 'Alignement ISO 9001',
        value: 76, unit: '%',
        target: 90, trend: +3.2,
        description: 'Couverture des exigences obligatoires (MUST)',
        icon: 'workspace_premium',
        state: 'warn'
      }
    ]).pipe(delay(150));
  }

  getAlignmentBars(): Observable<AlignmentBar[]> {
    return of([
      { standardCode: 'iso-9001', standardName: 'ISO 9001:2015', score: 76, status: 'IN_PROGRESS' },
      { standardCode: 'iso-27001', standardName: 'ISO/IEC 27001:2022', score: 42, status: 'PLANNING' }
    ]).pipe(delay(150));
  }
}
