import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { BehaviorSubject, Observable } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { CirclesService } from '../../circles.service';
import { CircleResponse, CircleStatus } from '../../circles.types';

@Component({
  selector: 'qos-circles-list',
  templateUrl: './circles-list.component.html',
  styleUrls: ['./circles-list.component.scss'],
  standalone: false
})
export class CirclesListComponent implements OnInit {

  readonly displayedColumns = ['name', 'status', 'members', 'meetings', 'proposals', 'updatedAt'];
  readonly statusFilter = new FormControl<CircleStatus | ''>('');
  readonly statuses: CircleStatus[] = ['ACTIVE', 'PAUSED', 'ARCHIVED'];

  circles$!: Observable<CircleResponse[]>;
  loading$ = new BehaviorSubject<boolean>(false);

  constructor(private readonly svc: CirclesService) {}

  ngOnInit(): void {
    this.circles$ = this.statusFilter.valueChanges.pipe(
      startWith(this.statusFilter.value),
      tap(() => this.loading$.next(true)),
      switchMap(s => this.svc.listCircles(0, 50, s || undefined).pipe(
        catchError(() => []),
        finalize(() => this.loading$.next(false))
      )),
      map(p => Array.isArray(p) ? [] : p.content)
    );
  }

  badge(s: CircleStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
