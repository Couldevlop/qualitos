import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { IshikawaService } from '../../ishikawa.service';
import { IshikawaDiagramResponse, IshikawaMode } from '../../ishikawa.types';

interface ModeOption { value: IshikawaMode; label: string; hint: string; }

@Component({
  selector: 'qos-ishikawa-create-dialog',
  templateUrl: './ishikawa-create-dialog.component.html',
  styleUrls: ['./ishikawa-create-dialog.component.scss'],
  standalone: false
})
export class IshikawaCreateDialogComponent {

  submitting = false;

  readonly modes: ModeOption[] = [
    { value: 'SIX_M',   label: '6M', hint: 'Méthodes · Main-d\'œuvre · Machines · Matières · Mesures · Milieu' },
    { value: 'SEVEN_M', label: '7M', hint: '6M + Management' },
    { value: 'EIGHT_M', label: '8M', hint: '7M + Moyens financiers (Money)' }
  ];

  readonly form = this.fb.nonNullable.group({
    problemStatement: ['', [Validators.required, Validators.maxLength(500)]],
    description: [''],
    mode: ['SIX_M' as IshikawaMode, [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly ishikawa: IshikawaService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<IshikawaCreateDialogComponent, IshikawaDiagramResponse>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const ownerId = this.auth.snapshot()?.userId;
    if (!ownerId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    const { problemStatement, description, mode } = this.form.getRawValue();
    this.ishikawa
      .createDiagram({
        problemStatement: problemStatement.trim(),
        description: description?.trim() || undefined,
        mode,
        ownerId
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: diagram => {
          this.snack.open('Diagramme Ishikawa créé.', 'OK', { duration: 2500 });
          this.dialogRef.close(diagram);
        },
        error: err => {
          this.snack.open(
            err?.error?.message ?? err?.message ?? 'Erreur lors de la création',
            'OK',
            { duration: 4000 }
          );
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
