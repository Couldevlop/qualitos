import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import {
  Tone, requirementLabel, riskBasis, riskLabel, riskTone, roleLabel, statusLabel, statusTone
} from '../../ai-systems.labels';
import {
  InUseRequirementKey,
  canDecommission, canDelete, canEdit, canPutInUse, canRegister, canWithdraw,
  inUseRequirements, isProhibited, isStuckBeforeUse, missingInUseRequirements
} from '../../ai-systems.rules';
import { AiSystemsService } from '../../ai-systems.service';
import {
  AiRiskClassification, AiSystemRole, AiSystemStatus, AiSystemView
} from '../../ai-systems.types';
import {
  AiSystemFormDialogComponent, AiSystemFormData
} from '../ai-system-form-dialog/ai-system-form-dialog.component';
import {
  AiSystemWithdrawDialogComponent, AiSystemWithdrawData
} from '../ai-system-withdraw-dialog/ai-system-withdraw-dialog.component';

export interface RequirementRow {
  key: InUseRequirementKey;
  label: string;
  satisfied: boolean;
}

/**
 * Fiche d'un système d'IA du registre AI Act.
 *
 * Deux contraintes du domaine serveur pilotent tout l'écran :
 *  - la fiche n'est modifiable et supprimable qu'à l'état brouillon ;
 *  - la mise en service exige des obligations satisfaites (Art. 13, 14, 43, 50),
 *    qu'il devient impossible de compléter une fois la fiche enregistrée.
 * L'écran affiche donc en permanence la check-list, et n'expose une action que
 * lorsque le serveur l'accepterait.
 */
@Component({
  selector: 'qos-ai-system-detail',
  templateUrl: './ai-system-detail.component.html',
  styleUrls: ['./ai-system-detail.component.scss'],
  standalone: false
})
export class AiSystemDetailComponent implements OnInit {

  system$!: Observable<AiSystemView | null>;

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  /** Une action de cycle de vie en vol : neutralise les autres, sans les cacher. */
  pending = false;

  private readonly reload$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: AiSystemsService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.system$ = combineLatest([this.route.paramMap, this.reload$]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([params]) => this.svc.resolve(params.get('id') ?? '').pipe(
        tap(found => {
          // `resolve` renvoie null sans appeler le serveur quand le paramètre n'est
          // ni un UUID ni une référence : inutile de provoquer un 400.
          if (!found) {
            this.errorState$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          }
        }),
        catchError(err => {
          this.errorState$.next(safeErrorMessage(err,
            $localize`:@@ai-systems.detail.not-found:Ce système d'IA est introuvable.`));
          return of(null);
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      shareReplay({ bufferSize: 1, refCount: false })
    );
  }

  // ---- Actions ---------------------------------------------------------------

  edit(system: AiSystemView): void {
    if (this.pending || !canEdit(system)) return;
    this.dialog.open<AiSystemFormDialogComponent, AiSystemFormData, AiSystemView | undefined>(
      AiSystemFormDialogComponent,
      {
        data: { mode: 'edit', system }, panelClass: 'qos-dialog-panel',
        autoFocus: 'first-tabbable', restoreFocus: true
      }
    ).afterClosed().subscribe(updated => {
      if (!updated) return;
      this.notify($localize`:@@ai-systems.detail.updated:Fiche mise à jour.`);
      this.reload$.next();
    });
  }

  register(system: AiSystemView): void {
    if (this.pending || !canRegister(system)) return;
    const missing = missingInUseRequirements(system).length;
    const message = missing
      ? $localize`:@@ai-systems.detail.register-incomplete:Des obligations ne sont pas encore documentées. Après enregistrement la fiche devient définitive : le système ne pourra plus être mis en service, seulement abandonné.`
      : $localize`:@@ai-systems.detail.register-message:La fiche devient définitive : ses informations ne seront plus modifiables.`;
    this.confirmThen(
      $localize`:@@ai-systems.detail.register-title:Enregistrer ce système ?`,
      message,
      $localize`:@@ai-systems.detail.register:Enregistrer`,
      missing > 0,
      () => this.run(this.svc.register(system.id),
        $localize`:@@ai-systems.detail.registered:Système enregistré.`)
    );
  }

  putInUse(system: AiSystemView): void {
    if (this.pending || !canPutInUse(system)) return;
    this.confirmThen(
      $localize`:@@ai-systems.detail.put-in-use-title:Déclarer ce système en service ?`,
      $localize`:@@ai-systems.detail.put-in-use-message:La date de mise en service sera horodatée et le système entrera dans le périmètre de surveillance post-marché.`,
      $localize`:@@ai-systems.detail.put-in-use:Mettre en service`,
      false,
      () => this.run(this.svc.putInUse(system.id),
        $localize`:@@ai-systems.detail.in-use:Système déclaré en service.`)
    );
  }

  decommission(system: AiSystemView): void {
    if (this.pending || !canDecommission(system)) return;
    this.confirmThen(
      $localize`:@@ai-systems.detail.decommission-title:Retirer ce système du service ?`,
      $localize`:@@ai-systems.detail.decommission-message:La fiche restera consultable comme archive, avec sa date de fin d'exploitation.`,
      $localize`:@@ai-systems.detail.decommission:Retirer du service`,
      true,
      () => this.run(this.svc.decommission(system.id),
        $localize`:@@ai-systems.detail.decommissioned:Système retiré du service.`)
    );
  }

  withdraw(system: AiSystemView): void {
    if (this.pending || !canWithdraw(system)) return;
    this.dialog.open<AiSystemWithdrawDialogComponent, AiSystemWithdrawData, AiSystemView | undefined>(
      AiSystemWithdrawDialogComponent,
      {
        data: { system }, panelClass: 'qos-dialog-panel',
        autoFocus: 'first-tabbable', restoreFocus: true
      }
    ).afterClosed().subscribe(updated => {
      if (!updated) return;
      this.notify($localize`:@@ai-systems.detail.withdrawn:Système abandonné.`);
      this.reload$.next();
    });
  }

  remove(system: AiSystemView): void {
    if (this.pending || !canDelete(system)) return;
    this.confirmThen(
      $localize`:@@ai-systems.detail.delete-title:Supprimer ce brouillon ?`,
      $localize`:@@ai-systems.detail.delete-message:Suppression définitive. Seuls les brouillons peuvent être supprimés : les autres états sont conservés pour l'audit.`,
      $localize`:@@common.delete:Supprimer`,
      true,
      () => {
        this.pending = true;
        this.svc.delete(system.id)
          .pipe(finalize(() => { this.pending = false; }))
          .subscribe({
            next: () => {
              this.notify($localize`:@@ai-systems.detail.deleted:Brouillon supprimé.`);
              this.router.navigate(['/ai-systems']);
            },
            error: err => this.notify(safeErrorMessage(err,
              $localize`:@@common.delete-failed:Suppression impossible.`))
          });
      }
    );
  }

  private confirmThen(title: string, message: string, confirmLabel: string,
                      destructive: boolean, action: () => void): void {
    this.dialog.open<ConfirmDialogComponent, unknown, boolean>(ConfirmDialogComponent, {
      data: {
        title, message, confirmLabel,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        destructive
      },
      panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(confirmed => { if (confirmed) action(); });
  }

  /**
   * Recharge systématiquement après une transition : le serveur recalcule les
   * drapeaux d'obligations et les dates d'effet, recopier la réponse localement
   * donnerait une vue partiellement fausse en cas d'évolution du domaine.
   */
  private run(call$: Observable<AiSystemView>, success: string): void {
    this.pending = true;
    call$.pipe(finalize(() => { this.pending = false; })).subscribe({
      next: () => { this.notify(success); this.reload$.next(); },
      error: err => this.notify(safeErrorMessage(err,
        $localize`:@@ai-systems.detail.action-failed:L'opération sur le système a échoué.`))
    });
  }

  private notify(message: string): void {
    this.snack.open(message, $localize`:@@common.ok:OK`, { duration: 3000 });
  }

  // ---- Présentation ----------------------------------------------------------

  canEdit(system: AiSystemView): boolean { return canEdit(system); }
  canDelete(system: AiSystemView): boolean { return canDelete(system); }
  canRegister(system: AiSystemView): boolean { return canRegister(system); }
  canPutInUse(system: AiSystemView): boolean { return canPutInUse(system); }
  canDecommission(system: AiSystemView): boolean { return canDecommission(system); }
  canWithdraw(system: AiSystemView): boolean { return canWithdraw(system); }
  isProhibited(system: AiSystemView): boolean { return isProhibited(system); }
  isStuck(system: AiSystemView): boolean { return isStuckBeforeUse(system); }

  /** Check-list complète (satisfaite ou non) : un écran d'obligations, pas un refus sec. */
  requirements(system: AiSystemView): RequirementRow[] {
    return inUseRequirements(system.riskClassification, system)
      .map(r => ({ key: r.key, label: requirementLabel(r.key), satisfied: r.satisfied }));
  }

  trackByKey(_index: number, row: RequirementRow): string { return row.key; }
  trackByValue(_index: number, value: string): string { return value; }

  riskLabel(risk: AiRiskClassification): string { return riskLabel(risk); }
  riskBasis(risk: AiRiskClassification): string { return riskBasis(risk); }
  riskTone(risk: AiRiskClassification): Tone { return riskTone(risk); }
  statusLabel(status: AiSystemStatus): string { return statusLabel(status); }
  statusTone(status: AiSystemStatus): Tone { return statusTone(status); }
  roleLabel(role: AiSystemRole): string { return roleLabel(role); }
}
