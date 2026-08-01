import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ComplaintsService } from '../../complaints.service';
import {
  COMPLAINT_CHANNELS, COMPLAINT_CHANNEL_LABEL, Complaint, ComplaintChannel,
  ComplaintResponseEntry, UUID_PATTERN
} from '../../complaints.types';

export interface ComplaintResponseDialogData {
  complaint: Complaint;
}

/**
 * Ajout d'un message au fil d'une réclamation.
 *
 * Deux natures très différentes, d'où l'avertissement à l'écran :
 *  - message au client : horodate `firstResponseAt` (prérequis à la résolution),
 *    fait basculer la réclamation en « Répondue », et devient immuable ;
 *  - note interne : ne change ni statut ni horodatage, et reste supprimable.
 */
@Component({
  selector: 'qos-complaint-response-dialog',
  templateUrl: './complaint-response-dialog.component.html',
  styleUrls: ['./complaint-response-dialog.component.scss'],
  standalone: false
})
export class ComplaintResponseDialogComponent {

  submitting = false;

  readonly channels = COMPLAINT_CHANNELS;
  readonly channelLabel = COMPLAINT_CHANNEL_LABEL;

  readonly form = this.fb.nonNullable.group({
    body: ['', [Validators.required, Validators.maxLength(4000)]],
    channel: [this.data.complaint.channel as ComplaintChannel, Validators.required],
    internalNote: [false]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ComplaintsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<ComplaintResponseDialogComponent, ComplaintResponseEntry>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ComplaintResponseDialogData
  ) {}

  /** Auteur du message : imposé par le serveur (@NotNull UUID authorUserId). */
  get authorId(): string | null {
    const id = this.auth.snapshot()?.userId;
    return id && UUID_PATTERN.test(id) ? id : null;
  }

  /** Le message part au client : conséquences à afficher avant l'envoi. */
  get customerFacing(): boolean {
    return !this.form.controls.internalNote.value;
  }

  submit(): void {
    if (this.form.invalid || this.submitting || !this.authorId) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.addResponse(this.data.complaint.id, {
      authorUserId: this.authorId,
      channel: v.channel,
      body: v.body.trim(),
      internalNote: v.internalNote
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: entry => {
          this.snack.open(
            v.internalNote
              ? $localize`:@@complaints.response.note-added:Note interne ajoutée.`
              : $localize`:@@complaints.response.sent:Réponse enregistrée.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(entry);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[complaint-response] failed', (err as { status?: number })?.status);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@complaints.response.error:Ajout impossible.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
