import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { AdmService } from '../../adm.service';
import { AdmStatus, AdmType, AdmView, Art22Basis, BASIS_LABEL, TYPE_LABEL } from '../../adm.types';
import { AdmEditDialogComponent } from '../adm-edit-dialog/adm-edit-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-adm-detail',
  templateUrl: './adm-detail.component.html',
  styleUrls: ['./adm-detail.component.scss'],
  standalone: false
})
export class AdmDetailComponent implements OnInit {

  row$!: Observable<AdmView | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);
  readonly typeLabel = TYPE_LABEL;
  readonly basisLabel = BASIS_LABEL;

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: AdmService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.row$ = this.route.paramMap.pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('adm-')) {
          this.error$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loading$.next(false);
          return of(null);
        }
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              this.error$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return of(null);
            }),
            tap(() => this.loading$.next(false))
          ))
        );
      })
    );
  }

  edit(r: AdmView): void {
    this.dialog.open(AdmEditDialogComponent, {
      data: { row: r }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  activate(r: AdmView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: $localize`:@@automated-decisions.detail.activate-confirm-title:Activer la décision ?`,
              message: $localize`:@@automated-decisions.detail.activate-confirm-message:Mise en production. Les invariants Art. 22 seront re-vérifiés côté serveur.`,
              confirmLabel: $localize`:@@automated-decisions.detail.activate:Activer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: false }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.activate(r.id).subscribe({
        next: () => { this.snack.open($localize`:@@automated-decisions.detail.activated-toast:Décision activée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@automated-decisions.detail.activate-failed:Activation impossible.`)
      });
    });
  }

  deprecate(r: AdmView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: $localize`:@@automated-decisions.detail.deprecate-confirm-title:Déprécier la décision ?`,
              message: $localize`:@@automated-decisions.detail.deprecate-confirm-message:Le traitement reste en production mais est signalé pour remplacement.`,
              confirmLabel: $localize`:@@automated-decisions.detail.deprecate:Déprécier`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: false }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.deprecate(r.id).subscribe({
        next: () => { this.snack.open($localize`:@@automated-decisions.detail.deprecated-toast:Décision dépréciée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@automated-decisions.detail.deprecate-failed:Dépréciation impossible.`)
      });
    });
  }

  archive(r: AdmView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: $localize`:@@automated-decisions.detail.archive-confirm-title:Archiver la décision ?`,
              message: $localize`:@@automated-decisions.detail.archive-confirm-message:Action terminale. Le traitement n'est plus en production.`,
              confirmLabel: $localize`:@@automated-decisions.detail.archive:Archiver`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.archive(r.id).subscribe({
        next: () => { this.snack.open($localize`:@@automated-decisions.detail.archived-toast:Décision archivée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@automated-decisions.detail.archive-failed:Archivage impossible.`)
      });
    });
  }

  remove(r: AdmView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: $localize`:@@automated-decisions.detail.delete-confirm-title:Supprimer la décision ?`, message: $localize`:@@automated-decisions.detail.delete-confirm-message:Suppression définitive (DRAFT uniquement).`,
              confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(r.id).subscribe({
        next: () => { this.snack.open($localize`:@@automated-decisions.detail.deleted-toast:Décision supprimée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.router.navigate(['/automated-decisions']); },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
      });
    });
  }

  statusBadge(s: AdmStatus): string { return 'badge badge-' + s.toLowerCase(); }
  typeBadge(t: AdmType): string { return 'tpill tpill-' + t.toLowerCase(); }
  typeOf(t: AdmType): string { return this.typeLabel[t]; }
  basisOf(b: Art22Basis | null | undefined): string { return b ? this.basisLabel[b] : '—'; }
  isLegalEffect(t: AdmType): boolean { return t === 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT'; }

  canEdit(s: AdmStatus): boolean      { return s !== 'ARCHIVED'; }
  canActivate(s: AdmStatus): boolean  { return s === 'DRAFT'; }
  canDeprecate(s: AdmStatus): boolean { return s === 'ACTIVE'; }
  canArchive(s: AdmStatus): boolean   { return s === 'DEPRECATED'; }
  canDelete(s: AdmStatus): boolean    { return s === 'DRAFT'; }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[adm-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
