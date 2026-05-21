import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { EhsService } from '../../ehs.service';
import { IncidentView } from '../../ehs.types';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export interface EhsLinkDialogData {
  incidentId: string;
  kind: 'CAPA' | 'NC';
}

@Component({
  selector: 'qos-ehs-link-dialog',
  templateUrl: './ehs-link-dialog.component.html',
  styleUrls: ['./ehs-link-dialog.component.scss'],
  standalone: false
})
export class EhsLinkDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    targetId: ['', [Validators.required, Validators.pattern(UUID_REGEX)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: EhsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<EhsLinkDialogComponent, IncidentView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: EhsLinkDialogData
  ) {}

  get title(): string {
    return this.data.kind === 'CAPA'
      ? 'Lier un cas CAPA'
      : 'Lier une non-conformité';
  }

  get fieldLabel(): string {
    return this.data.kind === 'CAPA' ? 'UUID du cas CAPA' : 'UUID de la NC';
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const id = this.form.getRawValue().targetId.trim();
    const op$ = this.data.kind === 'CAPA'
      ? this.svc.linkCapa(this.data.incidentId, { capaCaseId: id })
      : this.svc.linkNc(this.data.incidentId,   { ncId: id });

    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: i => { this.snack.open('Lien créé.', 'OK', { duration: 2200 }); this.dialogRef.close(i); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[ehs-link] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Lien impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
