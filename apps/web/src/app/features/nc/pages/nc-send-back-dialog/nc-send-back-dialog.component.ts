import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { NcService } from '../../nc.service';
import { NcResponse } from '../../nc.types';

export interface NcSendBackDialogData {
  ncId: string;
  reference: string;
}

/**
 * RENVOYER LE TRAITEMENT AU PILOTE : la seconde réponse du second regard.
 *
 * <p>La personne qui valide un écart résolu n'avait qu'une réponse — clore. Si
 * l'action corrective ne répondait pas à la cause, si une preuve manquait, si le
 * rapport se trompait, elle n'avait aucun geste pour le dire : on téléphonait au
 * pilote, et le registre gardait un écart « résolu » que personne ne clôturait,
 * sans qu'aucune fiche ne porte la raison de cette attente.
 *
 * <p>L'écart revient à <b>action définie</b>, et non à son début : le renvoi dit
 * que le traitement n'est pas encore bon, pas que l'instruction est à refaire.
 * L'analyse de cause, l'équipe, les actions et les preuves restent en place, et
 * le pilote résout à nouveau une fois corrigé.
 *
 * <p>Le message est <b>facultatif</b>, et c'est un choix. L'exiger
 * découragerait le renvoi lui-même, et un renvoi sans phrase vaut mieux qu'une
 * clôture de complaisance. Le dialogue invite donc, il ne réclame pas.
 */
@Component({
  selector: 'qos-nc-send-back-dialog',
  templateUrl: './nc-send-back-dialog.component.html',
  styleUrls: ['./nc-send-back-dialog.component.scss'],
  standalone: false
})
export class NcSendBackDialogComponent {

  submitting = false;

  // Pas de `Validators.required` : voir la raison dans la documentation de la
  // classe. Seule la longueur est bornée, comme au serveur.
  readonly form = this.fb.nonNullable.group({
    message: ['', [Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly nc: NcService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<NcSendBackDialogComponent, NcResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: NcSendBackDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const message = this.form.getRawValue().message.trim();
    this.submitting = true;
    this.nc
      // La chaîne vide part telle quelle : le serveur la range comme « pas de
      // message », et non comme un message blanc.
      .sendBack(this.data.ncId, { message })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: renvoi => {
          this.snack.open(
            $localize`:@@nc.send-back.success:Traitement renvoyé au pilote : l'écart redevient modifiable.`,
            $localize`:@@common.ok:OK`, { duration: 3000 });
          this.dialogRef.close(renvoi);
        },
        error: err => {
          // La fenêtre reste ouverte : le message écrit ne se perd pas sur un
          // refus de transition.
          this.snack.open(
            safeErrorMessage(err, $localize`:@@nc.send-back.error:Renvoi impossible.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
