import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { TrainingService } from '../../training.service';
import { EnrollmentResponse, PathResponse, TrainingPathStatus } from '../../training.types';

@Component({
  selector: 'qos-training-enroll-dialog',
  templateUrl: './training-enroll-dialog.component.html',
  styleUrls: ['./training-enroll-dialog.component.scss'],
  standalone: false
})
export class TrainingEnrollDialogComponent implements OnInit {

  submitting = false;
  loading = true;
  loadError: string | null = null;

  readonly dialogTitle = $localize`:@@training.home.enroll:S'inscrire à un parcours`;
  readonly submitLabel = $localize`:@@training.enroll-dialog.submit:S'inscrire`;

  readonly statusFilter = new FormControl<TrainingPathStatus>('ACTIVE');
  paths$ = new BehaviorSubject<PathResponse[]>([]);

  readonly form = this.fb.nonNullable.group({
    pathId: ['', [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: TrainingService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<TrainingEnrollDialogComponent, EnrollmentResponse>
  ) {}

  ngOnInit(): void {
    this.loadPaths();
    this.statusFilter.valueChanges.subscribe(() => this.loadPaths());
  }

  loadPaths(): void {
    this.loading = true;
    this.svc.listPaths(0, 100, this.statusFilter.value || undefined)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: page => { this.paths$.next(page.content); this.loadError = null; },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[training-enroll] paths failed', err?.status, err?.error?.title);
          this.loadError = safeErrorMessage(err, $localize`:@@training.enroll-dialog.load-error:Chargement parcours impossible.`);
        }
      });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const userId = this.auth.snapshot()?.userId;
    if (!userId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.enroll({ userId, pathId: v.pathId })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: e => {
          this.snack.open($localize`:@@training.enroll-dialog.enrolled:Inscription enregistrée.`, $localize`:@@common.ok:OK`, { duration: 2200 });
          this.dialogRef.close(e);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[training-enroll] enroll failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@training.enroll-dialog.enroll-error:Inscription impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
