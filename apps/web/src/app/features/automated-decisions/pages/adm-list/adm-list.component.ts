import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';
import { catchError, debounceTime, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { AdmService } from '../../adm.service';
import { AdmStatus, AdmType, AdmView, TYPE_LABEL } from '../../adm.types';
import { AdmCreateDialogComponent } from '../adm-create-dialog/adm-create-dialog.component';

@Component({
  selector: 'qos-adm-list',
  templateUrl: './adm-list.component.html',
  styleUrls: ['./adm-list.component.scss'],
  standalone: false
})
export class AdmListComponent implements OnInit {

  readonly statusCtrl = new FormControl<AdmStatus | ''>('', { nonNullable: true });
  readonly statuses: AdmStatus[] = ['DRAFT', 'ACTIVE', 'DEPRECATED', 'ARCHIVED'];
  readonly typeLabel = TYPE_LABEL;

  rows$!: Observable<AdmView[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  readonly columns = ['reference', 'name', 'type', 'status', 'updatedAt'];
  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: AdmService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.rows$ = combineLatest([
      this.statusCtrl.valueChanges.pipe(startWith(this.statusCtrl.value), debounceTime(120)),
      this.refresh$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([status]) => this.svc.list(status || undefined).pipe(
        catchError(err => {
          this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
          return of([] as AdmView[]);
        }),
        tap(() => this.loadingState$.next(false))
      )),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  open(r: AdmView): void { this.router.navigate(['/automated-decisions', r.id]); }

  create(): void {
    this.dialog.open(AdmCreateDialogComponent, {
      panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe((r?: AdmView) => {
      if (r) { this.snack.open($localize`:@@automated-decisions.list.created-toast:Décision automatisée créée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); }
    });
  }

  statusBadge(s: AdmStatus): string { return 'badge badge-' + s.toLowerCase(); }
  typeBadge(t: AdmType): string { return 'tpill tpill-' + t.toLowerCase(); }
  typeOf(t: AdmType): string { return this.typeLabel[t]; }
}
