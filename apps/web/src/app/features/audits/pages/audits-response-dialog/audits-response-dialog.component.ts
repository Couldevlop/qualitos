import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { AuditsService } from '../../audits.service';
import { ChecklistItemResponse } from '../../audits.types';

export interface AuditsResponseDialogData {
  planId: string;
  itemId: string;
  question: string;
  clauseRef?: string;
  /** Pre-fill with existing values if the question was already answered. */
  initialResponse?: string;
  initialConformant?: boolean | null;
}

@Component({
  selector: 'qos-audits-response-dialog',
  templateUrl: './audits-response-dialog.component.html',
  styleUrls: ['./audits-response-dialog.component.scss'],
  standalone: false
})
export class AuditsResponseDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    response: [''],
    // Tri-state: null = not yet decided (form-level), true = conforme, false = NC.
    // Validators.required ensures the auditor explicitly takes a position.
    conformant: [null as boolean | null, [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly audits: AuditsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<AuditsResponseDialogComponent, ChecklistItemResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AuditsResponseDialogData
  ) {
    this.form.patchValue({
      response: data.initialResponse ?? '',
      conformant: data.initialConformant ?? null
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const { response, conformant } = this.form.getRawValue();
    this.audits
      .respondChecklistItem(this.data.planId, this.data.itemId, {
        response: response?.trim() || undefined,
        conformant: conformant === null ? undefined : conformant
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: item => {
          this.snack.open('Réponse enregistrée.', 'OK', { duration: 2000 });
          this.dialogRef.close(item);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[audits-response-dialog] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, 'Erreur lors de l\'enregistrement.'),
            'OK', { duration: 4000 }
          );
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
