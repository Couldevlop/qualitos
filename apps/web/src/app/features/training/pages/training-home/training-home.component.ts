import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { TrainingService } from '../../training.service';
import {
  EnrollmentResponse,
  EnrollmentStatus,
  PathResponse,
  SkillResponse,
  TrainingPathStatus
} from '../../training.types';
import { TrainingEnrollDialogComponent } from '../training-enroll-dialog/training-enroll-dialog.component';
import { TrainingPathDialogComponent } from '../training-path-dialog/training-path-dialog.component';
import { TrainingSkillDialogComponent } from '../training-skill-dialog/training-skill-dialog.component';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

@Component({
  selector: 'qos-training-home',
  templateUrl: './training-home.component.html',
  styleUrls: ['./training-home.component.scss'],
  standalone: false
})
export class TrainingHomeComponent implements OnInit {

  // Paths tab
  readonly pathColumns = ['code', 'name', 'targetRole', 'durationHours', 'status'];
  readonly pathStatuses: TrainingPathStatus[] = ['DRAFT', 'ACTIVE', 'ARCHIVED'];
  readonly pathStatusFilter = new FormControl<TrainingPathStatus | ''>('ACTIVE');
  paths$!: Observable<PathResponse[]>;
  pathTotal = 0;
  pathIndex = 0;
  pathSize  = 20;

  // Skills tab
  readonly skillColumns = ['code', 'name', 'category', 'description'];
  readonly skillCategoryFilter = new FormControl<string>('');
  skills$!: Observable<SkillResponse[]>;
  skillTotal = 0;
  skillIndex = 0;
  skillSize  = 20;

  // Enrollments tab — defaults to current user
  readonly enrollColumns = ['pathCode', 'status', 'progressPct', 'enrolledOn', 'finalScore', 'actions'];
  enrollments$!: Observable<EnrollmentResponse[]>;
  pathLookup: Record<string, PathResponse> = {};
  enrollTotal = 0;
  enrollIndex = 0;
  enrollSize  = 20;

  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  pageSizeOptions = PAGE_SIZE_OPTIONS;

  private readonly refreshPaths$    = new BehaviorSubject<void>(undefined);
  private readonly refreshSkills$   = new BehaviorSubject<void>(undefined);
  private readonly refreshEnrolls$  = new BehaviorSubject<void>(undefined);
  private readonly pathPage$    = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });
  private readonly skillPage$   = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });
  private readonly enrollPage$  = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly svc: TrainingService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    // Paths
    this.paths$ = combineLatest([
      this.pathStatusFilter.valueChanges.pipe(startWith(this.pathStatusFilter.value)),
      this.pathPage$,
      this.refreshPaths$
    ]).pipe(
      tap(() => this.error$.next(null)),
      switchMap(([status, p]) =>
        this.svc.listPaths(p.index, p.size, status || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[training:paths] failed', err?.status, err?.error?.title);
            this.error$.next(safeErrorMessage(err, $localize`:@@training.home.paths-error:Erreur de chargement des parcours.`));
            return of(null);
          })
        )
      ),
      map(page => {
        if (!page) return [];
        this.pathTotal = page.totalElements;
        page.content.forEach(p => (this.pathLookup[p.id] = p));
        return page.content;
      })
    );

    // Skills
    this.skills$ = combineLatest([
      this.skillCategoryFilter.valueChanges.pipe(startWith(this.skillCategoryFilter.value)),
      this.skillPage$,
      this.refreshSkills$
    ]).pipe(
      switchMap(([cat, p]) =>
        this.svc.listSkills(p.index, p.size, cat?.trim() || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[training:skills] failed', err?.status, err?.error?.title);
            this.error$.next(safeErrorMessage(err, $localize`:@@training.home.skills-error:Erreur de chargement des compétences.`));
            return of(null);
          })
        )
      ),
      map(page => {
        if (!page) return [];
        this.skillTotal = page.totalElements;
        return page.content;
      })
    );

    // Enrollments (current user)
    this.enrollments$ = combineLatest([this.enrollPage$, this.refreshEnrolls$]).pipe(
      switchMap(([p]) => {
        const userId = this.auth.snapshot()?.userId;
        if (!userId) return of([]);
        return this.svc.listEnrollments(p.index, p.size, { userId }).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[training:enroll] failed', err?.status, err?.error?.title);
            this.error$.next(safeErrorMessage(err, $localize`:@@training.home.enrollments-error:Erreur de chargement des inscriptions.`));
            return of(null);
          }),
          map(page => {
            if (!page || Array.isArray(page)) return [];
            this.enrollTotal = page.totalElements;
            return page.content;
          })
        );
      })
    );
  }

  // ---- Paginators ----
  onPathPage(e: PageEvent): void {
    this.pathIndex = Math.max(0, e.pageIndex);
    this.pathSize  = Math.min(MAX_PAGE_SIZE, Math.max(1, e.pageSize));
    this.pathPage$.next({ index: this.pathIndex, size: this.pathSize });
  }
  onSkillPage(e: PageEvent): void {
    this.skillIndex = Math.max(0, e.pageIndex);
    this.skillSize  = Math.min(MAX_PAGE_SIZE, Math.max(1, e.pageSize));
    this.skillPage$.next({ index: this.skillIndex, size: this.skillSize });
  }
  onEnrollPage(e: PageEvent): void {
    this.enrollIndex = Math.max(0, e.pageIndex);
    this.enrollSize  = Math.min(MAX_PAGE_SIZE, Math.max(1, e.pageSize));
    this.enrollPage$.next({ index: this.enrollIndex, size: this.enrollSize });
  }

  // ---- Actions ----
  openPathCreate(): void {
    const ref = this.dialog.open(TrainingPathDialogComponent, {
      panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(c => { if (c) this.refreshPaths$.next(); });
  }
  openSkillCreate(): void {
    const ref = this.dialog.open(TrainingSkillDialogComponent, {
      panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(s => { if (s) this.refreshSkills$.next(); });
  }
  openEnroll(): void {
    const ref = this.dialog.open(TrainingEnrollDialogComponent, {
      panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(e => { if (e) this.refreshEnrolls$.next(); });
  }

  openPath(p: PathResponse): void { this.router.navigate(['/training/paths', p.id]); }

  startEnrollment(e: EnrollmentResponse): void {
    this.svc.startEnrollment(e.id).subscribe(() => this.refreshEnrolls$.next());
  }
  cancelEnrollment(e: EnrollmentResponse): void {
    this.svc.cancelEnrollment(e.id).subscribe(() => this.refreshEnrolls$.next());
  }

  pathName(pathId: string): string { return this.pathLookup[pathId]?.code ?? pathId.slice(0, 8); }

  pathStatusBadge(s: TrainingPathStatus): string { return 'pbadge pbadge-' + s.toLowerCase(); }
  enrollStatusBadge(s: EnrollmentStatus): string { return 'ebadge ebadge-' + s.toLowerCase(); }
  progressColor(p: number): string {
    if (p >= 80) return 'primary';
    return 'accent';
  }
}
