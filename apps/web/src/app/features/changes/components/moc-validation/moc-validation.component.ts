import { Component, ElementRef, EventEmitter, Input, OnChanges, Output, QueryList, ViewChildren } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { mocClassificationLabel, mocDecisionLabel, mocErrorMessage } from '../../moc.labels';
import { MocService } from '../../moc.service';
import { MocDetail, MocValidation } from '../../moc.types';

/**
 * La validation : une carte par rôle requis. Seul un titulaire du rôle (jamais
 * le demandeur) voit les boutons Approuver / Rejeter, tous deux actifs d'emblée.
 * Approuver décide aussitôt ; Rejeter ouvre le champ du motif, qui prend le
 * focus, et le refus ne part qu'à « Confirmer le rejet » avec un motif. Un seul
 * refus rejette la demande. Les autres lisent qui est attendu.
 */
@Component({
  selector: 'qos-moc-validation',
  templateUrl: './moc-validation.component.html',
  styleUrls: ['./moc-validation.component.scss'],
  standalone: false
})
export class MocValidationComponent implements OnChanges {

  @Input({ required: true }) detail!: MocDetail;
  @Output() readonly changed = new EventEmitter<MocDetail>();

  readonly commentMax = 1000;
  comments: Record<string, string> = {};
  /** Cartes dont le titulaire a choisi de rejeter : le champ du motif est ouvert. */
  rejecting: Record<string, boolean> = {};
  /** Cartes où le rejet a été confirmé sans motif : le champ le réclame. */
  reasonAsked: Record<string, boolean> = {};

  @ViewChildren('champ') private champs?: QueryList<ElementRef<HTMLTextAreaElement>>;
  busy: string | null = null;

  constructor(private readonly service: MocService, private readonly snack: MatSnackBar) {}

  ngOnChanges(): void {
    // Une carte décidée n'a plus de brouillon de commentaire.
    for (const v of this.detail.validations) {
      if (v.id && !v.canDecide) {
        delete this.comments[v.id];
        delete this.rejecting[v.id];
        delete this.reasonAsked[v.id];
      }
    }
  }

  get assessed(): boolean {
    return this.detail.classification !== 'NOT_ASSESSED';
  }

  get summary(): string {
    const classe = mocClassificationLabel(this.detail.classification).toLowerCase();
    const n = this.detail.validations.length;
    return $localize`:@@moc.validation.summary:Changement ${classe}:classe: : ${n}:count: validation(s) requise(s). Un seul refus rejette la demande.`;
  }

  /** Une demande close d'avant le MOC n'a pas de cartes : ses décisions sont à l'historique. */
  get legacy(): boolean {
    return !this.detail.validations.length && (this.detail.status === 'APPROVED' || this.detail.status === 'REJECTED');
  }

  decisionLabel = mocDecisionLabel;

  comment(v: MocValidation): string {
    return v.id ? (this.comments[v.id] ?? '') : '';
  }

  setComment(v: MocValidation, value: string): void {
    if (v.id) this.comments[v.id] = value;
  }

  canReject(v: MocValidation): boolean {
    return this.comment(v).trim().length > 0;
  }

  /** Vrai une fois « Rejeter » pressé sans motif, tant que le commentaire reste vide. */
  missingReason(v: MocValidation): boolean {
    return !!v.id && !!this.reasonAsked[v.id] && !this.canReject(v);
  }

  isRejecting(v: MocValidation): boolean {
    return !!v.id && !!this.rejecting[v.id];
  }

  approve(v: MocValidation): void {
    this.decide(v, 'APPROVED');
  }

  /** Ouvre le champ du motif et y place le curseur, une fois rendu. */
  startReject(v: MocValidation): void {
    if (!v.id) return;
    this.rejecting[v.id] = true;
    const id = v.id;
    setTimeout(() => this.champs?.find(c => c.nativeElement.dataset['carte'] === id)?.nativeElement.focus());
  }

  /** Revient aux deux choix ; le motif saisi est oublié. */
  cancelReject(v: MocValidation): void {
    if (!v.id) return;
    delete this.rejecting[v.id];
    delete this.reasonAsked[v.id];
    delete this.comments[v.id];
  }

  reject(v: MocValidation, champ?: HTMLTextAreaElement): void {
    if (!this.canReject(v)) {
      if (v.id) this.reasonAsked[v.id] = true;
      champ?.focus();
      return;
    }
    this.decide(v, 'REJECTED');
  }

  private decide(v: MocValidation, decision: 'APPROVED' | 'REJECTED'): void {
    if (!v.id || this.busy) return;
    this.busy = v.id;
    const texte = this.comment(v).trim();
    this.service.decide(this.detail.id, v.id, { decision, comment: texte || null }).subscribe({
      next: d => {
        this.busy = null;
        this.snack.open(decision === 'APPROVED'
          ? $localize`:@@moc.validation.approved:Validation enregistrée.`
          : $localize`:@@moc.validation.rejected:Refus enregistré : la demande est rejetée.`, undefined, { duration: 3000 });
        this.changed.emit(d);
      },
      error: err => {
        this.busy = null;
        this.snack.open(mocErrorMessage(err, $localize`:@@moc.validation.failed:La décision n'a pas été enregistrée.`),
          undefined, { duration: 6000 });
      }
    });
  }

  holdersText(v: MocValidation): string {
    return v.holders.join(', ');
  }

  trackCard(i: number, v: MocValidation): string {
    return v.id ?? v.roleLabel + i;
  }
}
