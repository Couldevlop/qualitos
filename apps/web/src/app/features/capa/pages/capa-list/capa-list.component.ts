import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { BehaviorSubject, Observable } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { CapaService } from '../../capa.service';
import { CapaCaseResponse, CapaCriticity, CapaStatus } from '../../capa.types';

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

  constructor(private readonly svc: CapaService) {}

  ngOnInit(): void {
    this.cases$ = this.statusFilter.valueChanges.pipe(
      startWith(this.statusFilter.value),
      tap(() => this.loading$.next(true)),
      switchMap(status =>
        this.svc.listCases(0, 50, status || undefined).pipe(
          catchError(() => []),
          finalize(() => this.loading$.next(false))
        )
      ),
      map(page => (Array.isArray(page) ? [] : page.content))
    );
  }

  statusBadge(s: CapaStatus): string { return 'badge badge-' + s.toLowerCase(); }
  criticityBadge(c: CapaCriticity): string { return 'crit crit-' + c.toLowerCase(); }
}
