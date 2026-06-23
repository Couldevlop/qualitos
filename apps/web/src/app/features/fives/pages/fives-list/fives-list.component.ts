import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';
import type { EChartsCoreOption } from 'echarts/core';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { FivesService } from '../../fives.service';
import { FiveSAuditResponse, FiveSAuditStatus } from '../../fives.types';
import { FivesCreateDialogComponent } from '../fives-create-dialog/fives-create-dialog.component';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

@Component({
  selector: 'qos-fives-list',
  templateUrl: './fives-list.component.html',
  styleUrls: ['./fives-list.component.scss'],
  standalone: false
})
export class FivesListComponent implements OnInit {

  readonly displayedColumns = ['zone', 'status', 'score', 'scheduledAt', 'updatedAt'];
  readonly statusFilter = new FormControl<FiveSAuditStatus | ''>('');
  readonly statuses: FiveSAuditStatus[] = ['DRAFT', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  audits$!: Observable<FiveSAuditResponse[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  /** Heatmap des scores 5S par zone × mois (CLAUDE.md §3.2). */
  heatmapOption$!: Observable<EChartsCoreOption | null>;

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly page$ = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly svc: FivesService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.audits$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.page$,
      this.refresh$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([status, p]) =>
        this.svc.listAudits(p.index, p.size, status || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[fives-list] listAudits failed', err?.status, err?.error?.title);
            this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
            return [];
          }),
          finalize(() => this.loadingState$.next(false))
        )
      ),
      map(page => {
        if (Array.isArray(page)) return [];
        this.totalElements = page.totalElements;
        return page.content;
      }),
      shareReplay({ bufferSize: 1, refCount: false }) // refCount:false : evite la boucle de teardown quand *ngIf loading masque la table
    );

    // Heatmap : agrège un échantillon élargi (jusqu'à 100 audits) par zone × mois.
    // Indépendante du filtre/pagination de la table ; rechargée sur refresh$.
    this.heatmapOption$ = this.refresh$.pipe(
      switchMap(() => this.svc.listAudits(0, 100).pipe(
        catchError(() => of(null)),
        map(page => this.buildHeatmapOption(page && !Array.isArray(page) ? page.content : []))
      )),
      shareReplay({ bufferSize: 1, refCount: false })
    );
  }

  /**
   * Construit l'option ECharts d'une heatmap zone (Y) × mois (X) colorée par le
   * score 5S moyen (0-100). Fonction pure → testable. Retourne `null` s'il n'y a
   * pas assez de données notées (le template affiche alors un état vide).
   */
  buildHeatmapOption(audits: FiveSAuditResponse[]): EChartsCoreOption | null {
    const scored = (audits ?? []).filter(a => a.overallScore != null && !!a.zone);
    if (scored.length === 0) return null;

    const monthOf = (iso: string): string => (iso ?? '').slice(0, 7); // YYYY-MM
    const zones = Array.from(new Set(scored.map(a => a.zone))).sort();
    const months = Array.from(new Set(scored.map(a => monthOf(a.updatedAt)))).filter(Boolean).sort();
    if (zones.length === 0 || months.length === 0) return null;

    // Moyenne des scores par (zone, mois).
    const sum = new Map<string, { total: number; n: number }>();
    for (const a of scored) {
      const key = a.zone + '|' + monthOf(a.updatedAt);
      const acc = sum.get(key) ?? { total: 0, n: 0 };
      acc.total += a.overallScore as number;
      acc.n += 1;
      sum.set(key, acc);
    }
    const data: [number, number, number][] = [];
    months.forEach((m, x) => zones.forEach((z, y) => {
      const acc = sum.get(z + '|' + m);
      if (acc) data.push([x, y, Math.round(acc.total / acc.n)]);
    }));

    const labelMonth = (m: string): string => {
      const [yy, mm] = m.split('-');
      return `${mm}/${yy.slice(2)}`;
    };
    return {
      tooltip: {
        position: 'top',
        formatter: (p: { value: number[] }) =>
          `<strong>${zones[p.value[1]]}</strong><br/>${labelMonth(months[p.value[0]])} · ${p.value[2]}/100`
      },
      grid: { top: 16, bottom: 8, left: 90, right: 16, containLabel: true },
      xAxis: {
        type: 'category', data: months.map(labelMonth), splitArea: { show: true },
        axisLine: { show: false }, axisTick: { show: false }
      },
      yAxis: {
        type: 'category', data: zones, splitArea: { show: true },
        axisLine: { show: false }, axisTick: { show: false }
      },
      visualMap: {
        min: 0, max: 100, calculable: false, orient: 'horizontal',
        left: 'center', bottom: 0, itemHeight: 90, itemWidth: 10,
        inRange: { color: ['#fee2e2', '#fde68a', '#bbf7d0', '#10b981'] },
        textStyle: { fontSize: 10 }
      },
      series: [{
        type: 'heatmap',
        data,
        label: { show: true, formatter: (p: { value: number[] }) => String(p.value[2]), fontSize: 10 },
        itemStyle: { borderRadius: 4, borderWidth: 1, borderColor: 'transparent' },
        emphasis: { itemStyle: { borderColor: 'rgba(0,0,0,0.2)' } }
      }]
    } as EChartsCoreOption;
  }

  onPage(e: PageEvent): void {
    this.pageIndex = Math.max(0, e.pageIndex);
    this.pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, e.pageSize));
    this.page$.next({ index: this.pageIndex, size: this.pageSize });
  }

  openCreate(): void {
    const ref = this.dialog.open(FivesCreateDialogComponent, {
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(created => {
      if (created) {
        this.pageIndex = 0;
        this.page$.next({ index: 0, size: this.pageSize });
        this.refresh$.next();
      }
    });
  }

  openAudit(a: FiveSAuditResponse): void {
    this.router.navigate(['/fives', a.id]);
  }

  badgeClass(status: FiveSAuditStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }

  scoreClass(score?: number): string {
    if (score == null) return 'score';
    if (score >= 80) return 'score score-high';
    if (score >= 60) return 'score score-mid';
    return 'score score-low';
  }
}
