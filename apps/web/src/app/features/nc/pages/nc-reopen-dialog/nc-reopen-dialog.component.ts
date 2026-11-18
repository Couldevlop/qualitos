import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { NcService } from '../../nc.service';
import { NcResponse, NcStatus } from '../../nc.types';

export interface NcReopenDialogData {
  ncId: string;
  reference: string;
  /** L'état d'où l'on repart : la phrase du dialogue le nomme. */
  status: NcStatus;
}

/**
 * Reprendre un écart refermé, et dire pourquoi — si on veut le dire.
 *
 * <p>Un écart revient : la récidive se constate après la clôture, le client
 * relance, un constat avait été écarté à tort. Sans cette reprise, il fallait
 * ouvrir un <b>second</b> écart, qui perdait l'analyse de cause, les actions
 * déjà menées et l'équipe du premier, et comptait deux fois le même problème
 * dans les indicateurs.
 *
 * <p>Le motif est <b>facultatif</b>, et c'est un choix. Il a son prix : sans
 * lui, la fiche montre un retour en arrière que personne ne sait expliquer six
 * mois plus tard. Mais l'exiger bloquerait le geste même, et un écart qui
 * revient doit pouvoir être repris tout de suite — on écrira pourquoi ensuite.
 * Le dialogue invite donc, il ne réclame pas.
 *
 * <p>Une fenêtre plutôt qu'une simple confirmation, pour la même raison que le
 * rejet : il y a quelque chose à écrire. Mais son bouton n'est jamais barré.
 */
@Component({
  selector: 'qos-nc-reopen-dialog',
  templateUrl: './nc-reopen-dialog.component.html',
  styleUrls: ['./nc-reopen-dialog.component.scss'],
  standalone: false
})
export class NcReopenDialogComponent {

  submitting = false;

  // Pas de `Validators.required` : voir la raison dans la documentation de la
  // classe. Seule la longueur est bornée, comme au serveur.
  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly nc: NcService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<NcReopenDialogComponent, NcResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: NcReopenDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const reason = this.form.getRawValue().reason.trim();
    this.submitting = true;
    this.nc
      // La chaîne vide part telle quelle : le serveur la range comme « pas de
      // motif », et non comme un motif blanc.
      .reopen(this.data.ncId, { reason })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: reprise => {
          this.snack.open(
            $localize`:@@nc.reopen.success:Écart repris : l'instruction reprend à son début.`,
            $localize`:@@common.ok:OK`, { duration: 3000 });
          this.dialogRef.close(reprise);
        },
        error: err => {
          // La fenêtre reste ouverte : le motif écrit ne se perd pas sur un
          // refus de transition.
          this.snack.open(
            safeErrorMessage(err, $localize`:@@nc.reopen.error:Reprise impossible.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
