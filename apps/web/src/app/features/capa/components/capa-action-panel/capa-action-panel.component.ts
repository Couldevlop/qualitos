import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { actionTypeLabel } from '../../capa.labels';
import { CapaService } from '../../capa.service';
import {
  CapaActionResponse, CapaActionStatus, CapaActionType, CapaCaseResponse, CapaStatus
} from '../../capa.types';
import {
  CapaActionDialogComponent, CapaActionDialogData
} from '../../pages/capa-action-dialog/capa-action-dialog.component';

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
 * <p><b>Un dossier porte plusieurs actions</b> (ADR 0115, qui révise l'ADR 0088
 * sur ce seul point) : on endigue, on corrige la cause, on prévient la récidive,
 * et le 8D nomme ces étapes séparément (D3, D5, D6). Le tableau les montre
 * toutes, chacune avec son porteur, son échéance et son avancement ; une ligne
 * s'ouvre d'un clic pour la corriger, et le bouton d'ajout en ajoute une.
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

  /**
   * Les natures à montrer. Vide — le défaut — montre toutes les actions.
   *
   * <p>Le rapport 8D nomme les trois gestes séparément : endiguer (D3), corriger
   * la cause (D5), prévenir la récidive (D6). Chaque discipline reçoit donc le
   * tableau de SA nature ; un tableau complet répété sous trois titres laisserait
   * chercher, dans chacun, les lignes qui le concernent.
   */
  // En lecture seule, et pour une raison : l'hôte doit passer un tableau STABLE
  // (le même objet d'une passe de détection à l'autre), sans quoi `ngOnChanges`
  // relirait le dossier par le réseau à chaque passe. Le type le dit.
  @Input() natures: readonly CapaActionType[] = [];

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
    const toutes = this.capa?.actions ?? [];
    if (this.natures.length === 0) {
      return toutes;
    }
    // Une action sans nature est corrective : c'est le défaut du serveur, et
    // l'exclure du D5 ferait disparaître les anciennes actions du rapport.
    return toutes.filter(a => this.natures.includes(a.actionType || 'CORRECTIVE'));
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

  // ---------- ajouter, ouvrir ----------

  /** Libellé lisible par un lecteur d'écran pour la ligne qui s'ouvre. */
  openLabel(action: CapaActionResponse): string {
    return $localize`:@@capa.actions.open-aria:Ouvrir l'action : ${action.title}:titre:`;
  }

  /**
   * Ajoute une action au dossier.
   *
   * <p>Le pilote et l'échéance du dossier sont proposés — c'est le cas le plus
   * fréquent — et restent modifiables : une action préventive portée par
   * quelqu'un d'autre, pour plus tard, est exactement ce que la liste permet
   * désormais d'écrire.
   */
  addAction(): void {
    if (!this.capa || this.isTerminal(this.capa.status)) {
      return;
    }
    this.openDialog({
      caseId: this.capa.id,
      ownerId: this.capa.ownerId,
      ownerName: this.capa.ownerName,
      dueDate: this.capa.dueDate,
      // Tableau restreint à une nature : l'action ajoutée là est de cette nature.
      actionType: this.natures.length === 1 ? this.natures[0] : undefined
    });
  }

  /**
   * Ouvre une action pour la relire et la corriger.
   *
   * <p>Un dossier terminé s'ouvre en lecture seule plutôt que de ne pas
   * s'ouvrir : relire ce qui a été fait est légitime après la clôture, et une
   * ligne qui ne réagit pas au clic passe pour une panne.
   */
  openAction(action: CapaActionResponse): void {
    if (!this.capa) {
      return;
    }
    this.openDialog({
      caseId: this.capa.id,
      action,
      ownerId: this.capa.ownerId,
      ownerName: this.capa.ownerName,
      dueDate: this.capa.dueDate,
      readOnly: this.isTerminal(this.capa.status)
    });
  }

  private openDialog(data: CapaActionDialogData): void {
    this.dialog
      .open<CapaActionDialogComponent, CapaActionDialogData, boolean>(CapaActionDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(saved => {
        if (saved) {
          this.reload();
          this.changed.emit();
        }
      });
  }
}
