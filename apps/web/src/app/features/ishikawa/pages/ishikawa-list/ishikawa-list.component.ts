import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { IshikawaService } from '../../ishikawa.service';
import { IshikawaDiagramResponse, IshikawaStatus } from '../../ishikawa.types';
import { IshikawaCreateDialogComponent } from '../ishikawa-create-dialog/ishikawa-create-dialog.component';

@Component({
  selector: 'qos-ishikawa-list',
  templateUrl: './ishikawa-list.component.html',
  styleUrls: ['./ishikawa-list.component.scss'],
  standalone: false
})
export class IshikawaListComponent implements OnInit {

  readonly displayedColumns = ['problem', 'mode', 'status', 'causes', 'updatedAt'];
  readonly statusFilter = new FormControl<IshikawaStatus | ''>('');
  readonly statuses: IshikawaStatus[] = ['DRAFT', 'IN_REVIEW', 'VALIDATED', 'ARCHIVED'];

  diagrams$!: Observable<IshikawaDiagramResponse[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$ = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: IshikawaService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  openDiagram(d: IshikawaDiagramResponse): void {
    this.router.navigate(['/ishikawa', d.id]);
  }

  ngOnInit(): void {
    this.diagrams$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(([status]) =>
        this.svc.listDiagrams(0, 50, status || undefined).pipe(
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
    const ref = this.dialog.open(IshikawaCreateDialogComponent, {
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(created => {
      if (created) this.refresh$.next();
    });
  }

  badgeClass(status: IshikawaStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }
}
