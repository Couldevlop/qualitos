import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';
import { catchError, debounceTime, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { FriaService } from '../../fria.service';
import { FriaStatus, FriaView } from '../../fria.types';
import { FriaDraftDialogComponent } from '../fria-draft-dialog/fria-draft-dialog.component';

@Component({
  selector: 'qos-fria-list',
  templateUrl: './fria-list.component.html',
  styleUrls: ['./fria-list.component.scss'],
  standalone: false
})
export class FriaListComponent implements OnInit {

  readonly statusCtrl = new FormControl<FriaStatus | ''>('', { nonNullable: true });
  readonly statuses: FriaStatus[] = ['DRAFT', 'SUBMITTED', 'APPROVED', 'ARCHIVED'];

  rows$!: Observable<FriaView[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  readonly columns = ['reference', 'aiSystem', 'affectedPersons', 'status', 'updatedAt'];
  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: FriaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.rows$ = combineLatest([
      this.statusCtrl.valueChanges.pipe(startWith(this.statusCtrl.value), debounceTime(120)),
      this.refresh$
    ]).pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(([status]) => this.svc.list(status || undefined).pipe(
        catchError(err => {
          this.error$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
          return of([] as FriaView[]);
        }),
        tap(() => this.loading$.next(false))
      ))
    );
  }

  open(f: FriaView): void { this.router.navigate(['/fria', f.id]); }

  draft(): void {
    this.dialog.open(FriaDraftDialogComponent, {
      panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe((f?: FriaView) => {
      if (f) { this.snack.open($localize`:@@fria.list.created:Brouillon FRIA créé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); }
    });
  }

  statusBadge(s: FriaStatus): string { return 'badge badge-' + s.toLowerCase(); }

  truncate(s: string | null | undefined, n = 80): string {
    if (!s) return '—';
    return s.length > n ? s.slice(0, n) + '…' : s;
  }
}
