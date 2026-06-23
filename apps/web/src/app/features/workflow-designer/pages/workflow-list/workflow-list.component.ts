import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { WorkflowDesignerService } from '../../workflow-designer.service';
import { WorkflowStatus, WorkflowSummary } from '../../workflow-designer.types';

@Component({
  selector: 'qos-workflow-list',
  templateUrl: './workflow-list.component.html',
  styleUrls: ['./workflow-list.component.scss'],
  standalone: false
})
export class WorkflowListComponent implements OnInit {

  readonly statusFilter = new FormControl<WorkflowStatus | ''>('');
  readonly statuses: WorkflowStatus[] = ['DRAFT', 'PUBLISHED', 'ARCHIVED'];

  workflows$!: Observable<WorkflowSummary[]>;

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: WorkflowDesignerService,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.workflows$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([status]) =>
        this.svc.list(0, 50, { status: status || undefined }).pipe(
          catchError(err => {
            this.errorState$.next(
              safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
            return [];
          }),
          finalize(() => this.loadingState$.next(false))
        )
      ),
      map(page => Array.isArray(page) ? [] : page.content),
      shareReplay({ bufferSize: 1, refCount: false }) // refCount:false : evite la boucle de teardown quand *ngIf loading masque la table
    );
  }

  createNew(): void {
    this.router.navigate(['/workflow-designer', 'new']);
  }

  open(wf: WorkflowSummary): void {
    this.router.navigate(['/workflow-designer', wf.id]);
  }

  publish(wf: WorkflowSummary, ev: Event): void {
    ev.stopPropagation();
    this.svc.publish(wf.id).subscribe({
      next: () => {
        this.snackBar.open($localize`:@@workflow.list.published:Workflow publié.`, '', { duration: 2500 });
        this.refresh$.next();
      },
      error: err => this.snackBar.open(
        safeErrorMessage(err, $localize`:@@workflow.list.publish-error:Publication impossible.`),
        '', { duration: 3500 })
    });
  }

  archive(wf: WorkflowSummary, ev: Event): void {
    ev.stopPropagation();
    this.svc.archive(wf.id).subscribe({
      next: () => {
        this.snackBar.open($localize`:@@workflow.list.archived:Workflow archivé.`, '', { duration: 2500 });
        this.refresh$.next();
      },
      error: err => this.snackBar.open(
        safeErrorMessage(err, $localize`:@@workflow.list.archive-error:Archivage impossible.`),
        '', { duration: 3500 })
    });
  }

  statusBadgeClass(status: WorkflowStatus): string {
    return 'wf-badge wf-' + status.toLowerCase();
  }
}
