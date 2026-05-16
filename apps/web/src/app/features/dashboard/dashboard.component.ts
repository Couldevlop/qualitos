import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { shareReplay } from 'rxjs/operators';

import { DashboardService } from './dashboard.service';
import { AlignmentBar, KpiCard } from './dashboard.types';

@Component({
  selector: 'qos-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  standalone: false
})
export class DashboardComponent implements OnInit {

  kpis$!: Observable<KpiCard[]>;
  alignments$!: Observable<AlignmentBar[]>;

  constructor(private readonly svc: DashboardService) {}

  ngOnInit(): void {
    this.kpis$ = this.svc.getExecutiveKpis().pipe(shareReplay(1));
    this.alignments$ = this.svc.getAlignmentBars().pipe(shareReplay(1));
  }

  trendClass(trend?: number): string {
    if (trend == null) return '';
    return trend > 0 ? 'trend trend-up' : 'trend trend-down';
  }

  trendIcon(trend?: number): string {
    if (trend == null) return 'remove';
    return trend > 0 ? 'trending_up' : 'trending_down';
  }
}
