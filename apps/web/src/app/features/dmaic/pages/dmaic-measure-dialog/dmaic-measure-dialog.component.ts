import { Component, Inject } from '@angular/core';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { concat } from 'rxjs';
import { finalize, tap, toArray } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { DmaicService } from '../../dmaic.service';
import { MeasureResponse } from '../../dmaic.types';

export interface DmaicMeasureDialogData {
  projectId: string;
}

/**
 * Saisit une ou plusieurs mesures d'une même série : chaque valeur devient une
 * mesure, avec la même personne, la même date et la même référence. La personne
 * qui mesure se choisit dans l'annuaire ; par défaut, soi-même.
 */
@Component({
  selector: 'qos-dmaic-measure-dialog',
  templateUrl: './dmaic-measure-dialog.component.html',
  styleUrls: ['./dmaic-measure-dialog.component.scss'],
  standalone: false
})
export class DmaicMeasureDialogComponent {

  /** Une série se saisit en une fois, dans une limite raisonnable pour un formulaire. */
  readonly maxValues = 50;

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    values: this.fb.array([this.valueControl()]),
    operatorId: [this.auth.snapshot()?.userId ?? null as string | null, [Validators.required]],
    sourceRef:  ['', [Validators.maxLength(255)]],
    recordedAt: [''],
    note: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DmaicService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DmaicMeasureDialogComponent, MeasureResponse[]>,
    @Inject(MAT_DIALOG_DATA) public readonly data: DmaicMeasureDialogData
  ) {}

  get values() { return this.form.controls.values; }

  addValue(): void {
    if (this.values.length < this.maxValues) this.values.push(this.valueControl());
  }

  removeValue(index: number): void {
    if (this.values.length > 1) this.values.removeAt(index);
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    // La valeur est facultative : les cases vides sont ignorées, et un relevé
    // sans aucune valeur s'enregistre quand même, une fois, sans valeur.
    const filled = v.values.filter((x): x is number => x !== null && Number.isFinite(x));
    const values: (number | null)[] = filled.length ? filled : [null];
    this.submitting = true;
    const done: MeasureResponse[] = [];
    // Les mesures partent dans l'ordre de saisie : c'est l'ordre du calcul de capabilité.
    concat(...values.map(value => this.svc.addMeasure(this.data.projectId, {
      value,
      sourceRef:  v.sourceRef?.trim()  || undefined,
      recordedAt: v.recordedAt || undefined,
      operatorId: v.operatorId ?? undefined,
      note: v.note?.trim() || undefined
    })))
      .pipe(tap(m => done.push(m)), toArray(), finalize(() => (this.submitting = false)))
      .subscribe({
        next: saved => {
          this.snack.open(saved.length > 1
            ? $localize`:@@dmaic.measure.saved-many:${saved.length}:count: mesures enregistrées.`
            : $localize`:@@dmaic.measure.saved:Mesure enregistrée.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(saved);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[dmaic-measure] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@dmaic.measure.error-save:Erreur lors de l'enregistrement.`), $localize`:@@common.ok:OK`, { duration: 4000 });
          // Des valeurs sont déjà parties : on les rend, pour ne pas les ressaisir en double.
          if (done.length) this.dialogRef.close(done);
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }

  private valueControl(): FormControl<number | null> {
    return new FormControl<number | null>(null);
  }
}
