import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';
import { Observable } from 'rxjs';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { FriaService } from '../../fria.service';
import { FriaView } from '../../fria.types';

export interface FriaReasonDialogData {
  id: string;
  mode: 'RETURN' | 'ARCHIVE';
}

@Component({
  selector: 'qos-fria-reason-dialog',
  templateUrl: './fria-reason-dialog.component.html',
  styleUrls: ['./fria-reason-dialog.component.scss'],
  standalone: false
})
export class FriaReasonDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(this.data.mode === 'RETURN' ? 4000 : 2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: FriaService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<FriaReasonDialogComponent, FriaView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: FriaReasonDialogData
  ) {}

  get title(): string {
    return this.data.mode === 'RETURN'
      ? 'Retourner la FRIA au brouillon' : 'Archiver la FRIA';
  }

  get hint(): string {
    return this.data.mode === 'RETURN'
      ? 'La FRIA repasse en DRAFT. Le motif sera consigné dans les notes d\'approbation.'
      : 'Action terminale. La FRIA passe en ARCHIVED — fin du déploiement de l\'IA évalué.';
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const reason = this.form.getRawValue().reason.trim();
    const op$: Observable<FriaView> = this.data.mode === 'RETURN'
      ? this.svc.returnToDraft(this.data.id, { reason })
      : this.svc.archive(this.data.id, { reason });
    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: f => { this.snack.open(this.data.mode === 'RETURN' ? 'FRIA renvoyée en brouillon.' : 'FRIA archivée.',
                                     'OK', { duration: 2200 }); this.dialogRef.close(f); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fria-reason] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Opération impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
