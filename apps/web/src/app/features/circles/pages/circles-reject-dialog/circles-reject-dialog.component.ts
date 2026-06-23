import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface CirclesRejectDialogData {
  proposalTitle: string;
}

@Component({
  selector: 'qos-circles-reject-dialog',
  templateUrl: './circles-reject-dialog.component.html',
  styleUrls: ['./circles-reject-dialog.component.scss'],
  standalone: false
})
export class CirclesRejectDialogComponent {

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.minLength(5)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<CirclesRejectDialogComponent, string>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CirclesRejectDialogData
  ) {}

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.dialogRef.close(this.form.getRawValue().reason.trim());
  }

  cancel(): void { this.dialogRef.close(); }
}
