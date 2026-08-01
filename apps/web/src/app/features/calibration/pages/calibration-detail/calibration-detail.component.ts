import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { CalibrationService } from '../../calibration.service';
import {
  CalibrationResult, EquipmentDetail, EquipmentStatus, MsaResult, MsaType
} from '../../calibration.types';
import {
  CalibrationEquipmentDialogComponent
} from '../calibration-equipment-dialog/calibration-equipment-dialog.component';
import {
  CalibrationMsaDialogComponent
} from '../calibration-msa-dialog/calibration-msa-dialog.component';
import {
  CalibrationPlanDialogComponent
} from '../calibration-plan-dialog/calibration-plan-dialog.component';
import {
  CalibrationRecordDialogComponent
} from '../calibration-record-dialog/calibration-record-dialog.component';

/** Rôles autorisés à supprimer une ressource qualité (SecurityConfig, règle DELETE). */
const DELETE_ROLES = ['quality_manager', 'admin_tenant', 'admin', 'super_admin'];

/**
 * Calibration & équipements — fiche d'un instrument (§4.10).
 *
 * Rassemble les trois objets liés du module : l'équipement, son plan de calibration
 * et ses enregistrements (plus les études MSA). Les actions proposées sont
 * exactement celles que le serveur accepte dans l'état courant — un bouton affiché
 * est un bouton qui aboutit :
 *  - pas d'enregistrement sur un équipement qui n'est pas EN SERVICE ;
 *  - pas de retour en service d'un instrument critique sans dernière calibration
 *    conforme ;
 *  - pas de suppression du plan d'un instrument critique ;
 *  - rien du tout sur un instrument réformé (état terminal côté serveur).
 */
@Component({
  selector: 'qos-calibration-detail',
  templateUrl: './calibration-detail.component.html',
  styleUrls: ['./calibration-detail.component.scss'],
  standalone: false
})
export class CalibrationDetailComponent implements OnInit {

  readonly recordColumns =
    ['performedOn', 'result', 'performedBy', 'certificate', 'nextDueOverride', 'measurements'];
  readonly msaColumns =
    ['performedOn', 'type', 'studyValue', 'threshold', 'result', 'notes'];

  detail$!: Observable<EquipmentDetail | null>;

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  /** Action en cours : évite le double-clic sur les transitions d'état. */
  pending = false;

  /** Les suppressions sont réservées au manager qualité ou plus (SecurityConfig). */
  readonly canDelete: boolean;

  /** Explique le bouton « Remettre en service » désactivé au lieu de le masquer. */
  readonly blockedReturnHint = $localize`:@@calibration.detail.return-blocked:Instrument critique : une calibration conforme est exigée avant la remise en service.`;

  equipmentId = '';

  private readonly reload$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: CalibrationService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly auth: AuthService
  ) {
    const roles = this.auth.snapshot()?.roles ?? [];
    this.canDelete = roles.some(r => DELETE_ROLES.includes(r));
  }

  ngOnInit(): void {
    this.detail$ = combineLatest([this.route.paramMap, this.reload$]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([params]) => {
        this.equipmentId = params.get('id') ?? '';
        return this.svc.detail(this.equipmentId).pipe(
          catchError(err => {
            this.errorState$.next(safeErrorMessage(err,
              $localize`:@@calibration.detail.load-error:Équipement introuvable.`));
            return of(null);
          }),
          finalize(() => this.loadingState$.next(false))
        );
      }),
      // refCount:false : en-tête, synthèse, plan, enregistrements et MSA s'abonnent
      // séparément — sans quoi chaque section relancerait les cinq requêtes.
      shareReplay({ bufferSize: 1, refCount: false })
    );
  }

  // ---- Équipement ---------------------------------------------------------------

  edit(d: EquipmentDetail): void {
    const ref = this.dialog.open(CalibrationEquipmentDialogComponent, {
      data: { equipment: d.equipment },
      autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.reload$.next(); });
  }

  outOfService(d: EquipmentDetail): void {
    this.confirmThenStatus(d, 'OUT_OF_SERVICE',
      $localize`:@@calibration.detail.out-title:Mettre l'équipement hors service ?`,
      $localize`:@@calibration.detail.out-message:Aucun enregistrement de calibration ne pourra plus être saisi tant que l'instrument n'est pas remis en service.`);
  }

  retire(d: EquipmentDetail): void {
    this.confirmThenStatus(d, 'RETIRED',
      $localize`:@@calibration.detail.retire-title:Réformer l'équipement ?`,
      $localize`:@@calibration.detail.retire-message:L'état « réformé » est définitif : plus aucune modification, ni plan, ni enregistrement, ni étude MSA.`);
  }

  /** Retour en service : sans confirmation, l'action est réversible et non destructive. */
  returnToService(d: EquipmentDetail): void {
    this.runStatus(d, 'ACTIVE');
  }

  remove(d: EquipmentDetail): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@calibration.detail.delete-title:Supprimer l'équipement ?`,
        message: $localize`:@@calibration.detail.delete-message:Suppression définitive de l'instrument et de tout son historique : plan, enregistrements de calibration et études MSA.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        destructive: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok || this.pending) return;
      this.pending = true;
      this.svc.deleteEquipment(d.equipment.id)
        .pipe(finalize(() => { this.pending = false; }))
        .subscribe({
          next: () => {
            this.snack.open($localize`:@@calibration.detail.deleted:Équipement supprimé.`,
              $localize`:@@common.ok:OK`, { duration: 2500 });
            this.router.navigate(['/calibration']);
          },
          error: err => this.fail(err,
            $localize`:@@calibration.detail.delete-failed:Suppression impossible.`)
        });
    });
  }

  // ---- Plan -----------------------------------------------------------------------

  editPlan(d: EquipmentDetail): void {
    const ref = this.dialog.open(CalibrationPlanDialogComponent, {
      data: { equipmentId: d.equipment.id, plan: d.plan },
      autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(saved => { if (saved) this.reload$.next(); });
  }

  removePlan(d: EquipmentDetail): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@calibration.detail.delete-plan-title:Supprimer le plan de calibration ?`,
        message: $localize`:@@calibration.detail.delete-plan-message:L'instrument ne sera plus suivi : aucune échéance ne sera calculée à partir des prochains enregistrements.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        destructive: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok || this.pending) return;
      this.pending = true;
      this.svc.deletePlan(d.equipment.id)
        .pipe(finalize(() => { this.pending = false; }))
        .subscribe({
          next: () => {
            this.snack.open($localize`:@@calibration.detail.plan-deleted:Plan supprimé.`,
              $localize`:@@common.ok:OK`, { duration: 2500 });
            this.reload$.next();
          },
          error: err => this.fail(err,
            $localize`:@@calibration.detail.delete-failed:Suppression impossible.`)
        });
    });
  }

  // ---- Enregistrements et MSA -------------------------------------------------------

  addRecord(d: EquipmentDetail): void {
    const ref = this.dialog.open(CalibrationRecordDialogComponent, {
      data: { equipmentId: d.equipment.id, critical: d.equipment.critical },
      autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(saved => { if (saved) this.reload$.next(); });
  }

  addMsa(d: EquipmentDetail): void {
    const ref = this.dialog.open(CalibrationMsaDialogComponent, {
      data: { equipmentId: d.equipment.id },
      autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(saved => { if (saved) this.reload$.next(); });
  }

  // ---- Règles d'affichage des actions -------------------------------------------------

  /** Un instrument réformé est figé côté serveur : plus aucune mutation n'aboutit. */
  isRetired(d: EquipmentDetail): boolean {
    return d.equipment.status === 'RETIRED';
  }

  /** Le serveur refuse un enregistrement si l'équipement n'est pas EN SERVICE. */
  canRecord(d: EquipmentDetail): boolean {
    return d.equipment.status === 'ACTIVE';
  }

  /**
   * Remise en service : le serveur exige une dernière calibration PASS pour un
   * instrument critique. On affiche le bouton désactivé plutôt que de le masquer,
   * pour que l'utilisateur comprenne ce qui lui manque.
   */
  canReturnToService(d: EquipmentDetail): boolean {
    if (d.equipment.status !== 'OUT_OF_SERVICE') return false;
    return !d.equipment.critical || d.summary.lastResult === 'PASS';
  }

  /** Le serveur interdit de supprimer le plan d'un instrument critique. */
  canDeletePlan(d: EquipmentDetail): boolean {
    return !!d.plan && !d.equipment.critical && !this.isRetired(d) && this.canDelete;
  }

  // ---- Présentation ---------------------------------------------------------------------

  statusLabel(status: EquipmentStatus): string {
    return ({
      ACTIVE: $localize`:@@calibration.status.active:En service`,
      OUT_OF_SERVICE: $localize`:@@calibration.status.out-of-service:Hors service`,
      RETIRED: $localize`:@@calibration.status.retired:Réformé`
    })[status];
  }

  statusTone(status: EquipmentStatus): 'neutral' | 'success' | 'warn' {
    if (status === 'ACTIVE') return 'success';
    if (status === 'OUT_OF_SERVICE') return 'warn';
    return 'neutral';
  }

  resultLabel(result: CalibrationResult): string {
    return ({
      PASS: $localize`:@@calibration.result.pass:Conforme`,
      CONDITIONAL: $localize`:@@calibration.result.conditional:Conforme sous condition`,
      FAIL: $localize`:@@calibration.result.fail:Non conforme`
    })[result];
  }

  resultTone(result: CalibrationResult | null): 'neutral' | 'success' | 'warn' | 'danger' {
    if (result === 'PASS') return 'success';
    if (result === 'CONDITIONAL') return 'warn';
    if (result === 'FAIL') return 'danger';
    return 'neutral';
  }

  msaTypeLabel(type: MsaType): string {
    return ({
      GAGE_R_R: $localize`:@@calibration.msa.type.gage-rr:R&R (répétabilité/reproductibilité)`,
      BIAS: $localize`:@@calibration.msa.type.bias:Justesse (biais)`,
      LINEARITY: $localize`:@@calibration.msa.type.linearity:Linéarité`,
      STABILITY: $localize`:@@calibration.msa.type.stability:Stabilité`
    })[type];
  }

  msaResultLabel(result: MsaResult): string {
    return ({
      PASS: $localize`:@@calibration.msa.result.pass:Acceptable`,
      MARGINAL: $localize`:@@calibration.msa.result.marginal:Marginal`,
      FAIL: $localize`:@@calibration.msa.result.fail:Inacceptable`
    })[result];
  }

  msaResultTone(result: MsaResult): 'success' | 'warn' | 'danger' {
    if (result === 'PASS') return 'success';
    if (result === 'MARGINAL') return 'warn';
    return 'danger';
  }

  // ---- Interne --------------------------------------------------------------------------

  private confirmThenStatus(d: EquipmentDetail, target: EquipmentStatus,
                            title: string, message: string): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title, message,
        confirmLabel: $localize`:@@common.confirm:Confirmer`,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        destructive: target === 'RETIRED'
      }
    });
    ref.afterClosed().subscribe(ok => { if (ok) this.runStatus(d, target); });
  }

  private runStatus(d: EquipmentDetail, target: EquipmentStatus): void {
    if (this.pending) return;
    this.pending = true;
    this.svc.setStatus(d.equipment.id, target)
      .pipe(finalize(() => { this.pending = false; }))
      .subscribe({
        next: () => {
          this.snack.open($localize`:@@calibration.detail.status-changed:Statut mis à jour.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
          this.reload$.next();
        },
        error: err => this.fail(err,
          $localize`:@@calibration.detail.status-failed:Changement de statut refusé.`)
      });
  }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[calibration-detail] action failed',
      (err as { status?: number })?.status);
    this.snack.open(safeErrorMessage(err, fallback),
      $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
