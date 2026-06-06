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
      ? $localize`:@@fria.reason.title-return:Retourner la FRIA au brouillon`
      : $localize`:@@fria.reason.title-archive:Archiver la FRIA`;
  }

  get hint(): string {
    return this.data.mode === 'RETURN'
      ? $localize`:@@fria.reason.hint-return:La FRIA repasse en DRAFT. Le motif sera consigné dans les notes d'approbation.`
      : $localize`:@@fria.reason.hint-archive:Action terminale. La FRIA passe en ARCHIVED — fin du déploiement de l'IA évalué.`;
  }

  get reasonLabel(): string {
    return this.data.mode === 'RETURN'
      ? $localize`:@@fria.reason.label-return:Motif du retour`
      : $localize`:@@fria.reason.label-archive:Motif de l'archivage`;
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
        next: f => { this.snack.open(this.data.mode === 'RETURN'
                                       ? $localize`:@@fria.reason.returned:FRIA renvoyée en brouillon.`
                                       : $localize`:@@fria.reason.archived:FRIA archivée.`,
                                     $localize`:@@common.ok:OK`, { duration: 2200 }); this.dialogRef.close(f); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fria-reason] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@fria.reason.op-failed:Opération impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
