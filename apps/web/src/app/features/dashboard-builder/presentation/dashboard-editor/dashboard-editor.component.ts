/**
 * Drag-and-drop dashboard editor.
 * Pluggable grid: when angular-gridster2 is installed, swap the basic
 * css-grid mock-up below with `<gridster><gridster-item>...`.
 */
import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

import { DashboardBuilderService } from '../../application/dashboard-builder.service';
import {
  DashboardLayout,
  Widget,
  WidgetType
} from '../../domain/dashboard.model';

@Component({
  selector: 'qos-dashboard-editor',
  templateUrl: './dashboard-editor.component.html',
  styleUrls: ['./dashboard-editor.component.scss'],
  standalone: false
})
export class DashboardEditorComponent implements OnInit, OnDestroy {

  layout!: DashboardLayout;
  loading = false;
  saving = false;
  isNew = true;
  readonly availableTypes: ReadonlyArray<WidgetType> = [
    'kpi', 'line', 'bar', 'pie', 'gauge', 'table', 'heatmap', 'narrative'
  ];

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly svc: DashboardBuilderService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.route.paramMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const id = params.get('id');
        if (id) {
          this.isNew = false;
          this.svc.get(id).subscribe({
            next: l => { this.layout = l; this.loading = false; },
            error: () => { this.loading = false; this.snack.open('Layout not found', 'OK'); }
          });
        } else {
          this.layout = this.svc.newDefaultLayout('New dashboard');
          this.loading = false;
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  addWidget(type: WidgetType): void {
    const widget: Widget = {
      id: this.svc.generateId(),
      type,
      title: `${type.toUpperCase()} widget`,
      position: { x: 0, y: 0, cols: 3, rows: 2 },
      config: {}
    };
    this.layout = { ...this.layout, widgets: [...this.layout.widgets, widget] };
  }

  removeWidget(id: string): void {
    this.layout = {
      ...this.layout,
      widgets: this.layout.widgets.filter(w => w.id !== id)
    };
  }

  save(): void {
    this.saving = true;
    const op$ = this.isNew && !this.layout.id
      ? this.svc.save(this.layout)
      : this.svc.update(this.layout.id!, this.layout);
    op$.subscribe({
      next: saved => {
        this.layout = saved;
        this.isNew = false;
        this.saving = false;
        this.snack.open(`Saved "${saved.name}"`, 'OK', { duration: 2500 });
      },
      error: () => {
        this.saving = false;
        this.snack.open('Save failed', 'OK', { duration: 3500 });
      }
    });
  }

  back(): void {
    this.router.navigate(['../'], { relativeTo: this.route });
  }

  onNameChange(value: string): void {
    this.layout = { ...this.layout, name: value };
  }

  onSharedChange(value: boolean): void {
    this.layout = { ...this.layout, shared: value };
  }

  trackById(_: number, w: Widget): string {
    return w.id;
  }
}
