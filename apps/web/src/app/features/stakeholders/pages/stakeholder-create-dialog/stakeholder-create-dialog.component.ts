import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { categoryLabel, sideLabel } from '../../stakeholders.labels';
import { StakeholdersService } from '../../stakeholders.service';
import {
  STAKEHOLDER_CATEGORIES,
  STAKEHOLDER_LIMITS,
  StakeholderCategory,
  StakeholderSheet,
  StakeholderSide
} from '../../stakeholders.types';

/** Ce que la fenêtre rend : la fiche créée, que l'hôte ouvre aussitôt. */
export interface StakeholderCreated {
  sheet: StakeholderSheet;
}

/**
 * Inscrire une partie intéressée : **trois champs**, et rien de plus.
 *
 * <p>Qui, de quelle famille, dedans ou dehors. Tout le reste — besoins,
 * pertinence, cotation, exigences — s'analyse dans la fiche, qui s'ouvre
 * immédiatement après. Demander les vingt champs de la fiche ici ferait d'une
 * inscription de dix secondes un formulaire qu'on remet à plus tard, et le
 * registre resterait vide.
 */
@Component({
  selector: 'qos-stakeholder-create-dialog',
  templateUrl: './stakeholder-create-dialog.component.html',
  styleUrls: ['./stakeholder-create-dialog.component.scss'],
  standalone: false
})
export class StakeholderCreateDialogComponent {

  readonly categories = STAKEHOLDER_CATEGORIES;
  readonly limits = STAKEHOLDER_LIMITS;

  readonly categoryLabel = categoryLabel;
  readonly sideLabel = sideLabel;

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(STAKEHOLDER_LIMITS.name)]],
    category: ['CUSTOMERS' as StakeholderCategory, [Validators.required]],
    side: ['EXTERNAL' as StakeholderSide, [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: StakeholdersService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<StakeholderCreateDialogComponent, StakeholderCreated>
  ) {}

  /**
   * Ce qui empêche d'inscrire, dit en clair : un bouton grisé sans explication
   * laisse l'utilisateur chercher le champ fautif.
   */
  get blockedReason(): string | undefined {
    if (this.form.controls.name.hasError('required')) {
      return $localize`:@@stakeholders.create.need-name:Donnez le nom ou la raison sociale de la partie prenante.`;
    }
    if (this.form.controls.name.hasError('maxlength')) {
      return $localize`:@@stakeholders.create.name-too-long:Le nom dépasse la longueur acceptée.`;
    }
    return undefined;
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const { name, category, side } = this.form.getRawValue();
    this.svc.create({ name: name.trim(), category, side })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: sheet => {
          this.snack.open($localize`:@@stakeholders.create.done:Partie prenante inscrite ; complétez son analyse.`,
            undefined, { duration: 3000 });
          this.dialogRef.close({ sheet });
        },
        error: err => this.snack.open(
          safeErrorMessage(err, $localize`:@@stakeholders.create.failed:La partie prenante n'a pas pu être inscrite.`),
          $localize`:@@common.ok:OK`, { duration: 5000 })
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
