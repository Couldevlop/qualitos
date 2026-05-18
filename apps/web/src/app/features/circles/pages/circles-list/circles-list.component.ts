import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { CirclesService } from '../../circles.service';
import { CircleResponse, CircleStatus } from '../../circles.types';
import { CirclesCreateDialogComponent } from '../circles-create-dialog/circles-create-dialog.component';

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

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: CirclesService,
    private readonly dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.circles$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      tap(() => this.loading$.next(true)),
      switchMap(([s]) => this.svc.listCircles(0, 50, s || undefined).pipe(
        catchError(() => []),
        finalize(() => this.loading$.next(false))
      )),
      map(p => Array.isArray(p) ? [] : p.content)
    );
  }

  openCreate(): void {
    const ref = this.dialog.open(CirclesCreateDialogComponent, {
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(created => {
      if (created) this.refresh$.next();
    });
  }

  badge(s: CircleStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
