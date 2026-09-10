import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';

import { nonBlank } from '../../../apqp/apqp.validators';
import { SubmitIdeaRequest } from '../../ideas.types';

/**
 * Déposer une idée.
 *
 * <p>Le titre seul est obligatoire : c'est ce que chaque colonne affiche, et
 * une idée sans titre n'y serait plus repérable. Le cercle n'a pas de champ
 * ici — aucun écran de la boîte à idées ne liste encore les cercles auxquels
 * la rattacher — mais le service et le type l'acceptent déjà pour le jour où
 * ce contexte existera.
 */
@Component({
  selector: 'qos-idea-dialog',
  templateUrl: './idea-dialog.component.html',
  standalone: false
})
export class IdeaDialogComponent {

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, nonBlank, Validators.maxLength(255)]],
    description: ['', [Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<IdeaDialogComponent, SubmitIdeaRequest>
  ) {}

  /** Ce qui empêche de valider, nommé plutôt que laissé à deviner. */
  get blockedReason(): string | undefined {
    return this.form.controls.title.invalid
      ? $localize`:@@ideas.dialog.blocked-title:Donnez un titre : c'est ce que chaque colonne affiche.`
      : undefined;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { title, description } = this.form.getRawValue();
    // Une description réduite à des espaces n'est pas une valeur : on l'omet
    // plutôt que d'enregistrer une ligne blanche que l'écran afficherait
    // comme un vide.
    this.dialogRef.close({
      title: title.trim(),
      description: description.trim() || undefined
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
