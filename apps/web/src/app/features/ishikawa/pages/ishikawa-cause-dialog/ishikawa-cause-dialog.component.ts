import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { IshikawaService } from '../../ishikawa.service';
import { CauseCategory, IshikawaCauseResponse, IshikawaMode } from '../../ishikawa.types';

export interface IshikawaCauseDialogData {
  diagramId: string;
  mode: IshikawaMode;
}

interface CategoryOption { value: CauseCategory; label: string; }

@Component({
  selector: 'qos-ishikawa-cause-dialog',
  templateUrl: './ishikawa-cause-dialog.component.html',
  styleUrls: ['./ishikawa-cause-dialog.component.scss'],
  standalone: false
})
export class IshikawaCauseDialogComponent {

  submitting = false;

  /** Categories filtered to what the diagram's mode actually displays. */
  readonly categories: CategoryOption[];

  readonly form = this.fb.nonNullable.group({
    category: ['METHODS' as CauseCategory, [Validators.required]],
    label: ['', [Validators.required, Validators.maxLength(500)]],
    description: [''],
    rootCauseScore: [0]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly ishikawa: IshikawaService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<IshikawaCauseDialogComponent, IshikawaCauseResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: IshikawaCauseDialogData
  ) {
    this.categories = this.categoriesFor(this.data.mode);
  }

  private categoriesFor(mode: IshikawaMode): CategoryOption[] {
    const sixM: CategoryOption[] = [
      { value: 'METHODS',      label: 'Méthodes' },
      { value: 'MANPOWER',     label: 'Main-d\'œuvre' },
      { value: 'MACHINES',     label: 'Machines' },
      { value: 'MATERIALS',    label: 'Matières' },
      { value: 'MEASUREMENTS', label: 'Mesures' },
      { value: 'ENVIRONMENT',  label: 'Milieu' }
    ];
    if (mode === 'SIX_M') return sixM;
    if (mode === 'SEVEN_M') return [...sixM, { value: 'MANAGEMENT', label: 'Management' }];
    return [...sixM, { value: 'MANAGEMENT', label: 'Management' }, { value: 'MONEY', label: 'Moyens financiers' }];
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const { category, label, description, rootCauseScore } = this.form.getRawValue();
    const score = typeof rootCauseScore === 'number' && rootCauseScore > 0 ? rootCauseScore : undefined;
    this.ishikawa
      .addCause(this.data.diagramId, {
        category,
        label: label.trim(),
        description: description?.trim() || undefined,
        rootCauseScore: score
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: cause => {
          this.snack.open('Cause ajoutée.', 'OK', { duration: 2500 });
          this.dialogRef.close(cause);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[ishikawa-cause-dialog] addCause failed', err?.status, err?.error?.title);
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
