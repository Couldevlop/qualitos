import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ApqpProject, ApqpProjectType, CreateApqpProjectRequest } from '../../apqp.types';
import { nonBlank } from '../../apqp.validators';

/** Ce que la liste transmet : le projet à modifier, ou rien pour en créer un. */
export interface ApqpProjectDialogData {
  project?: ApqpProject;
}

/**
 * Créer ou modifier un projet APQP.
 *
 * <p>Un seul dialogue pour les deux, parce que ce sont les mêmes champs : les
 * séparer aurait fait deux gabarits à tenir d'accord pour une différence qui
 * tient au titre et au libellé du bouton.
 *
 * <p>Le TYPE est obligatoire : il dit pourquoi le projet existe — lancement,
 * transfert, nouveau client — et c'est la seule chose qui permette de comparer
 * deux projets entre eux. Le client et la référence restent facultatifs : un
 * projet interne n'a ni l'un ni l'autre.
 */
@Component({
  selector: 'qos-apqp-project-dialog',
  templateUrl: './apqp-project-dialog.component.html',
  standalone: false
})
export class ApqpProjectDialogComponent {

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, nonBlank, Validators.maxLength(255)]],
    type: ['NPI' as ApqpProjectType, [Validators.required]],
    customer: ['', [Validators.maxLength(255)]],
    reference: ['', [Validators.maxLength(120)]],
    description: ['', [Validators.maxLength(2000)]]
  });

  readonly types: { value: ApqpProjectType; label: string }[] = [
    { value: 'NPI', label: $localize`:@@apqp.project.type.npi:NPI — nouveau produit` },
    { value: 'TOW', label: $localize`:@@apqp.project.type.tow:ToW — transfert d'activité` },
    { value: 'MAJOR_MODIFICATION', label: $localize`:@@apqp.project.type.major-modification:Modification majeure` },
    { value: 'OTHER', label: $localize`:@@apqp.project.type.other:Autre` }
  ];

  readonly editing: boolean;

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef:
      MatDialogRef<ApqpProjectDialogComponent, CreateApqpProjectRequest>,
    @Inject(MAT_DIALOG_DATA) readonly data: ApqpProjectDialogData
  ) {
    this.editing = !!data?.project;
    if (data?.project) {
      this.form.patchValue({
        name: data.project.name,
        type: data.project.type,
        customer: data.project.customer ?? '',
        reference: data.project.reference ?? '',
        description: data.project.description ?? ''
      });
    }
  }

  get dialogTitle(): string {
    return this.editing
      ? $localize`:@@apqp.project-dialog.title-edit:Modifier le projet`
      : $localize`:@@apqp.project-dialog.title-create:Nouveau projet APQP`;
  }

  get submitLabel(): string {
    return this.editing
      ? $localize`:@@common.save:Enregistrer`
      : $localize`:@@common.add:Ajouter`;
  }

  /** Ce qui empêche de valider, nommé plutôt que laissé à deviner. */
  get blockedReason(): string | undefined {
    return this.form.controls.name.invalid
      ? $localize`:@@apqp.project-dialog.blocked-name:Donnez un nom au projet : c'est ce que la liste affiche.`
      : undefined;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const saisie = this.form.getRawValue();
    // Un champ réduit à des espaces n'est pas une valeur : on l'omet plutôt que
    // d'enregistrer une ligne blanche que l'écran afficherait comme un vide.
    this.dialogRef.close({
      name: saisie.name.trim(),
      type: saisie.type,
      customer: saisie.customer.trim() || null,
      reference: saisie.reference.trim() || null,
      description: saisie.description.trim() || null
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
