import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { TenantPlan, TenantSummary, TenantUpdateRequest } from '../../../features/admin/admin.types';

/** Ce que la fenêtre rend : la correction à envoyer, ou rien si l'on renonce. */
export type ClientEditResult = TenantUpdateRequest;

/**
 * Corriger un client existant : son nom et son offre.
 *
 * <p>Un client se créait et ne se corrigeait plus : une raison sociale mal
 * orthographiée restait telle quelle sur tous ses documents, et un passage de
 * STARTER à PRO devait se faire en base. Les deux se font ici.
 *
 * <p>Le repère (`slug`) est montré mais pas modifiable. Il nomme le client dans
 * les adresses, les jetons et les journaux : le changer rendrait illisible tout
 * ce qui a déjà été écrit sous l'ancien, et c'est exactement ce qu'un auditeur
 * vient relire.
 *
 * <p>L'état ne se change pas ici non plus : suspendre un client coupe l'accès
 * d'un établissement entier, et cette décision se prend sur un bouton qui le
 * dit, avec une confirmation — pas sur une case au milieu d'un formulaire de
 * correction orthographique.
 */
@Component({
  selector: 'qos-client-edit-dialog',
  templateUrl: './client-edit-dialog.component.html',
  styleUrls: ['./client-edit-dialog.component.scss'],
  standalone: false
})
export class ClientEditDialogComponent {

  readonly plans: readonly TenantPlan[] = ['STARTER', 'PRO', 'ENTERPRISE'];

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    plan: ['STARTER' as TenantPlan, [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<ClientEditDialogComponent, ClientEditResult>,
    @Inject(MAT_DIALOG_DATA) public readonly client: TenantSummary
  ) {
    this.form.setValue({ name: client.name, plan: client.plan });
  }

  /** Rien à envoyer tant que rien n'a bougé : une écriture sans changement serait un acte de plus dans la piste d'audit. */
  get unchanged(): boolean {
    const v = this.form.getRawValue();
    return v.name.trim() === this.client.name && v.plan === this.client.plan;
  }

  submit(): void {
    if (this.form.invalid || this.unchanged) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.dialogRef.close({ name: v.name.trim(), plan: v.plan });
  }

  cancel(): void { this.dialogRef.close(); }
}
