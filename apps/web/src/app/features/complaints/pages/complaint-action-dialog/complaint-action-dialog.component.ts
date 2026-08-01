import { Component, Inject } from '@angular/core';
import { FormBuilder, ValidatorFn, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ComplaintsService } from '../../complaints.service';
import { Complaint, UUID_PATTERN } from '../../complaints.types';

/** Transitions qui demandent UNE information avant d'être envoyées au serveur. */
export type ComplaintActionMode = 'ASSIGN' | 'REJECT' | 'RESOLVE' | 'SATISFACTION';

export interface ComplaintActionDialogData {
  complaint: Complaint;
  mode: ComplaintActionMode;
}

/**
 * Dialogue d'action à un seul champ (assignation, rejet, résolution, satisfaction).
 *
 * Quatre dialogues d'un champ chacun auraient été quatre fois le même squelette :
 * on factorise, mais chaque mode garde son libellé, son champ, ses validations et
 * l'appel serveur qui lui correspond — aucune logique n'est mutualisée à tort.
 */
@Component({
  selector: 'qos-complaint-action-dialog',
  templateUrl: './complaint-action-dialog.component.html',
  styleUrls: ['./complaint-action-dialog.component.scss'],
  standalone: false
})
export class ComplaintActionDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    text: [this.initialText(), this.textValidators()],
    // Échelle 0..10 imposée par le serveur (@Min(0) @Max(10)).
    score: [this.data.complaint.satisfactionScore ?? 8,
            [Validators.required, Validators.min(0), Validators.max(10)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ComplaintsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<ComplaintActionDialogComponent, Complaint>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ComplaintActionDialogData
  ) {
    // Un contrôle désactivé sort du calcul de validité : c'est ce qui permet à un
    // seul FormGroup de servir les quatre modes sans validation parasite.
    if (this.mode === 'SATISFACTION') this.form.controls.text.disable();
    else this.form.controls.score.disable();
  }

  get mode(): ComplaintActionMode {
    return this.data.mode;
  }

  // ---- Présentation dépendante du mode ---------------------------------------

  get title(): string {
    switch (this.mode) {
      case 'ASSIGN': return $localize`:@@complaints.action.assign-title:Assigner la réclamation`;
      case 'REJECT': return $localize`:@@complaints.action.reject-title:Rejeter la réclamation`;
      case 'RESOLVE': return $localize`:@@complaints.action.resolve-title:Résoudre la réclamation`;
      default: return $localize`:@@complaints.action.satisfaction-title:Satisfaction du client`;
    }
  }

  get subtitle(): string {
    switch (this.mode) {
      case 'ASSIGN': return $localize`:@@complaints.action.assign-subtitle:Assigner une réclamation encore « Reçue » la fait passer en investigation.`;
      case 'REJECT': return $localize`:@@complaints.action.reject-subtitle:Action terminale : une réclamation rejetée ne peut plus être traitée ni rouverte.`;
      case 'RESOLVE': return $localize`:@@complaints.action.resolve-subtitle:Le dossier CAPA est facultatif ; il relie la réclamation à l'action corrective engagée.`;
      default: return $localize`:@@complaints.action.satisfaction-subtitle:Note de 0 à 10 recueillie auprès du client après traitement.`;
    }
  }

  get fieldLabel(): string {
    switch (this.mode) {
      case 'ASSIGN': return $localize`:@@complaints.action.assignee-label:Utilisateur assigné (UUID)`;
      case 'REJECT': return $localize`:@@complaints.action.reason-label:Motif du rejet`;
      case 'RESOLVE': return $localize`:@@complaints.action.capa-label:Dossier CAPA lié (UUID, facultatif)`;
      default: return $localize`:@@complaints.action.score-label:Note de satisfaction (0 à 10)`;
    }
  }

  get submitLabel(): string {
    switch (this.mode) {
      case 'ASSIGN': return $localize`:@@complaints.action.assign-submit:Assigner`;
      case 'REJECT': return $localize`:@@complaints.action.reject-submit:Confirmer le rejet`;
      case 'RESOLVE': return $localize`:@@complaints.action.resolve-submit:Marquer résolue`;
      default: return $localize`:@@complaints.action.satisfaction-submit:Enregistrer la note`;
    }
  }

  get submitIcon(): string {
    switch (this.mode) {
      case 'ASSIGN': return 'person_add';
      case 'REJECT': return 'block';
      case 'RESOLVE': return 'task_alt';
      default: return 'sentiment_satisfied';
    }
  }

  get submitColor(): 'primary' | 'warn' {
    return this.mode === 'REJECT' ? 'warn' : 'primary';
  }

  /** Seuls l'assignation et le rejet exigent une saisie. */
  get fieldRequired(): boolean {
    return this.mode === 'ASSIGN' || this.mode === 'REJECT';
  }

  get isTextMode(): boolean {
    return this.mode !== 'SATISFACTION';
  }

  get isLongText(): boolean {
    return this.mode === 'REJECT';
  }

  /** Raccourci d'assignation : se prendre le dossier sans copier son propre UUID. */
  get canAssignToSelf(): boolean {
    const id = this.auth.snapshot()?.userId;
    return this.mode === 'ASSIGN' && !!id && UUID_PATTERN.test(id)
      && this.form.controls.text.value !== id;
  }

  assignToSelf(): void {
    const id = this.auth.snapshot()?.userId;
    if (id) this.form.controls.text.setValue(id);
  }

  // ---- Envoi -----------------------------------------------------------------

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.call()
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: updated => {
          this.snack.open(this.successMessage(), $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(updated);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[complaint-action] failed', this.mode, (err as { status?: number })?.status);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@complaints.action.error:Action impossible dans l'état actuel.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  private call(): Observable<Complaint> {
    const id = this.data.complaint.id;
    const text = this.form.controls.text.value.trim();
    switch (this.mode) {
      case 'ASSIGN': return this.svc.assign(id, text);
      case 'REJECT': return this.svc.reject(id, text);
      case 'RESOLVE': return this.svc.resolve(id, text.length > 0 ? text : undefined);
      default: return this.svc.setSatisfaction(id, this.form.controls.score.value);
    }
  }

  private successMessage(): string {
    switch (this.mode) {
      case 'ASSIGN': return $localize`:@@complaints.action.assign-success:Réclamation assignée.`;
      case 'REJECT': return $localize`:@@complaints.action.reject-success:Réclamation rejetée.`;
      case 'RESOLVE': return $localize`:@@complaints.action.resolve-success:Réclamation résolue.`;
      default: return $localize`:@@complaints.action.satisfaction-success:Note de satisfaction enregistrée.`;
    }
  }

  private initialText(): string {
    // On repropose l'assignation courante : le cas le plus fréquent est de la
    // corriger, pas de la ressaisir intégralement.
    return this.data.mode === 'ASSIGN' ? (this.data.complaint.assignedToUserId ?? '') : '';
  }

  private textValidators(): ValidatorFn[] {
    switch (this.data.mode) {
      case 'ASSIGN': return [Validators.required, Validators.pattern(UUID_PATTERN)];
      case 'REJECT': return [Validators.required, Validators.maxLength(1000)];
      case 'RESOLVE': return [Validators.pattern(UUID_PATTERN)];
      default: return [];
    }
  }
}
