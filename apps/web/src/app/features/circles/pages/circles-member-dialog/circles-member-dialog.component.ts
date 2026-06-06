import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { CirclesService } from '../../circles.service';
import { CircleMemberResponse, CircleRole } from '../../circles.types';

export interface CirclesMemberDialogData {
  circleId: string;
}

// OWASP A03 — userId must be a UUID before we POST it. Mirror of the
// route-param guard used elsewhere.
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-circles-member-dialog',
  templateUrl: './circles-member-dialog.component.html',
  styleUrls: ['./circles-member-dialog.component.scss'],
  standalone: false
})
export class CirclesMemberDialogComponent {

  submitting = false;

  readonly roles: { value: CircleRole; label: string }[] = [
    { value: 'FACILITATOR', label: $localize`:@@circles.member.role-facilitator:Animateur` },
    { value: 'SECRETARY',   label: $localize`:@@circles.member.role-secretary:Secrétaire` },
    { value: 'MEMBER',      label: $localize`:@@circles.member.role-member:Membre` }
  ];

  readonly form = this.fb.nonNullable.group({
    userId: ['', [Validators.required, Validators.pattern(UUID_RE)]],
    role: ['MEMBER' as CircleRole, [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly circles: CirclesService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CirclesMemberDialogComponent, CircleMemberResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CirclesMemberDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const { userId, role } = this.form.getRawValue();
    this.circles
      .addMember(this.data.circleId, { userId: userId.trim(), role })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: member => {
          this.snack.open($localize`:@@circles.member.added:Membre ajouté.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(member);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[circles-member-dialog] addMember failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@circles.member.error:Erreur lors de l'ajout.`),
            'OK', { duration: 4000 }
          );
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
