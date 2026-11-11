import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { actionTypeLabel } from '../../capa.labels';
import { CapaService } from '../../capa.service';
import {
  CapaActionResponse, CapaActionStatus, CapaCaseResponse, CapaStatus
} from '../../capa.types';
import {
  CapaCreateDialogComponent, CapaCreateDialogData, CapaOpened
} from '../../pages/capa-create-dialog/capa-create-dialog.component';

/** Le pas suivant d'une action : il n'y en a jamais deux. */
const SUITE: Record<CapaActionStatus, CapaActionStatus | null> = {
  PENDING: 'IN_PROGRESS',
  IN_PROGRESS: 'DONE',
  DONE: null
};

/**
 * L'action du dossier CAPA, lue et avancée <b>hors de la fiche CAPA</b>.
 *
 * <p>Depuis la fiche d'un écart, « Voir la CAPA » emmenait dans un autre écran
 * pour une seule question : où en est l'action ? On revenait donc en arrière
 * pour la poser, et on ne la posait plus. Ce panneau la montre sur place, et
 * laisse la faire avancer sans quitter l'écart — c'est aussi ce que le D5 du
 * rapport 8D rassemble.
 *
 * <p><b>Une CAPA EST son action</b> (ADR 0088) : pas de liste à rallonger, pas
 * de bouton d'ajout. Un dossier sans action — ancien, ou ouvert par un automate
 * — propose de définir la sienne, <b>une fois</b>, dans la même fenêtre que la
 * création. Les anciens dossiers à plusieurs actions les montrent toutes.
 *
 * <p>Il ne remplace pas le bloc de la fiche CAPA, qui gère en plus la PREUVE de
 * chaque action (dépôt, retrait, aperçu) avec son sélecteur de fichier : y
 * attacher une pièce depuis la fiche de l'écart serait une autre décision, et
 * un panneau qui ferait les deux ne serait lisible dans aucun des deux écrans.
 */
@Component({
  selector: 'qos-capa-action-panel',
  templateUrl: './capa-action-panel.component.html',
  styleUrls: ['./capa-action-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class CapaActionPanelComponent implements OnChanges {

  /** Le dossier à lire. Le panneau le charge lui-même : l'appelant n'a que l'identifiant. */
  @Input() capaCaseId = '';

  /** Émis après chaque changement enregistré : l'écran appelant peut se relire. */
  @Output() readonly changed = new EventEmitter<void>();

  capa: CapaCaseResponse | null = null;
  readonly loading$ = new BehaviorSubject<boolean>(false);
  readonly acting$ = new BehaviorSubject<boolean>(false);
  /** Ce qui empêche de lire le dossier, dit plutôt que laissé en blanc. */
  error: string | null = null;

  constructor(
    private readonly capaService: CapaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnChanges(): void {
    this.reload();
  }

  reload(): void {
    if (!this.capaCaseId) {
      this.capa = null;
      this.error = null;
      this.cdr.markForCheck();
      return;
    }
    this.error = null;
    this.loading$.next(true);
    this.capaService.getCase(this.capaCaseId).pipe(
      finalize(() => this.loading$.next(false))
    ).subscribe({
      next: capa => {
        this.capa = capa;
        this.cdr.markForCheck();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[capa-action-panel] getCase failed', err?.status, err?.error?.title);
        this.capa = null;
        this.error = safeErrorMessage(
          err, $localize`:@@capa.action-panel.failed:L'action de la CAPA n'a pas pu être lue.`);
        this.cdr.markForCheck();
      }
    });
  }

  // ---------- lecture ----------

  get actions(): CapaActionResponse[] {
    return this.capa?.actions ?? [];
  }

  trackById(_: number, action: CapaActionResponse): string {
    return action.id;
  }

  /** Un dossier clos ou rejeté ne bouge plus : le serveur tient la même garde. */
  isTerminal(status: CapaStatus): boolean {
    return status === 'CLOSED' || status === 'REJECTED';
  }

  actionTypeLabel(type: CapaActionResponse['actionType']): string {
    return actionTypeLabel(type || 'CORRECTIVE');
  }

  actionStatusLabel(status: CapaActionStatus): string {
    return status === 'PENDING'
      ? $localize`:@@capa.action-status.pending:À faire`
      : status === 'IN_PROGRESS'
        ? $localize`:@@capa.action-status.in-progress:En cours`
        : $localize`:@@capa.action-status.done:Faite`;
  }

  actionBadge(status: CapaActionStatus): string {
    return 'badge badge-' + status.toLowerCase().replace('_', '-');
  }

  /**
   * Le responsable et l'échéance de l'action ne s'affichent que s'ils
   * s'écartent de ceux du dossier : à la décision ils en sont copiés, et répéter
   * la même valeur à deux endroits ne renseigne personne.
   */
  showsAssignee(action: CapaActionResponse): boolean {
    return !!this.capa && action.assigneeId !== this.capa.ownerId;
  }

  showsDueDate(action: CapaActionResponse): boolean {
    return !!action.dueDate && !!this.capa && action.dueDate !== this.capa.dueDate;
  }

  // ---------- avancer ----------

  nextStatus(status: CapaActionStatus): CapaActionStatus | null {
    return SUITE[status];
  }

  canAdvance(action: CapaActionResponse): boolean {
    return !!this.capa
      && action.status !== 'DONE'
      && !this.isTerminal(this.capa.status)
      && !this.acting$.value;
  }

  advanceLabel(status: CapaActionStatus): string {
    return status === 'PENDING'
      ? $localize`:@@capa.detail.action-start:Démarrer`
      : $localize`:@@capa.detail.action-complete:Terminer`;
  }

  /**
   * Fait avancer l'action d'un cran.
   *
   * <p>Seul le statut part : renvoyer le libellé le réécrirait à l'identique et
   * écraserait une correction faite entre-temps par quelqu'un d'autre.
   */
  advance(action: CapaActionResponse): void {
    const suivant = this.nextStatus(action.status);
    if (!suivant || !this.capa || this.acting$.value) {
      return;
    }
    this.acting$.next(true);
    this.capaService.updateAction(this.capa.id, action.id, { status: suivant }).pipe(
      finalize(() => this.acting$.next(false))
    ).subscribe({
      next: () => {
        this.snack.open(
          $localize`:@@capa.detail.action-advanced:Action mise à jour.`,
          $localize`:@@common.ok:OK`, { duration: 2000 });
        this.reload();
        this.changed.emit();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[capa-action-panel] advance failed', err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(
            err, $localize`:@@capa.detail.action-advance-error:Mise à jour de l'action impossible.`),
          $localize`:@@common.close:Fermer`, { duration: 4000 });
      }
    });
  }

  // ---------- définir l'action manquante ----------

  /**
   * Définir l'action d'un dossier qui n'en a pas.
   *
   * <p>Une fois, et dans la même fenêtre que la création : c'est le geste prévu
   * par l'ADR 0088 pour les dossiers anciens ou ouverts par un automate, pas une
   * porte d'entrée vers une liste d'actions.
   */
  defineAction(): void {
    if (!this.capa || this.actions.length > 0 || this.isTerminal(this.capa.status)) {
      return;
    }
    const data: CapaCreateDialogData = {
      mode: 'define-action',
      caseId: this.capa.id,
      prefill: {
        title: this.capa.title,
        description: this.capa.description,
        type: this.capa.type === 'PREVENTIVE' ? 'PREVENTIVE' : 'CORRECTIVE',
        dueDate: this.capa.dueDate
      }
    };
    this.dialog
      .open<CapaCreateDialogComponent, CapaCreateDialogData, CapaOpened>(CapaCreateDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(defined => {
        if (defined) {
          this.reload();
          this.changed.emit();
        }
      });
  }
}
