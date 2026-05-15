import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { BehaviorSubject, Observable } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { IshikawaService } from '../../ishikawa.service';
import { IshikawaDiagramResponse, IshikawaStatus } from '../../ishikawa.types';

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

  constructor(private readonly svc: IshikawaService) {}

  ngOnInit(): void {
    this.diagrams$ = this.statusFilter.valueChanges.pipe(
      startWith(this.statusFilter.value),
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(status =>
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

  badgeClass(status: IshikawaStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }
}
