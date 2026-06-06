import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { RopaService } from '../../ropa.service';
import {
  LawfulBasis,
  ProcessingActivityStatus,
  ProcessingActivityView
} from '../../ropa.types';
import { RopaDialogComponent } from '../ropa-dialog/ropa-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-ropa-detail',
  templateUrl: './ropa-detail.component.html',
  styleUrls: ['./ropa-detail.component.scss'],
  standalone: false
})
export class RopaDetailComponent implements OnInit {

  activity$!: Observable<ProcessingActivityView | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  readonly editTooltipImmutable = $localize`:@@ropa.detail.edit-tooltip-immutable:Une fiche ACTIVE est immutable`;
  readonly editTooltipDraft = $localize`:@@ropa.detail.edit-tooltip-draft:Modifier le brouillon`;
  readonly deleteTooltipActive = $localize`:@@ropa.detail.delete-tooltip-active:Impossible — archivez d'abord`;
  readonly deleteTooltipDefault = $localize`:@@common.delete:Supprimer`;
  readonly noJustificationLabel = $localize`:@@ropa.detail.no-justification:Aucune justification — à compléter.`;

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: RopaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.activity$ = this.route.paramMap.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        // OWASP A03 — UUID v4 regex on path id (mock-id fallback for demo data).
        if (!UUID_REGEX.test(id) && !id.startsWith('ropa-')) {
          this.errorState$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loadingState$.next(false);
          return of(null);
        }
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[ropa-detail] failed', err?.status, err?.error?.title);
              this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return of(null);
            }),
            tap(() => this.loadingState$.next(false))
          ))
        );
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  openEdit(a: ProcessingActivityView): void {
    // OWASP A04 — ACTIVE is immutable (matches backend invariant).
    if (a.status !== 'DRAFT') {
      this.snack.open($localize`:@@ropa.detail.only-draft-editable:Seul un brouillon peut être édité. Archivez pour modifier ensuite.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(RopaDialogComponent, {
      data: { activity: a }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  activate(a: ProcessingActivityView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@ropa.detail.activate-confirm-title:Activer cette activité de traitement ?`,
        message: $localize`:@@ropa.detail.activate-confirm-message:Une fois ACTIVE, la fiche devient immutable pour préserver la traçabilité (Art. 30 RGPD). Toute évolution future devra passer par un nouveau brouillon.`,
        confirmLabel: $localize`:@@ropa.detail.activate:Activer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: false
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.activate(a.id).subscribe({
        next: () => { this.snack.open($localize`:@@ropa.detail.activated:Activité activée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@ropa.detail.activate-error:Activation impossible.`)
      });
    });
  }

  archive(a: ProcessingActivityView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@ropa.detail.archive-confirm-title:Archiver cette activité ?`,
        message: $localize`:@@ropa.detail.archive-confirm-message:« ${a.name}:name: » sera marquée ARCHIVED. La fiche reste consultable pour les contrôles CNIL/EDPB.`,
        confirmLabel: $localize`:@@ropa.detail.archive:Archiver`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.archive(a.id).subscribe({
        next: () => { this.snack.open($localize`:@@ropa.detail.archived:Activité archivée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@ropa.detail.archive-error:Archivage impossible.`)
      });
    });
  }

  remove(a: ProcessingActivityView): void {
    // OWASP A04 — backend refuse de supprimer ACTIVE ; on coupe en amont aussi.
    if (a.status === 'ACTIVE') {
      this.snack.open($localize`:@@ropa.detail.cannot-delete-active:Impossible de supprimer une activité ACTIVE — archivez-la d'abord.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@ropa.detail.delete-confirm-title:Supprimer cette activité ?`,
        message: $localize`:@@ropa.detail.delete-confirm-message:Suppression définitive de « ${a.name}:name: ». Cette action est irréversible et ne devrait être utilisée que pour un brouillon créé par erreur.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(a.id).subscribe({
        next: () => { this.snack.open($localize`:@@ropa.detail.deleted:Activité supprimée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.router.navigate(['/ropa']); },
        error: err => this.fail(err, $localize`:@@ropa.detail.delete-error:Suppression impossible.`)
      });
    });
  }

  basisLabel(b: LawfulBasis): string {
    return ({
      CONSENT: $localize`:@@ropa.basis-art.consent:Consentement (Art. 6.1.a)`,
      CONTRACT: $localize`:@@ropa.basis-art.contract:Exécution d'un contrat (Art. 6.1.b)`,
      LEGAL_OBLIGATION: $localize`:@@ropa.basis-art.legal-obligation:Obligation légale (Art. 6.1.c)`,
      VITAL_INTERESTS: $localize`:@@ropa.basis-art.vital-interests:Intérêts vitaux (Art. 6.1.d)`,
      PUBLIC_TASK: $localize`:@@ropa.basis-art.public-task:Mission de service public (Art. 6.1.e)`,
      LEGITIMATE_INTERESTS: $localize`:@@ropa.basis-art.legitimate-interests:Intérêt légitime (Art. 6.1.f)`
    })[b];
  }
  basisBadge(b: LawfulBasis): string  { return 'lb lb-' + b.toLowerCase(); }
  statusBadge(s: ProcessingActivityStatus): string { return 'badge badge-' + s.toLowerCase(); }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[ropa-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
