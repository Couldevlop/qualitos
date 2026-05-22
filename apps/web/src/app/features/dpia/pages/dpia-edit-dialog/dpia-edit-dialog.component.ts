import { Component, Inject } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { DpiaService } from '../../dpia.service';
import { DpiaView, RiskLevel } from '../../dpia.types';

export interface DpiaEditDialogData { dpia: DpiaView; }

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function linesToList(input?: string | null): string[] {
  if (!input) return [];
  const set = new Set<string>();
  for (const line of input.split('\n')) {
    const v = line.trim();
    if (v) set.add(v);
  }
  return Array.from(set);
}
function listToLines(arr?: string[] | null): string { return (arr ?? []).join('\n'); }

function uuidLinesValidator(): ValidatorFn {
  return (ctrl: AbstractControl): ValidationErrors | null => {
    const ids = linesToList(ctrl.value);
    for (const id of ids) if (!UUID_REGEX.test(id)) return { uuidLine: id };
    return null;
  };
}

@Component({
  selector: 'qos-dpia-edit-dialog',
  templateUrl: './dpia-edit-dialog.component.html',
  styleUrls: ['./dpia-edit-dialog.component.scss'],
  standalone: false
})
export class DpiaEditDialogComponent {

  submitting = false;

  readonly risks: RiskLevel[] = ['LOW', 'MEDIUM', 'HIGH', 'SEVERE'];

  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DpiaService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DpiaEditDialogComponent, DpiaView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: DpiaEditDialogData
  ) {
    const d = data.dpia;
    this.form = this.fb.nonNullable.group({
      title:       [d.title,                [Validators.required, Validators.maxLength(250)]],
      description: [d.description ?? '',    [Validators.maxLength(4000)]],
      linkedProcessingActivityIds: [listToLines(d.linkedProcessingActivityIds), [uuidLinesValidator()]],
      necessityAndProportionalityNotes: [d.necessityAndProportionalityNotes ?? '', [Validators.maxLength(8000)]],
      risksToRightsAndFreedoms:         [d.risksToRightsAndFreedoms ?? '',         [Validators.maxLength(8000)]],
      mitigationMeasures:               [d.mitigationMeasures ?? '',               [Validators.maxLength(8000)]],
      overallRiskLevel:     [d.overallRiskLevel, [Validators.required]],
      consultationRequired: [d.consultationRequired],
      consultationNotes:    [d.consultationNotes ?? '', [Validators.maxLength(8000)]]
    });

    // OWASP A04 — couplage automatique : HIGH/SEVERE force consultationRequired=true.
    this.form.controls.overallRiskLevel.valueChanges.subscribe(level => {
      if (DpiaService.requiresPriorConsultation(level)) {
        this.form.controls.consultationRequired.setValue(true);
      }
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();

    // OWASP A04 — refuser de désactiver la consultation si le risque l'impose.
    if (DpiaService.requiresPriorConsultation(v.overallRiskLevel) && !v.consultationRequired) {
      this.snack.open('Le risque HIGH/SEVERE impose la consultation préalable Art. 36 — case obligatoire.',
        'OK', { duration: 4500 });
      return;
    }

    this.submitting = true;
    this.svc.edit(this.data.dpia.id, {
      title: v.title.trim(),
      description: v.description?.trim() || undefined,
      linkedProcessingActivityIds: linesToList(v.linkedProcessingActivityIds),
      necessityAndProportionalityNotes: v.necessityAndProportionalityNotes?.trim() || undefined,
      risksToRightsAndFreedoms:         v.risksToRightsAndFreedoms?.trim()         || undefined,
      mitigationMeasures:               v.mitigationMeasures?.trim()               || undefined,
      overallRiskLevel: v.overallRiskLevel,
      consultationRequired: v.consultationRequired,
      consultationNotes: v.consultationNotes?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: d => { this.snack.open('DPIA mise à jour.', 'OK', { duration: 2500 }); this.dialogRef.close(d); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[dpia-edit] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de la mise à jour.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
