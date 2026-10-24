import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { normalize } from '../../competency-matrix/competency-matrix.model';
import { CompetencyMatrixService } from '../../competency-matrix/competency-matrix.service';
import { CompetencyMatrixView } from '../../competency-matrix/competency-matrix.types';

export interface CompetencySkillDialogData {
  /** Catégories à proposer : celles déjà employées, et les ateliers du client. */
  categories: string[];
  /** La catégorie proposée d'emblée (celle du filtre en cours, s'il y en a un). */
  category?: string;
}

/**
 * Ajouter une compétence à la matrice : son intitulé, sa catégorie, et le
 * FORMATEUR — la personne de l'annuaire vers qui se tourner quand la
 * compétence passe à risque. Sans formateur, pas d'ajout.
 */
@Component({
  selector: 'qos-competency-skill-dialog',
  templateUrl: './competency-skill-dialog.component.html',
  styleUrls: ['./competency-skill-dialog.component.scss'],
  standalone: false
})
export class CompetencySkillDialogComponent {

  readonly title = $localize`:@@training.matrix.add-skill-title:Ajouter une compétence`;
  readonly submitLabel = $localize`:@@training.matrix.add:Ajouter`;

  submitting = false;
  error = '';
  private trainer: DirectoryUser | null = null;

  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly service: CompetencyMatrixService,
    private readonly dialogRef: MatDialogRef<CompetencySkillDialogComponent, CompetencyMatrixView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CompetencySkillDialogData
  ) {
    this.form = this.fb.nonNullable.group({
      name: ['', [Validators.required, Validators.maxLength(200)]],
      category: [data.category ?? '', [Validators.required, Validators.maxLength(64)]],
      trainerUserId: ['', Validators.required]
    });
  }

  /** Les catégories qui correspondent à la saisie, pour l'autocomplétion. */
  get categoryOptions(): string[] {
    const q = normalize(this.form.controls.category.value ?? '');
    return this.data.categories.filter(c => !q || normalize(c).includes(q));
  }

  onTrainer(user: DirectoryUser | null): void {
    this.trainer = user;
  }

  submit(): void {
    for (const key of ['name', 'category'] as const) {
      const ctrl = this.form.controls[key];
      if (ctrl.value !== ctrl.value.trim()) ctrl.setValue(ctrl.value.trim());
    }
    if (this.form.invalid || !this.trainer || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.error = '';
    const v = this.form.getRawValue();
    this.service.addSkill({
      name: v.name, category: v.category, trainerUserId: v.trainerUserId, trainerName: this.trainer.displayName
    })
      .pipe(finalize(() => this.submitting = false))
      .subscribe({
        next: view => this.dialogRef.close(view),
        error: err => this.error = err?.status === 409
          ? $localize`:@@training.matrix.skill-exists:Cette compétence figure déjà dans la matrice.`
          : safeErrorMessage(err, $localize`:@@training.matrix.skill-failed:La compétence n'a pas pu être ajoutée.`)
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
