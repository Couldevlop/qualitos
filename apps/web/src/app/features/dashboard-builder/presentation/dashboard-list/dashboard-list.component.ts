/**
 * List view — shows the user's dashboards + shared ones.
 */
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { shareReplay } from 'rxjs/operators';

import { DashboardBuilderService } from '../../application/dashboard-builder.service';
import { DashboardLayout } from '../../domain/dashboard.model';

@Component({
  selector: 'qos-dashboard-list',
  templateUrl: './dashboard-list.component.html',
  styleUrls: ['./dashboard-list.component.scss'],
  standalone: false
})
export class DashboardListComponent implements OnInit {
  layouts$!: Observable<DashboardLayout[]>;

  constructor(
    private readonly svc: DashboardBuilderService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.layouts$ = this.svc.list().pipe(shareReplay(1));
  }

  open(layout: DashboardLayout): void {
    this.router.navigate(['/dashboard-builder', layout.id]);
  }

  create(): void {
    this.router.navigate(['/dashboard-builder', 'new']);
  }
}
