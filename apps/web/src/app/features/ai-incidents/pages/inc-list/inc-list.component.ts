import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiIncidentsService } from '../../ai-inc.service';
import { AiIncSeverity, AiIncStatus, AiIncView } from '../../ai-inc.types';
import { IncDetectDialogComponent } from '../inc-detect-dialog/inc-detect-dialog.component';

type Mode = 'all' | 'overdue';

@Component({
  selector: 'qos-inc-list',
  templateUrl: './inc-list.component.html',
  styleUrls: ['./inc-list.component.scss'],
  standalone: false
})
export class IncListComponent implements OnInit {

  readonly displayedColumns = ['reference', 'severity', 'description', 'status', 'deadline'];

  readonly statuses: AiIncStatus[] = ['DETECTED', 'INVESTIGATING', 'NOTIFIED_REGULATOR', 'CLOSED', 'DISMISSED'];
  readonly statusFilter = new FormControl<AiIncStatus | ''>('');
  readonly modeFilter   = new FormControl<Mode>('all');

  readonly modes: { value: Mode; label: string }[] = [
    { value: 'all',     label: 'Tous les incidents' },
    { value: 'overdue', label: 'Notification régulateur en retard' }
  ];

  rows$!: Observable<AiIncView[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: AiIncidentsService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.rows$ = combineLatest([
      this.modeFilter.valueChanges.pipe(startWith(this.modeFilter.value)),
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(([mode, status]) => {
        const op$ = mode === 'overdue' ? this.svc.listOverdue(200) : this.svc.list(status || undefined);
        return op$.pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[inc-list] failed', err?.status, err?.error?.title);
            this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
            return of([] as AiIncView[]);
          }),
          finalize(() => this.loading$.next(false))
        );
      })
    );
  }

  openDetect(): void {
    const ref = this.dialog.open(IncDetectDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(i => { if (i) this.router.navigate(['/ai-incidents', i.id]); });
  }

  open(i: AiIncView): void { this.router.navigate(['/ai-incidents', i.id]); }
  isOverdue(): boolean { return this.modeFilter.value === 'overdue'; }

  severityLabel(s: AiIncSeverity): string {
    return ({
      DEATH_OR_SERIOUS_HARM_TO_HEALTH: 'Décès / atteinte grave santé (2j)',
      SERIOUS_INFRINGEMENT_FUNDAMENTAL_RIGHTS: 'Atteinte droits fondamentaux (10j)',
      CRITICAL_INFRASTRUCTURE_DISRUPTION: 'Infra critique (15j)',
      SERIOUS_PROPERTY_OR_ENVIRONMENTAL_DAMAGE: 'Dommage bien/env. (15j)'
    })[s];
  }
  severityBadge(s: AiIncSeverity): string { return 'sev sev-' + s.toLowerCase(); }
  statusBadge(s: AiIncStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
