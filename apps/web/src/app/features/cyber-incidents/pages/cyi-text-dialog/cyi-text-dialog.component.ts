import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';
import { Observable } from 'rxjs';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { CyberIncidentsService } from '../../cyi.service';
import { CyiView } from '../../cyi.types';

export interface CyiTextDialogData {
  id: string;
  mode: 'CLOSE' | 'REJECT';
}

@Component({
  selector: 'qos-cyi-text-dialog',
  templateUrl: './cyi-text-dialog.component.html',
  styleUrls: ['./cyi-text-dialog.component.scss'],
  standalone: false
})
export class CyiTextDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    text: ['', this.data.mode === 'REJECT'
        ? [Validators.required, Validators.maxLength(2000)]
        : [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: CyberIncidentsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CyiTextDialogComponent, CyiView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CyiTextDialogData
  ) {}

  get title(): string {
    return this.data.mode === 'CLOSE' ? 'Clôturer l\'incident' : 'Rejeter l\'incident (faux positif)';
  }

  get hint(): string {
    return this.data.mode === 'CLOSE'
      ? 'Clôture définitive — l\'incident reste consultable mais ne peut plus changer d\'état.'
      : 'Rejet définitif — utilise pour un faux positif, un doublon, ou hors champ NIS 2.';
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const text = this.form.getRawValue().text.trim();
    const op$: Observable<CyiView> = this.data.mode === 'CLOSE'
      ? this.svc.close(this.data.id, { closureNotes: text || undefined })
      : this.svc.reject(this.data.id, { reason: text });
    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: i => { this.snack.open(this.data.mode === 'CLOSE' ? 'Incident clôturé.' : 'Incident rejeté.',
                                     'OK', { duration: 2200 }); this.dialogRef.close(i); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[cyi-text] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Opération impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
