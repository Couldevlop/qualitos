import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
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

  /** Ce qui manque à la phase en cours (fiche enregistrée) : tant qu'il en reste, on n'avance pas. */
  missing: string[] = [];
  readonly advanceBlockedHint =
    $localize`:@@dmaic.detail.advance-blocked:Répondez à toutes les questions de la phase et enregistrez la fiche pour passer à la suivante.`;

  project$!: Observable<DmaicProjectResponse | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  measures: MeasureResponse[]      = [];
  assignments: AssignmentResponse[] = [];
  capability: CapabilityResponse | null = null;

  readonly measureColumns    = ['value', 'operatorId', 'recordedAt', 'sourceRef'];
  readonly assignmentColumns = ['deviceCode', 'deviceName', 'deviceType', 'status', 'note'];

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private projectId = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: DmaicService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.project$ = this.route.paramMap.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        // OWASP A03 — validate path param shape before hitting backend (mock allows demo ids).
        if (!UUID_REGEX.test(id) && !id.startsWith('dmaic-')) {
          this.errorState$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loadingState$.next(false);
          return of(null);
        }
        this.projectId = id;
        return this.refresh$.pipe(
          switchMap(() => this.svc.getProject(id).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[dmaic-detail] getProject failed', err?.status, err?.error?.title);
              this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return of(null);
            })
          ))
        );
      }),
      tap(p => {
        this.loadingState$.next(false);
        if (p) {
          this.loadMeasures();
          this.loadAssignments();
          this.refreshCapability();
        }
      }),
      map(p => p),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  onMissing(list: string[]): void {
    this.missing = list;
  }

  /** Mesures du projet, dans l'ordre de relevé : celui du calcul de capabilité. */
  private loadMeasures(): void {
    this.svc.listMeasures(this.projectId).subscribe({
      next: list => (this.measures = list),
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[dmaic-detail] measures failed', err?.status, err?.error?.title);
        this.measures = [];
      }
    });
  }

  private loadAssignments(): void {
    this.svc.listAssignments(this.projectId).subscribe({
      next: list => (this.assignments = list),
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[dmaic-detail] assignments failed', err?.status, err?.error?.title);
        this.assignments = [];
      }
    });
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
      next: () => { this.snack.open($localize`:@@dmaic.detail.phase-advanced:Phase avancée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@dmaic.detail.advance-failed:Avance impossible.`)
    });
  }

  hold(p: DmaicProjectResponse): void {
    this.svc.hold(p.id).subscribe({
      next: () => { this.snack.open($localize`:@@dmaic.detail.project-held:Projet en pause.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@dmaic.detail.hold-failed:Mise en pause impossible.`)
    });
  }

  resume(p: DmaicProjectResponse): void {
    this.svc.resume(p.id).subscribe({
      next: () => { this.snack.open($localize`:@@dmaic.detail.project-resumed:Projet repris.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@dmaic.detail.resume-failed:Reprise impossible.`)
    });
  }

  cancel(p: DmaicProjectResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@dmaic.detail.cancel-confirm-title:Annuler le projet ?`,
        message: 'Le projet « ' + p.title + ' » sera marqué comme annulé. Cette action est irréversible.',
        confirmLabel: $localize`:@@dmaic.detail.cancel-confirm-label:Annuler le projet`,
        cancelLabel: $localize`:@@dmaic.detail.keep-label:Conserver`,
        danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.cancel(p.id).subscribe({
        next: () => { this.snack.open($localize`:@@dmaic.detail.project-cancelled:Projet annulé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@dmaic.detail.cancel-failed:Annulation impossible.`)
      });
    });
  }

  remove(p: DmaicProjectResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@dmaic.detail.delete-confirm-title:Supprimer le projet ?`,
        message: 'Suppression définitive de « ' + p.title + ' » et de ses mesures + assignations.',
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.deleteProject(p.id).subscribe({
        next: () => {
          this.snack.open($localize`:@@dmaic.detail.project-deleted:Projet supprimé.`, $localize`:@@common.ok:OK`, { duration: 2200 });
          this.router.navigate(['/dmaic']);
        },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
      });
    });
  }

  openAddMeasure(): void {
    const ref = this.dialog.open(DmaicMeasureDialogComponent, {
      data: { projectId: this.projectId },
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    // Le rechargement du projet relit aussi les mesures et la capabilité.
    ref.afterClosed().subscribe(m => { if (m) this.refresh$.next(); });
  }

  openAddPokaYoke(): void {
    const ref = this.dialog.open(DmaicPokaYokeDialogComponent, {
      data: { projectId: this.projectId },
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(a => { if (a) this.refresh$.next(); });
  }

  removeMeasure(m: MeasureResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@dmaic.detail.delete-measure-title:Supprimer la mesure ?`,
        message: $localize`:@@dmaic.detail.delete-measure-message:Cette mesure sera retirée du calcul de capabilité.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.deleteMeasure(this.projectId, m.id).subscribe({
        next: () => {
          this.measures = this.measures.filter(x => x.id !== m.id);
          this.refresh$.next();
        },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
      });
    });
  }

  removeAssignment(a: AssignmentResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@dmaic.detail.delete-assignment-title:Supprimer l'assignation ?`,
        message: 'Le Poka-Yoke « ' + a.deviceCode + ' » sera détaché du projet.',
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.deleteAssignment(this.projectId, a.id).subscribe({
        next: () => {
          this.assignments = this.assignments.filter(x => x.id !== a.id);
          this.refresh$.next();
        },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
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
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
