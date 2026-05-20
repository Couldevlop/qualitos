import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { SuppliersService } from '../../suppliers.service';
import { SupplierResponse, SupplierStatus } from '../../suppliers.types';

export interface SuppliersStatusDialogData {
  supplier: SupplierResponse;
  target: SupplierStatus;
  reasonRequired: boolean;
}

@Component({
  selector: 'qos-suppliers-status-dialog',
  templateUrl: './suppliers-status-dialog.component.html',
  styleUrls: ['./suppliers-status-dialog.component.scss'],
  standalone: false
})
export class SuppliersStatusDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    reason: ['', this.data.reasonRequired
      ? [Validators.required, Validators.maxLength(1000)]
      : [Validators.maxLength(1000)]
    ]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: SuppliersService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<SuppliersStatusDialogComponent, SupplierResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: SuppliersStatusDialogData
  ) {}

  get title(): string {
    return ({
      PROSPECT:    'Repasser en PROSPECT',
      APPROVED:    'Approuver le fournisseur',
      CONDITIONAL: 'Mettre sous surveillance (CONDITIONAL)',
      SUSPENDED:   'Suspendre le fournisseur',
      BLACKLISTED: 'Black-lister le fournisseur'
    })[this.data.target];
  }

  get description(): string {
    return ({
      PROSPECT:    'Le fournisseur ne pourra plus être source d\'achats fermes.',
      APPROVED:    'Le fournisseur sera actif et conforme. Les commandes peuvent être passées.',
      CONDITIONAL: 'Commandes possibles mais sous surveillance renforcée.',
      SUSPENDED:   'Toutes les commandes seront gelées. Réintégration possible vers APPROVED après remédiation.',
      BLACKLISTED: 'Statut TERMINAL : le fournisseur sera exclu. Toute réintégration future passera par un nouvel onboarding avec un code différent.'
    })[this.data.target];
  }

  get danger(): boolean {
    return this.data.target === 'SUSPENDED' || this.data.target === 'BLACKLISTED';
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const actorUserId = this.auth.snapshot()?.userId;
    if (!actorUserId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.changeStatus(this.data.supplier.id, this.data.target, {
      actorUserId,
      reason: v.reason?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: s => {
          this.snack.open('Statut mis à jour : ' + s.status + '.', 'OK', { duration: 2500 });
          this.dialogRef.close(s);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[suppliers-status] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Changement de statut impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
