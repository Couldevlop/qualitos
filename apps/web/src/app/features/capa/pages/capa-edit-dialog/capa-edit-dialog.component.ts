import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { personName } from '../../../../core/directory/directory-names';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { CapaService } from '../../capa.service';
import { CapaCaseResponse, CapaCriticity } from '../../capa.types';

export interface CapaEditDialogData {
  capa: CapaCaseResponse;
}

@Component({
  selector: 'qos-capa-edit-dialog',
  templateUrl: './capa-edit-dialog.component.html',
  styleUrls: ['./capa-edit-dialog.component.scss'],
  standalone: false
})
export class CapaEditDialogComponent {
  submitting = false;

  /**
   * Le nom recopié avec le vérificateur (ADR 0091).
   *
   * <p>Le vérificateur se choisit dans l'annuaire en lecture restreinte, ouvert
   * à tout membre — pas une saisie libre : c'est un vrai compte, à qui on peut
   * notifier la tâche, et deux orthographes du même nom ne peuvent pas devenir
   * deux personnes. Au départ, le nom déjà enregistré ; ensuite, celui que
   * l'annuaire donne à la personne choisie.
   */
  private verifierName: string | undefined;

  readonly criticities: CapaCriticity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    criticity: ['MEDIUM' as CapaCriticity, [Validators.required]],
    sourceRef: ['', [Validators.maxLength(255)]],
    dueDate: [''],
    // `null` = la question n'a pas été tranchée ; c'est distinct de `false`.
    verificationRequired: [null as boolean | null],
    verificationAssigneeId: [null as string | null],
    verificationInstructions: ['', [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly capa: CapaService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CapaEditDialogComponent, CapaCaseResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CapaEditDialogData
  ) {
    this.form.patchValue({
      title: data.capa.title,
      description: data.capa.description ?? '',
      criticity: data.capa.criticity,
      sourceRef: data.capa.sourceRef ?? '',
      dueDate: data.capa.dueDate ?? '',
      verificationRequired: data.capa.verificationRequired ?? null,
      verificationAssigneeId: data.capa.verificationAssigneeId ?? null,
      verificationInstructions: data.capa.verificationInstructions ?? ''
    });
    this.verifierName = data.capa.verificationAssigneeName || undefined;
    // La question, l'apparition du bloc « à qui » et ses validateurs sont tenus
    // par le bloc partagé `qos-capa-verification-fields` (remède NG0100 compris) :
    // ici, ne pas répondre laisse la question ouverte — pas de validateur.
  }

  /**
   * Le sélecteur rend la personne choisie — son compte de CONNEXION (`sub`),
   * celui que le moteur rapproche de l'utilisateur courant (ADR 0081, 0088) —
   * et son nom, recopié avec lui.
   */
  onVerifier(user: DirectoryUser | null): void {
    this.verifierName = user ? personName(user) : undefined;
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.capa
      .updateCase(this.data.capa.id, {
        title: v.title.trim(),
        description: v.description?.trim() || undefined,
        criticity: v.criticity,
        sourceRef: v.sourceRef?.trim() || undefined,
        dueDate: v.dueDate || undefined,
        // `null` signifie « non tranché » : on ne l'envoie pas, sans quoi une
        // mise à jour muette effacerait une décision déjà prise.
        verificationRequired: v.verificationRequired ?? undefined,
        verificationAssigneeId: v.verificationRequired === true
          ? (v.verificationAssigneeId ?? undefined) : undefined,
        // Le nom part avec l'identifiant : il est recopié dans le dossier pour
        // rester lisible même si le compte disparaît de l'annuaire.
        verificationAssigneeName: v.verificationRequired === true
          ? this.nomDu(v.verificationAssigneeId) : undefined,
        verificationInstructions: v.verificationRequired === true
          ? (v.verificationInstructions?.trim() || undefined) : undefined
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: c => {
          this.snack.open($localize`:@@capa.edit.success:Cas mis à jour.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(c);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-edit] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-update:Erreur lors de la mise à jour.`),
            $localize`:@@common.ok:OK`, { duration: 4000 }
          );
        }
      });
  }

  /** Le nom du vérificateur, tel qu'il sera figé dans le dossier. */
  private nomDu(id: string | null): string | undefined {
    return id ? this.verifierName : undefined;
  }

  cancel(): void { this.dialogRef.close(); }
}
