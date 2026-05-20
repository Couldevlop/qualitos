import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { AuditsService } from '../../audits.service';
import { ChecklistItemResponse } from '../../audits.types';

export interface AuditsChecklistDialogData {
  planId: string;
}

@Component({
  selector: 'qos-audits-checklist-dialog',
  templateUrl: './audits-checklist-dialog.component.html',
  styleUrls: ['./audits-checklist-dialog.component.scss'],
  standalone: false
})
export class AuditsChecklistDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    question: ['', [Validators.required]],
    clauseRef: ['', [Validators.maxLength(100)]],
    expectedEvidence: [''],
    weight: [1, [Validators.min(1)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly audits: AuditsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<AuditsChecklistDialogComponent, ChecklistItemResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AuditsChecklistDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const { question, clauseRef, expectedEvidence, weight } = this.form.getRawValue();
    this.audits
      .addChecklistItem(this.data.planId, {
        question: question.trim(),
        clauseRef: clauseRef?.trim() || undefined,
        expectedEvidence: expectedEvidence?.trim() || undefined,
        weight: weight > 0 ? weight : undefined
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: item => {
          this.snack.open('Question ajoutée.', 'OK', { duration: 2500 });
          this.dialogRef.close(item);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[audits-checklist-dialog] add failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, 'Erreur lors de l\'ajout.'),
            'OK', { duration: 4000 }
          );
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
