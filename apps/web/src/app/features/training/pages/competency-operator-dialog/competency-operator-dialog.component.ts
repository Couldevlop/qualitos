import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { CompetencyMatrixService } from '../../competency-matrix/competency-matrix.service';
import { CompetencyMatrixView } from '../../competency-matrix/competency-matrix.types';

export interface CompetencyOperatorDialogData {
  /** Les ateliers du client, dans leur ordre. */
  workshops: string[];
  /** L'atelier proposé d'emblée (celui du filtre en cours, s'il y en a un). */
  workshop?: string;
  /** Les comptes déjà inscrits : l'annuaire ne les repropose pas. */
  excludeIds: string[];
}

/**
 * Inscrire une personne dans la matrice : un compte de l'annuaire, jamais un
 * nom tapé, et son atelier. Rend la matrice entière, relue par le serveur.
 */
@Component({
  selector: 'qos-competency-operator-dialog',
  templateUrl: './competency-operator-dialog.component.html',
  styleUrls: ['./competency-operator-dialog.component.scss'],
  standalone: false
})
export class CompetencyOperatorDialogComponent {

  readonly title = $localize`:@@training.matrix.add-operator-title:Ajouter un opérateur`;
  readonly submitLabel = $localize`:@@training.matrix.add:Ajouter`;

  submitting = false;
  error = '';
  private person: DirectoryUser | null = null;

  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly service: CompetencyMatrixService,
    private readonly dialogRef: MatDialogRef<CompetencyOperatorDialogComponent, CompetencyMatrixView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CompetencyOperatorDialogData
  ) {
    const initial = data.workshop && data.workshops.includes(data.workshop) ? data.workshop : data.workshops[0] ?? '';
    this.form = this.fb.nonNullable.group({
      userId: ['', Validators.required],
      workshop: [initial, Validators.required]
    });
  }

  onPerson(user: DirectoryUser | null): void {
    this.person = user;
  }

  submit(): void {
    if (this.form.invalid || !this.person || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.error = '';
    const v = this.form.getRawValue();
    this.service.addOperator({ userId: v.userId, userName: this.person.displayName, workshop: v.workshop })
      .pipe(finalize(() => this.submitting = false))
      .subscribe({
        next: view => this.dialogRef.close(view),
        error: err => this.error = err?.status === 409
          ? $localize`:@@training.matrix.operator-exists:Cette personne figure déjà dans la matrice.`
          : safeErrorMessage(err, $localize`:@@training.matrix.operator-failed:L'opérateur n'a pas pu être ajouté.`)
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
