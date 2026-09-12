import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { NcService } from '../../nc.service';
import { NcResponse } from '../../nc.types';

export interface NcRejectDialogData {
  ncId: string;
  reference: string;
}

/**
 * Écarter une réclamation : le motif, et rien d'autre.
 *
 * <p>Un dialogue de saisie plutôt qu'une simple confirmation. Rejeter n'est pas
 * annuler : on affirme avoir instruit la réclamation et ne pas la retenir, et
 * c'est cette phrase que le client lira et que l'auditeur demandera. Sans motif,
 * le geste ne se défend pas — d'où l'obligation, tenue ici comme au serveur.
 */
@Component({
  selector: 'qos-nc-reject-dialog',
  templateUrl: './nc-reject-dialog.component.html',
  styleUrls: ['./nc-reject-dialog.component.scss'],
  standalone: false
})
export class NcRejectDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly nc: NcService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<NcRejectDialogComponent, NcResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: NcRejectDialogData
  ) {}

  /**
   * Pourquoi le bouton est barré, s'il l'est.
   *
   * <p>`Validators.required` laisse passer des espaces : on vérifie le contenu
   * réel, sinon le serveur refuserait un motif que l'écran a cru valide.
   */
  get blockedReason(): string | undefined {
    return this.form.getRawValue().reason.trim().length === 0
      ? $localize`:@@nc.reject.blocked:Dites pourquoi la réclamation n'est pas retenue.`
      : undefined;
  }

  submit(): void {
    const reason = this.form.getRawValue().reason.trim();
    if (this.form.invalid || reason.length === 0 || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.nc
      .reject(this.data.ncId, { reason })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: rejetee => {
          this.snack.open(
            $localize`:@@nc.reject.success:Réclamation rejetée.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(rejetee);
        },
        error: err => {
          // Le dialogue reste ouvert : la saisie est trop coûteuse pour être
          // perdue sur un refus de transition.
          this.snack.open(
            safeErrorMessage(err, $localize`:@@nc.reject.error:Rejet impossible.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
