import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ApqpPhase, CreateApqpPhaseRequest } from '../../apqp.types';
import { nonBlank } from '../../apqp.validators';

/** Ce que la liste appelante transmet : la phase à modifier, ou rien pour créer. */
export interface ApqpPhaseDialogData {
  phase?: ApqpPhase;
}

/**
 * Créer ou renommer une phase du cycle.
 *
 * <p>Un seul dialogue pour les deux, parce que ce sont les mêmes champs : les
 * séparer aurait fait deux gabarits à tenir d'accord pour une différence qui
 * tient au titre et au libellé du bouton.
 */
@Component({
  selector: 'qos-apqp-phase-dialog',
  templateUrl: './apqp-phase-dialog.component.html',
  standalone: false
})
export class ApqpPhaseDialogComponent {

  readonly form = this.fb.nonNullable.group({
    // L'intitulé seul est obligatoire : c'est tout ce que le V affiche, et une
    // phase sans nom n'y serait plus repérable.
    title: ['', [Validators.required, nonBlank, Validators.maxLength(255)]],
    purpose: ['', [Validators.maxLength(500)]],
    question: ['', [Validators.maxLength(500)]]
  });

  readonly editing: boolean;

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<ApqpPhaseDialogComponent, CreateApqpPhaseRequest>,
    @Inject(MAT_DIALOG_DATA) readonly data: ApqpPhaseDialogData
  ) {
    this.editing = !!data?.phase;
    if (data?.phase) {
      this.form.patchValue({
        title: data.phase.title,
        purpose: data.phase.purpose ?? '',
        question: data.phase.question ?? ''
      });
    }
  }

  get dialogTitle(): string {
    return this.editing
      ? $localize`:@@apqp.phase-dialog.title-edit:Modifier la phase`
      : $localize`:@@apqp.phase-dialog.title-create:Ajouter une phase`;
  }

  get submitLabel(): string {
    return this.editing
      ? $localize`:@@common.save:Enregistrer`
      : $localize`:@@common.add:Ajouter`;
  }

  /** Ce qui empêche de valider, nommé plutôt que laissé à deviner. */
  get blockedReason(): string | undefined {
    return this.form.controls.title.invalid
      ? $localize`:@@apqp.phase-dialog.blocked-title:Donnez un intitulé : c'est ce que le schéma affiche.`
      : undefined;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { title, purpose, question } = this.form.getRawValue();
    // Un champ réduit à des espaces n'est pas une valeur : on l'omet plutôt que
    // d'enregistrer une ligne blanche que l'écran afficherait comme un vide.
    this.dialogRef.close({
      title: title.trim(),
      purpose: purpose.trim() || undefined,
      question: question.trim() || undefined
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
