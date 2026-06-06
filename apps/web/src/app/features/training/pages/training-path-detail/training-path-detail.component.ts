import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, forkJoin, of } from 'rxjs';
import { catchError, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { TrainingService } from '../../training.service';
import {
  PathResponse,
  SkillRequirementResponse,
  SkillResponse,
  TrainingPathStatus
} from '../../training.types';
import { TrainingPathDialogComponent } from '../training-path-dialog/training-path-dialog.component';
import { TrainingRequirementDialogComponent } from '../training-requirement-dialog/training-requirement-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const LEVEL_LABELS = ['NONE', 'AWARE', 'PRACTITIONER', 'COMPETENT', 'EXPERT'];

@Component({
  selector: 'qos-training-path-detail',
  templateUrl: './training-path-detail.component.html',
  styleUrls: ['./training-path-detail.component.scss'],
  standalone: false
})
export class TrainingPathDetailComponent implements OnInit {

  readonly reqColumns = ['skillCode', 'skillName', 'category', 'targetLevel', 'actions'];

  path$!: Observable<PathResponse | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  requirements: SkillRequirementResponse[] = [];
  skillsById: Record<string, SkillResponse> = {};

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private pathId = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: TrainingService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.path$ = this.route.paramMap.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('path-')) {
          this.errorState$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loadingState$.next(false);
          return of(null);
        }
        this.pathId = id;
        return this.refresh$.pipe(
          switchMap(() => forkJoin({
            path: this.svc.getPath(id).pipe(catchError(err => {
              this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return of(null);
            })),
            requirements: this.svc.listRequirements(id).pipe(catchError(() => of([]))),
            skillsPage: this.svc.listSkills(0, 200).pipe(catchError(() => of({
              content: [], totalElements: 0, totalPages: 0, number: 0, size: 0
            })))
          })),
          tap(({ requirements, skillsPage }) => {
            this.loadingState$.next(false);
            this.requirements = requirements;
            this.skillsById = {};
            skillsPage.content.forEach(s => (this.skillsById[s.id] = s));
          }),
          switchMap(({ path }) => of(path))
        );
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  openEdit(p: PathResponse): void {
    const ref = this.dialog.open(TrainingPathDialogComponent, {
      data: { path: p }, autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  activate(p: PathResponse): void {
    this.svc.activatePath(p.id).subscribe({
      next: () => { this.snack.open($localize`:@@training.path-detail.activated:Parcours activé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@training.path-detail.activate-error:Activation impossible.`)
    });
  }
  reopen(p: PathResponse): void {
    this.svc.reopenPath(p.id).subscribe({
      next: () => { this.snack.open($localize`:@@training.path-detail.reopened:Parcours rouvert (DRAFT).`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@training.path-detail.reopen-error:Réouverture impossible.`)
    });
  }
  archive(p: PathResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@training.path-detail.archive-confirm-title:Archiver le parcours ?`,
        message: $localize`:@@training.path-detail.archive-confirm-message:« ${p.name}:name: » sera marqué ARCHIVED. Les inscriptions en cours ne sont pas annulées.`,
        confirmLabel: $localize`:@@training.path-detail.archive:Archiver`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.archivePath(p.id).subscribe({
        next: () => { this.snack.open($localize`:@@training.path-detail.archived:Parcours archivé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@training.path-detail.archive-error:Archivage impossible.`)
      });
    });
  }
  remove(p: PathResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@training.path-detail.delete-confirm-title:Supprimer le parcours ?`,
        message: $localize`:@@training.path-detail.delete-confirm-message:Suppression définitive de « ${p.name}:name: » et de toutes ses exigences.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.deletePath(p.id).subscribe({
        next: () => { this.snack.open($localize`:@@training.path-detail.deleted:Parcours supprimé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.router.navigate(['/training']); },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
      });
    });
  }

  openAttach(p: PathResponse): void {
    const ref = this.dialog.open(TrainingRequirementDialogComponent, {
      data: { pathId: p.id, excludeSkillIds: this.requirements.map(r => r.skillId) },
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(r => { if (r) this.refresh$.next(); });
  }

  detach(p: PathResponse, r: SkillRequirementResponse): void {
    const skill = this.skillsById[r.skillId];
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@training.path-detail.detach-confirm-title:Retirer l'exigence ?`,
        message: $localize`:@@training.path-detail.detach-confirm-message:La compétence « ${skill?.name ?? r.skillId}:skill: » ne sera plus requise pour ce parcours.`,
        confirmLabel: $localize`:@@training.path-detail.detach-tooltip:Retirer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.detachRequirement(p.id, r.skillId).subscribe({
        next: () => { this.requirements = this.requirements.filter(x => x.skillId !== r.skillId); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@training.path-detail.detach-error:Retrait impossible.`)
      });
    });
  }

  skillCode(r: SkillRequirementResponse): string { return this.skillsById[r.skillId]?.code ?? r.skillId.slice(0, 8); }
  skillName(r: SkillRequirementResponse): string { return this.skillsById[r.skillId]?.name ?? '—'; }
  skillCategory(r: SkillRequirementResponse): string { return this.skillsById[r.skillId]?.category ?? '—'; }
  levelLabel(n: number): string { return LEVEL_LABELS[n] ?? String(n); }
  statusBadge(s: TrainingPathStatus): string { return 'pbadge pbadge-' + s.toLowerCase(); }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[training-path-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
