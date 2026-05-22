import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiQmsService } from '../../ai-qms.service';
import { AiQmsStatus, AiQmsView } from '../../ai-qms.types';
import { AiQmsDialogComponent } from '../ai-qms-dialog/ai-qms-dialog.component';

@Component({
  selector: 'qos-ai-qms-list',
  templateUrl: './ai-qms-list.component.html',
  styleUrls: ['./ai-qms-list.component.scss'],
  standalone: false
})
export class AiQmsListComponent implements OnInit {

  readonly displayedColumns = ['reference', 'name', 'version', 'status', 'updatedAt'];

  readonly statuses: AiQmsStatus[] = ['DRAFT', 'APPROVED', 'IN_FORCE', 'SUPERSEDED', 'ARCHIVED'];
  readonly statusFilter = new FormControl<AiQmsStatus | ''>('IN_FORCE');

  rows$!: Observable<AiQmsView[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: AiQmsService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.rows$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(([status]) =>
        this.svc.list(status || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[ai-qms-list] failed', err?.status, err?.error?.title);
            this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
            return of([] as AiQmsView[]);
          }),
          finalize(() => this.loading$.next(false))
        )
      )
    );
  }

  openCreate(): void {
    const ref = this.dialog.open(AiQmsDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(q => { if (q) this.router.navigate(['/ai-qms', q.id]); });
  }

  open(q: AiQmsView): void { this.router.navigate(['/ai-qms', q.id]); }

  statusBadge(s: AiQmsStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
