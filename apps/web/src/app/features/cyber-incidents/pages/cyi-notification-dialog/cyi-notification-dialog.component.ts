import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';
import { Observable } from 'rxjs';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { CyberIncidentsService } from '../../cyi.service';
import { CyiView } from '../../cyi.types';

export type CyiNotificationMode = 'EARLY_WARNING' | 'INITIAL_ASSESSMENT' | 'FINAL_REPORT';

export interface CyiNotificationDialogData {
  id: string;
  mode: CyiNotificationMode;
}

@Component({
  selector: 'qos-cyi-notification-dialog',
  templateUrl: './cyi-notification-dialog.component.html',
  styleUrls: ['./cyi-notification-dialog.component.scss'],
  standalone: false
})
export class CyiNotificationDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    sentAt:    [new Date().toISOString().slice(0, 16), [Validators.required]],
    reference: ['', [Validators.required, Validators.maxLength(250)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: CyberIncidentsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CyiNotificationDialogComponent, CyiView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CyiNotificationDialogData
  ) {}

  get title(): string {
    switch (this.data.mode) {
      case 'EARLY_WARNING':      return 'Enregistrer l\'alerte CSIRT 24h';
      case 'INITIAL_ASSESSMENT': return 'Enregistrer l\'évaluation initiale 72h';
      case 'FINAL_REPORT':       return 'Enregistrer le rapport final 1 mois';
    }
  }

  get hint(): string {
    switch (this.data.mode) {
      case 'EARLY_WARNING':      return 'Alerte initiale CSIRT — délai 24h depuis la détection.';
      case 'INITIAL_ASSESSMENT': return 'Évaluation initiale détaillée — délai 72h depuis la détection.';
      case 'FINAL_REPORT':       return 'Rapport final post-incident — délai 1 mois depuis la détection.';
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    if (new Date(v.sentAt).getTime() > Date.now()) {
      this.snack.open('La date d\'envoi ne peut pas être dans le futur.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    const payload = { sentAt: new Date(v.sentAt).toISOString(), reference: v.reference.trim() };
    const op$: Observable<CyiView> =
      this.data.mode === 'EARLY_WARNING'      ? this.svc.recordEarlyWarning(this.data.id, payload) :
      this.data.mode === 'INITIAL_ASSESSMENT' ? this.svc.recordInitialAssessment(this.data.id, payload) :
                                                this.svc.recordFinalReport(this.data.id, payload);
    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: i => { this.snack.open('Notification enregistrée.', 'OK', { duration: 2200 }); this.dialogRef.close(i); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[cyi-notif] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Enregistrement impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
