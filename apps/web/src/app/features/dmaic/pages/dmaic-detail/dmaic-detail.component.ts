import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, finalize, map, switchMap, tap } from 'rxjs/operators';

import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { DmaicService } from '../../dmaic.service';
import {
  AssignmentResponse,
  CapabilityResponse,
  DmaicPhase,
  DmaicProjectResponse,
  DmaicStatus,
  MeasureResponse,
  PokaYokeAssignmentStatus
} from '../../dmaic.types';
import { DmaicEditDialogComponent } from '../dmaic-edit-dialog/dmaic-edit-dialog.component';
import { DmaicMeasureDialogComponent } from '../dmaic-measure-dialog/dmaic-measure-dialog.component';
import { DmaicPokaYokeDialogComponent } from '../dmaic-pokayoke-dialog/dmaic-pokayoke-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-dmaic-detail',
  templateUrl: './dmaic-detail.component.html',
  styleUrls: ['./dmaic-detail.component.scss'],
  standalone: false
})
export class DmaicDetailComponent implements OnInit {

  readonly phases: DmaicPhase[] = ['DEFINE', 'MEASURE', 'ANALYZE', 'IMPROVE', 'CONTROL'];

  project$!: Observable<DmaicProjectResponse | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  measures: MeasureResponse[]      = [];
  assignments: AssignmentResponse[] = [];
  capability: CapabilityResponse | null = null;

  readonly measureColumns    = ['value', 'subgroupId', 'recordedAt', 'sourceRef'];
  readonly assignmentColumns = ['deviceCode', 'deviceName', 'deviceType', 'status', 'note'];

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private projectId = '';
  // Note: mock list helpers — backend has no list endpoints for these (read-after-write only).
  // For real API we rely on the project response counts; UI shows last-added items via local refresh.
  private addedMeasures: MeasureResponse[]      = [];
  private addedAssignments: AssignmentResponse[] = [];

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: DmaicService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.project$ = this.route.paramMap.pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        // OWASP A03 — validate path param shape before hitting backend (mock allows demo ids).
        if (!UUID_REGEX.test(id) && !id.startsWith('dmaic-')) {
          this.error$.next('Identifiant invalide.');
          this.loading$.next(false);
          return of(null);
        }
        this.projectId = id;
        return this.refresh$.pipe(
          switchMap(() => this.svc.getProject(id).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[dmaic-detail] getProject failed', err?.status, err?.error?.title);
              this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
              return of(null);
            })
          ))
        );
      }),
      tap(p => {
        this.loading$.next(false);
        if (p) {
          this.measures    = this.svc.listMeasures(p.id);
          this.assignments = this.svc.listAssignments(p.id);
          this.refreshCapability();
        }
      }),
      map(p => p)
    );
  }

  refreshCapability(): void {
    if (!this.projectId) return;
    this.svc.capability(this.projectId).subscribe({
      next: c => (this.capability = c),
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[dmaic-detail] capability failed', err?.status, err?.error?.title);
        this.capability = null;
      }
    });
  }

  openEdit(p: DmaicProjectResponse): void {
    const ref = this.dialog.open(DmaicEditDialogComponent, {
      data: { project: p }, autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  advance(p: DmaicProjectResponse): void {
    this.svc.advance(p.id).subscribe({
      next: () => { this.snack.open('Phase avancée.', 'OK', { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, 'Avance impossible.')
    });
  }

  hold(p: DmaicProjectResponse): void {
    this.svc.hold(p.id).subscribe({
      next: () => { this.snack.open('Projet en pause.', 'OK', { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, 'Mise en pause impossible.')
    });
  }

  resume(p: DmaicProjectResponse): void {
    this.svc.resume(p.id).subscribe({
      next: () => { this.snack.open('Projet repris.', 'OK', { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, 'Reprise impossible.')
    });
  }

  cancel(p: DmaicProjectResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Annuler le projet ?',
        message: 'Le projet « ' + p.title + ' » sera marqué comme annulé. Cette action est irréversible.',
        confirmLabel: 'Annuler le projet',
        cancelLabel: 'Conserver',
        danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.cancel(p.id).subscribe({
        next: () => { this.snack.open('Projet annulé.', 'OK', { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, 'Annulation impossible.')
      });
    });
  }

  remove(p: DmaicProjectResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Supprimer le projet ?',
        message: 'Suppression définitive de « ' + p.title + ' » et de ses mesures + assignations.',
        confirmLabel: 'Supprimer',
        cancelLabel: 'Annuler',
        danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.deleteProject(p.id).subscribe({
        next: () => {
          this.snack.open('Projet supprimé.', 'OK', { duration: 2200 });
          this.router.navigate(['/dmaic']);
        },
        error: err => this.fail(err, 'Suppression impossible.')
      });
    });
  }

  openAddMeasure(): void {
    const ref = this.dialog.open(DmaicMeasureDialogComponent, {
      data: { projectId: this.projectId },
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(m => {
      if (m) {
        this.addedMeasures = [m, ...this.addedMeasures];
        this.measures = [m, ...this.measures];
        this.refresh$.next();
      }
    });
  }

  openAddPokaYoke(): void {
    const ref = this.dialog.open(DmaicPokaYokeDialogComponent, {
      data: { projectId: this.projectId },
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(a => {
      if (a) {
        this.addedAssignments = [a, ...this.addedAssignments];
        this.assignments = [a, ...this.assignments];
        this.refresh$.next();
      }
    });
  }

  removeMeasure(m: MeasureResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Supprimer la mesure ?',
        message: 'Cette mesure sera retirée du calcul de capabilité.',
        confirmLabel: 'Supprimer', cancelLabel: 'Annuler', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.deleteMeasure(this.projectId, m.id).subscribe({
        next: () => {
          this.measures = this.measures.filter(x => x.id !== m.id);
          this.refresh$.next();
        },
        error: err => this.fail(err, 'Suppression impossible.')
      });
    });
  }

  removeAssignment(a: AssignmentResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Supprimer l\'assignation ?',
        message: 'Le Poka-Yoke « ' + a.deviceCode + ' » sera détaché du projet.',
        confirmLabel: 'Supprimer', cancelLabel: 'Annuler', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.deleteAssignment(this.projectId, a.id).subscribe({
        next: () => {
          this.assignments = this.assignments.filter(x => x.id !== a.id);
          this.refresh$.next();
        },
        error: err => this.fail(err, 'Suppression impossible.')
      });
    });
  }

  phaseIndex(phase: DmaicPhase): number { return this.phases.indexOf(phase); }
  phaseBadge(p: DmaicPhase): string  { return 'phase phase-' + p.toLowerCase(); }
  statusBadge(s: DmaicStatus): string { return 'badge badge-' + s.toLowerCase(); }
  assignmentBadge(s: PokaYokeAssignmentStatus): string {
    return 'assign assign-' + s.toLowerCase();
  }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[dmaic-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
