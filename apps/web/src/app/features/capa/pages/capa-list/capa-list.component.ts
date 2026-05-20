import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { CapaService } from '../../capa.service';
import { CapaCaseResponse, CapaCriticity, CapaStatus } from '../../capa.types';
import { CapaCreateDialogComponent } from '../capa-create-dialog/capa-create-dialog.component';

@Component({
  selector: 'qos-capa-list',
  templateUrl: './capa-list.component.html',
  styleUrls: ['./capa-list.component.scss'],
  standalone: false
})
export class CapaListComponent implements OnInit {

  readonly displayedColumns = ['title', 'type', 'criticity', 'status', 'dueDate'];
  readonly statusFilter = new FormControl<CapaStatus | ''>('');
  readonly statuses: CapaStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED'];

  cases$!: Observable<CapaCaseResponse[]>;
  loading$ = new BehaviorSubject<boolean>(false);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: CapaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  openCase(c: CapaCaseResponse): void {
    this.snack.open(
      `Détail du cas "${c.title}" — en cours d'implémentation.`,
      'OK',
      { duration: 3000 }
    );
  }

  ngOnInit(): void {
    this.cases$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      tap(() => this.loading$.next(true)),
      switchMap(([status]) =>
        this.svc.listCases(0, 50, status || undefined).pipe(
          catchError(() => []),
          finalize(() => this.loading$.next(false))
        )
      ),
      map(page => (Array.isArray(page) ? [] : page.content))
    );
  }

  openCreate(): void {
    const ref = this.dialog.open(CapaCreateDialogComponent, {
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(created => {
      if (created) this.refresh$.next();
    });
  }

  statusBadge(s: CapaStatus): string { return 'badge badge-' + s.toLowerCase(); }
  criticityBadge(c: CapaCriticity): string { return 'crit crit-' + c.toLowerCase(); }
}
