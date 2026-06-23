import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { RecordImpactRequest } from '../../circles.types';

export interface CirclesImpactDialogData {
  proposalTitle: string;
}

@Component({
  selector: 'qos-circles-impact-dialog',
  templateUrl: './circles-impact-dialog.component.html',
  styleUrls: ['./circles-impact-dialog.component.scss'],
  standalone: false
})
export class CirclesImpactDialogComponent {

  readonly form = this.fb.nonNullable.group({
    impactNote: ['', [Validators.required, Validators.minLength(5)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<CirclesImpactDialogComponent, RecordImpactRequest>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CirclesImpactDialogData
  ) {}

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    this.dialogRef.close({ impactNote: v.impactNote.trim() });
  }

  cancel(): void { this.dialogRef.close(); }
}
