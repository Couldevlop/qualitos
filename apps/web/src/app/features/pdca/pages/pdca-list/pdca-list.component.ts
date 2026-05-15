import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { PdcaService } from '../../pdca.service';
import { PdcaCycleResponse, PdcaStatus } from '../../pdca.types';

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

  constructor(private readonly pdca: PdcaService) {}

  ngOnInit(): void {
    this.cycles$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value))
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

  statusBadgeClass(status: PdcaStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }
}
