import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { TransfersService } from '../../transfers.service';
import { TransferMechanism, TransferStatus, TransferView } from '../../transfers.types';
import { TrxDialogComponent } from '../trx-dialog/trx-dialog.component';
import { TrxReasonDialogComponent } from '../trx-reason-dialog/trx-reason-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-trx-detail',
  templateUrl: './trx-detail.component.html',
  styleUrls: ['./trx-detail.component.scss'],
  standalone: false
})
export class TrxDetailComponent implements OnInit {

  transfer$!: Observable<TransferView | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: TransfersService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.transfer$ = this.route.paramMap.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('cbt-')) {
          this.errorState$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loadingState$.next(false);
          return of(null);
        }
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[trx-detail] failed', err?.status, err?.error?.title);
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

  openEdit(t: TransferView): void {
    if (t.status === 'TERMINATED') {
      this.snack.open($localize`:@@transfers.detail.edit-terminated-blocked:Un transfert TERMINATED ne peut plus être modifié.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(TrxDialogComponent, {
      data: { transfer: t }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  activate(t: TransferView): void {
    this.svc.activate(t.id).subscribe({
      next: () => { this.snack.open($localize`:@@transfers.detail.activated:Transfert activé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@transfers.detail.activate-failed:Activation impossible.`)
    });
  }

  suspend(t: TransferView): void {
    const ref = this.dialog.open(TrxReasonDialogComponent, {
      data: { transferId: t.id, mode: 'SUSPEND' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  terminate(t: TransferView): void {
    const ref = this.dialog.open(TrxReasonDialogComponent, {
      data: { transferId: t.id, mode: 'TERMINATE' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  remove(t: TransferView): void {
    if (t.status !== 'DRAFT') {
      this.snack.open($localize`:@@transfers.detail.only-draft-deletable:Seul un brouillon peut être supprimé.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@transfers.detail.delete-confirm-title:Supprimer le brouillon ?`,
        message: 'Suppression définitive de « ' + t.reference + ' ».',
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(t.id).subscribe({
        next: () => { this.snack.open($localize`:@@transfers.detail.deleted:Brouillon supprimé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.router.navigate(['/cross-border']); },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
      });
    });
  }

  mechLabel(m: TransferMechanism): string {
    return ({
      ADEQUACY_DECISION: $localize`:@@transfers.mech.adequacy:Décision d'adéquation (Art. 45)`,
      STANDARD_CONTRACTUAL_CLAUSES: $localize`:@@transfers.mech.scc:Clauses contractuelles types — SCC (Art. 46.2)`,
      BINDING_CORPORATE_RULES: $localize`:@@transfers.mech.bcr:Règles d'entreprise contraignantes (Art. 47)`,
      CODE_OF_CONDUCT: $localize`:@@transfers.mech.coc:Code de conduite (Art. 46.2.e)`,
      CERTIFICATION: $localize`:@@transfers.mech.certification:Certification (Art. 46.2.f)`,
      DEROGATION_ART49: $localize`:@@transfers.mech.derogation:Dérogation Art. 49`
    })[m];
  }
  mechBadge(m: TransferMechanism): string { return 'mech mech-' + m.toLowerCase(); }
  statusBadge(s: TransferStatus): string { return 'badge badge-' + s.toLowerCase(); }
  isDerogation(m: TransferMechanism): boolean { return m === 'DEROGATION_ART49'; }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[trx-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
