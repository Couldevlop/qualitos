import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';
import { catchError, debounceTime, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { EudbService } from '../../eudb.service';
import { EudbStatus, EudbView } from '../../eudb.types';
import { EudbDraftDialogComponent } from '../eudb-draft-dialog/eudb-draft-dialog.component';

@Component({
  selector: 'qos-eudb-list',
  templateUrl: './eudb-list.component.html',
  styleUrls: ['./eudb-list.component.scss'],
  standalone: false
})
export class EudbListComponent implements OnInit {

  readonly statusCtrl = new FormControl<EudbStatus | ''>('', { nonNullable: true });
  readonly statuses: EudbStatus[] = ['DRAFT', 'SUBMITTED', 'REGISTERED', 'UPDATED', 'REJECTED', 'RETIRED'];

  rows$!: Observable<EudbView[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  readonly columns = ['reference', 'eudbId', 'providerEntityName', 'memberState', 'status', 'updatedAt'];
  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: EudbService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.rows$ = combineLatest([
      this.statusCtrl.valueChanges.pipe(startWith(this.statusCtrl.value), debounceTime(120)),
      this.refresh$
    ]).pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(([status]) => this.svc.list(status || undefined).pipe(
        catchError(err => {
          this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
          return of([] as EudbView[]);
        }),
        tap(() => this.loading$.next(false))
      ))
    );
  }

  open(r: EudbView): void { this.router.navigate(['/ai-eudb', r.id]); }

  draft(): void {
    this.dialog.open(EudbDraftDialogComponent, {
      panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe((r?: EudbView) => {
      if (r) { this.snack.open('Brouillon EUDB créé.', 'OK', { duration: 2200 }); this.refresh$.next(); }
    });
  }

  statusBadge(s: EudbStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
