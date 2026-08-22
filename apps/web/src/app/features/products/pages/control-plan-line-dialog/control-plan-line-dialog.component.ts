import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { FmeaService } from '../../../fmea/fmea.service';
import { FmeaItemResponse } from '../../../fmea/fmea.types';
import { ProductsService } from '../../products.service';
import {
  CharacteristicClass,
  CharacteristicType,
  ControlPlanLineView,
  InputOutput,
  ProductOperationResponse
} from '../../products.types';

export interface ControlPlanLineDialogData {
  productId: string;
  planId: string;
  line?: ControlPlanLineView;
}

/**
 * Saisie d'une ligne de control plan, au format AIAG.
 *
 * <p>Le lien vers une ligne de PFMEA est proposé mais jamais imposé : un plan se
 * remplit par passes, et refuser une ligne sans justification empêcherait de
 * commencer. En revanche, l'absence de justification se voit dans le tableau.
 */
@Component({
  selector: 'qos-control-plan-line-dialog',
  templateUrl: './control-plan-line-dialog.component.html',
  styleUrls: ['./control-plan-line-dialog.component.scss'],
  standalone: false
})
export class ControlPlanLineDialogComponent implements OnInit {

  readonly form: FormGroup;
  readonly editing: boolean;
  readonly types: CharacteristicType[] = ['PRODUCT', 'PROCESS'];
  readonly classes: CharacteristicClass[] = ['STANDARD', 'SPECIAL', 'SAFETY', 'REGULATORY'];
  readonly flows: InputOutput[] = ['INPUT', 'OUTPUT'];

  operations: ProductOperationResponse[] = [];
  fmeaItems: FmeaItemResponse[] = [];
  saving = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly service: ProductsService,
    private readonly fmea: FmeaService,
    private readonly dialogRef: MatDialogRef<ControlPlanLineDialogComponent>,
    private readonly snack: MatSnackBar,
    @Inject(MAT_DIALOG_DATA) private readonly data: ControlPlanLineDialogData
  ) {
    const line = this.data.line;
    this.editing = !!line;
    this.form = this.fb.group({
      sequenceNo: [line?.sequenceNo ?? 10, [Validators.required, Validators.min(0)]],
      operationId: [line?.operationId ?? null],
      machine: [line?.machine ?? '', Validators.maxLength(250)],
      characteristicNo: [line?.characteristicNo ?? '', Validators.maxLength(32)],
      characteristicLabel: [line?.characteristicLabel ?? '',
        [Validators.required, Validators.maxLength(500)]],
      characteristicType: [line?.characteristicType ?? 'PRODUCT', Validators.required],
      specialClass: [line?.specialClass ?? 'STANDARD'],
      specification: [line?.specification ?? '', Validators.maxLength(500)],
      toleranceLower: [line?.toleranceLower ?? null],
      toleranceUpper: [line?.toleranceUpper ?? null],
      unit: [line?.unit ?? '', Validators.maxLength(24)],
      measurementTechnique: [line?.measurementTechnique ?? '', Validators.maxLength(250)],
      sampleSize: [line?.sampleSize ?? '', Validators.maxLength(120)],
      sampleFrequency: [line?.sampleFrequency ?? '', Validators.maxLength(120)],
      controlMethod: [line?.controlMethod ?? '', Validators.maxLength(500)],
      reactionPlan: [line?.reactionPlan ?? '', Validators.maxLength(1000)],
      fmeaItemId: [line?.fmeaItemId ?? null],
      sopReference: [line?.sopReference ?? '', Validators.maxLength(64)],
      inputOutput: [line?.inputOutput ?? null],
      whoMeasures: [line?.whoMeasures ?? '', Validators.maxLength(250)],
      recordingLocation: [line?.recordingLocation ?? '', Validators.maxLength(250)]
    });
  }

  ngOnInit(): void {
    // Les deux listes ne sont que des aides à la saisie : leur échec ne doit pas
    // empêcher d'écrire une ligne.
    this.service.operations(this.data.productId).pipe(catchError(() => of([])))
      .subscribe(operations => (this.operations = operations));

    this.fmea.list(0, 50, 'ACTIVE', 'PROCESS_FMEA', this.data.productId).pipe(
      switchMap(page => page.content.length > 0
        ? this.fmea.listItems(page.content[0].id).pipe(map(items => items.content))
        : of([] as FmeaItemResponse[])),
      catchError(() => of([] as FmeaItemResponse[]))
    ).subscribe(items => (this.fmeaItems = items));
  }

  save(): void {
    if (this.form.invalid || this.saving) return;
    this.saving = true;
    const value = this.form.getRawValue();
    const payload = {
      sequenceNo: Number(value.sequenceNo),
      operationId: value.operationId || undefined,
      machine: value.machine || undefined,
      characteristicNo: value.characteristicNo || undefined,
      characteristicLabel: value.characteristicLabel,
      characteristicType: value.characteristicType as CharacteristicType,
      specialClass: value.specialClass || undefined,
      specification: value.specification || undefined,
      toleranceLower: this.numberOrUndefined(value.toleranceLower),
      toleranceUpper: this.numberOrUndefined(value.toleranceUpper),
      unit: value.unit || undefined,
      measurementTechnique: value.measurementTechnique || undefined,
      sampleSize: value.sampleSize || undefined,
      sampleFrequency: value.sampleFrequency || undefined,
      controlMethod: value.controlMethod || undefined,
      reactionPlan: value.reactionPlan || undefined,
      fmeaItemId: value.fmeaItemId || undefined,
      sopReference: value.sopReference || undefined,
      inputOutput: value.inputOutput || undefined,
      whoMeasures: value.whoMeasures || undefined,
      recordingLocation: value.recordingLocation || undefined
    };

    const request = this.editing
      ? this.service.updateLine(this.data.productId, this.data.planId, this.data.line!.id, payload)
      : this.service.addLine(this.data.productId, this.data.planId, payload);

    request.subscribe({
      next: line => this.dialogRef.close(line),
      error: err => {
        this.saving = false;
        this.snack.open(
          err?.status === 409
            ? $localize`:@@controlplan.locked:Ce plan est approuvé : ouvrez une révision pour le modifier.`
            : $localize`:@@product.save-failed:Enregistrement impossible.`,
          $localize`:@@common.ok:OK`, { duration: 5000 });
      }
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  private numberOrUndefined(value: unknown): number | undefined {
    return value === null || value === undefined || value === '' ? undefined : Number(value);
  }
}
