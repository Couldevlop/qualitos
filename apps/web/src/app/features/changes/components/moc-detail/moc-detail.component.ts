import { Component, EventEmitter, Input, Output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';

import { AuthzService } from '../../../../core/authz/authz.service';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import {
  mocClassificationLabel,
  mocErrorMessage,
  mocHistoryLabel,
  mocStatusLabel,
  mocTypeLabel
} from '../../moc.labels';
import { MocService } from '../../moc.service';
import { MOC_STEPS, MocClassification, MocDetail, MocHistoryEntry, MocStatus } from '../../moc.types';
import {
  MocRequestDialogComponent,
  MocRequestDialogData
} from '../../pages/moc-request-dialog/moc-request-dialog.component';

/**
 * Le panneau d'une demande : en-tête, frise d'avancement, actions permises, et
 * les quatre onglets (demande, analyse d'impact, validation, historique).
 *
 * <p>Les actions ne s'affichent qu'à qui conduit la demande (son demandeur ou un
 * gestionnaire, `canManage` calculé par le serveur) et qu'au bon statut. Le
 * serveur reste seul juge : un refus est expliqué, jamais masqué.
 */
@Component({
  selector: 'qos-moc-detail',
  templateUrl: './moc-detail.component.html',
  styleUrls: ['./moc-detail.component.scss'],
  standalone: false
})
export class MocDetailComponent {

  @Input({ required: true }) detail!: MocDetail;
  @Output() readonly changed = new EventEmitter<MocDetail>();
  @Output() readonly deleted = new EventEmitter<string>();

  readonly steps = MOC_STEPS;
  tab = 0;
  busy = false;
  canCreateRisk = false;

  constructor(
    private readonly service: MocService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    authz: AuthzService
  ) {
    authz.can('risk.manage').pipe(takeUntilDestroyed()).subscribe(v => this.canCreateRisk = v);
  }

  // ---------- frise ----------

  /** Le rang de l'étape courante ; un rejet occupe la dernière place, en rouge. */
  get stepIndex(): number {
    const s: MocStatus = this.detail.status === 'REJECTED' ? 'APPROVED' : this.detail.status;
    return this.steps.indexOf(s);
  }

  get progress(): number {
    return Math.round((this.stepIndex / (this.steps.length - 1)) * 100);
  }

  stepLabel(s: MocStatus, i: number): string {
    return i === this.steps.length - 1 && this.detail.status === 'REJECTED' ? mocStatusLabel('REJECTED') : mocStatusLabel(s);
  }

  // ---------- état ----------

  get editable(): boolean {
    return this.detail.canManage && (this.detail.status === 'DRAFT' || this.detail.status === 'IN_ANALYSIS');
  }

  get missingDomains(): number {
    return this.detail.impacts.filter(i => !i.level).length;
  }

  statusLabel = mocStatusLabel;
  classificationLabel = mocClassificationLabel;
  typeLabel = mocTypeLabel;

  /** « Demande créée par S. Benali » : l'acteur est nommé quand l'historique le connaît. */
  historyText(h: MocHistoryEntry): string {
    const quoi = mocHistoryLabel(h.event);
    const qui = h.actorName;
    return qui ? $localize`:@@moc.history.with-actor:${quoi}:event: par ${qui}:actor:` : quoi;
  }

  /**
   * Le détail est écrit par le serveur ; la soumission y consigne la
   * classification par son code (« CRITICAL · 54/100 »). On l'affiche dans la
   * langue de l'écran, y compris pour les entrées déjà enregistrées.
   */
  historyDetail(h: MocHistoryEntry): string | null {
    if (!h.detail) {
      return null;
    }
    return h.detail.replace(/^(NOT_ASSESSED|MINOR|MAJOR|CRITICAL)/,
      (code) => mocClassificationLabel(code as MocClassification));
  }

  trackHistory(i: number, h: MocHistoryEntry): string {
    return h.occurredAt + h.event + i;
  }

  // ---------- actions ----------

  edit(): void {
    this.dialog.open<MocRequestDialogComponent, MocRequestDialogData, MocDetail>(MocRequestDialogComponent, {
      data: { detail: this.detail }, width: '640px', panelClass: 'qos-dialog-panel'
    }).afterClosed().subscribe(d => {
      if (d) this.changed.emit(d);
    });
  }

  sendToAnalysis(): void {
    this.run(this.service.sendToAnalysis(this.detail.id),
      $localize`:@@moc.sent-to-analysis:Demande envoyée en analyse.`, 1);
  }

  submit(): void {
    this.run(this.service.submit(this.detail.id),
      $localize`:@@moc.submitted:Demande soumise à validation : les validateurs sont prévenus.`, 2);
  }

  reopen(): void {
    this.run(this.service.reopen(this.detail.id), $localize`:@@moc.reopened:L'analyse est rouverte.`, 1);
  }

  remove(): void {
    this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@moc.delete-title:Supprimer la demande ${this.detail.code}:code: ?`,
        message: $localize`:@@moc.delete-message:Le brouillon et son historique seront supprimés. Cette action est définitive.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      }
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.busy = true;
      this.service.delete(this.detail.id).subscribe({
        next: () => {
          this.busy = false;
          this.snack.open($localize`:@@moc.deleted:Demande supprimée.`, undefined, { duration: 3000 });
          this.deleted.emit(this.detail.id);
        },
        error: err => this.fail(err)
      });
    });
  }

  private run(call: Observable<MocDetail>, message: string, tab: number): void {
    if (this.busy) return;
    this.busy = true;
    call.subscribe({
      next: d => {
        this.busy = false;
        this.tab = tab;
        this.snack.open(message, undefined, { duration: 3000 });
        this.changed.emit(d);
      },
      error: err => this.fail(err)
    });
  }

  private fail(err: unknown): void {
    this.busy = false;
    this.snack.open(mocErrorMessage(err, $localize`:@@moc.action-failed:L'action sur la demande a échoué.`),
      undefined, { duration: 6000 });
  }
}
