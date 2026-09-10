import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { nonBlank } from '../../../apqp/apqp.validators';

/** Ce que l'appelant transmet : le titre de l'idée concernée, la question à poser, et le libellé du bouton. */
export interface IdeaRejectDialogData {
  title: string;
  prompt: string;
  submitLabel: string;
}

/** Ce que le dialogue rend : le texte saisi. L'appelant le traduit en la requête qui lui convient. */
export interface IdeaRejectDialogResult {
  text: string;
}

/**
 * Un seul champ libre obligatoire, sous deux habits : le refus d'une idée et
 * la consignation de son impact une fois réalisée.
 *
 * <p>Deux dialogues jumeaux pour un même champ de texte se désynchroniseraient
 * à la première retouche — c'est la donnée d'entrée qui distingue les deux
 * usages, pas le code.
 */
@Component({
  selector: 'qos-idea-reject-dialog',
  templateUrl: './idea-reject-dialog.component.html',
  standalone: false
})
export class IdeaRejectDialogComponent {

  readonly form = this.fb.nonNullable.group({
    text: ['', [Validators.required, nonBlank, Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef:
      MatDialogRef<IdeaRejectDialogComponent, IdeaRejectDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: IdeaRejectDialogData
  ) {}

  /** Ce qui empêche de valider, nommé plutôt que laissé à deviner. */
  get blockedReason(): string | undefined {
    return this.form.controls.text.invalid
      ? $localize`:@@ideas.reject-dialog.blocked:Ce champ est requis pour valider.`
      : undefined;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close({ text: this.form.getRawValue().text.trim() });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
