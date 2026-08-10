import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { CapaService } from '../../capa.service';
import { CapaActionResponse } from '../../capa.types';

export interface CapaActionDialogData {
  caseId: string;
}

@Component({
  selector: 'qos-capa-action-dialog',
  templateUrl: './capa-action-dialog.component.html',
  styleUrls: ['./capa-action-dialog.component.scss'],
  standalone: false
})
export class CapaActionDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    // Jour de la DÉCISION. Pré-rempli au jour même parce que c'est le cas
    // courant, mais modifiable : une action décidée en comité et saisie plus
    // tard doit pouvoir porter la date du comité (ADR 0052).
    decidedOn: [CapaActionDialogComponent.today()],
    assigneeName: ['', [Validators.maxLength(255)]],
    dueDate: ['']
  });

  /** Date du jour au format d'un input[type=date], sans dépendance de fuseau serveur. */
  private static today(): string {
    const d = new Date();
    const mois = String(d.getMonth() + 1).padStart(2, '0');
    const jour = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${mois}-${jour}`;
  }

  constructor(
    private readonly fb: FormBuilder,
    private readonly capa: CapaService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CapaActionDialogComponent, CapaActionResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CapaActionDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const { title, description, decidedOn, assigneeName, dueDate } = this.form.getRawValue();
    this.capa
      .addAction(this.data.caseId, {
        title: title.trim(),
        description: description?.trim() || undefined,
        decidedOn: decidedOn || undefined,
        assigneeName: assigneeName?.trim() || undefined,
        dueDate: dueDate || undefined
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: action => {
          this.snack.open($localize`:@@capa.action.added:Action ajoutée.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(action);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-action-dialog] addAction failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-add:Erreur lors de l'ajout.`),
            $localize`:@@common.ok:OK`, { duration: 4000 }
          );
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
