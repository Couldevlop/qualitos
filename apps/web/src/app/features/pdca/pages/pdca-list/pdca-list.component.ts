import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { PdcaService } from '../../pdca.service';
import { PdcaCycleResponse, PdcaStatus } from '../../pdca.types';
import { PdcaCreateDialogComponent } from '../pdca-create-dialog/pdca-create-dialog.component';

@Component({
  selector: 'qos-pdca-list',
  templateUrl: './pdca-list.component.html',
  styleUrls: ['./pdca-list.component.scss'],
  standalone: false
})
export class PdcaListComponent implements OnInit {

  readonly displayedColumns = ['title', 'status', 'steps', 'updatedAt'];
  readonly statusFilter = new FormControl<PdcaStatus | ''>('');
  readonly statuses: PdcaStatus[] = ['PLAN', 'DO', 'CHECK', 'ACT', 'COMPLETED', 'CANCELLED'];

  cycles$!: Observable<PdcaCycleResponse[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$ = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly pdca: PdcaService,
    private readonly dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.cycles$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(([status]) =>
        this.pdca.listCycles(0, 50, status || undefined).pipe(
          catchError(err => {
            this.error$.next(err?.message ?? 'Erreur réseau');
            return [];
          }),
          finalize(() => this.loading$.next(false))
        )
      ),
      map(page => (Array.isArray(page) ? [] : page.content))
    );
  }

  openCreate(): void {
    const ref = this.dialog.open(PdcaCreateDialogComponent, {
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(created => {
      if (created) {
        this.refresh$.next();
      }
    });
  }

  statusBadgeClass(status: PdcaStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }
}
