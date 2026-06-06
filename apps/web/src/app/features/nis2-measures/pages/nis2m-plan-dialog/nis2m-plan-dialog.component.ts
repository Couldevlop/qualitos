import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { Nis2MeasuresService } from '../../nis2m.service';
import {
  CATEGORY_LABEL,
  Nis2MeasureCategory,
  Nis2MeasureView,
  ResidualRiskRating
} from '../../nis2m.types';

const URL_LINE = /^https?:\/\/[^\s]+$/;
const UUID_LINE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function linesValidator(re: RegExp, msg: string): ValidatorFn {
  return (c: AbstractControl): ValidationErrors | null => {
    const v = (c.value ?? '').toString().trim();
    if (!v) return null;
    for (const l of v.split(/\r?\n/).map((s: string) => s.trim()).filter((s: string) => s)) {
      if (!re.test(l)) return { lines: msg };
    }
    return null;
  };
}

@Component({
  selector: 'qos-nis2m-plan-dialog',
  templateUrl: './nis2m-plan-dialog.component.html',
  styleUrls: ['./nis2m-plan-dialog.component.scss'],
  standalone: false
})
export class Nis2mPlanDialogComponent {

  submitting = false;
  readonly categoryLabel = CATEGORY_LABEL;
  readonly categories: Nis2MeasureCategory[] = Object.keys(CATEGORY_LABEL) as Nis2MeasureCategory[];
  readonly risks: ResidualRiskRating[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  readonly form = this.fb.nonNullable.group({
    reference:                 ['', [Validators.required, Validators.maxLength(64), Validators.pattern(/^[A-Z][A-Z0-9_-]{1,63}$/)]],
    category:                  ['RISK_ANALYSIS' as Nis2MeasureCategory, [Validators.required]],
    title:                     ['', [Validators.required, Validators.maxLength(250)]],
    description:               ['', [Validators.maxLength(4000)]],
    ownerUserId:               ['', [Validators.pattern(/^$|^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i)]],
    maturityLevel:             [3,   [Validators.required, Validators.min(1), Validators.max(5)]],
    residualRiskRating:        ['MEDIUM' as ResidualRiskRating, [Validators.required]],
    criticalRiskJustification: ['', [Validators.maxLength(4000)]],
    reviewIntervalDays:        [180, [Validators.required, Validators.min(30), Validators.max(1095)]],
    evidenceUrlsRaw:           ['',  [linesValidator(URL_LINE, $localize`:@@nis2-measures.plan.url-per-line:Une URL http(s) par ligne.`)]],
    linkedActivitiesRaw:       ['',  [linesValidator(UUID_LINE, $localize`:@@nis2-measures.plan.uuid-per-line:Un UUID par ligne.`)]],
    linkedAgreementsRaw:       ['',  [linesValidator(UUID_LINE, $localize`:@@nis2-measures.plan.uuid-per-line:Un UUID par ligne.`)]],
    notes:                     ['',  [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: Nis2MeasuresService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<Nis2mPlanDialogComponent, Nis2MeasureView>
  ) {}

  isCritical(): boolean { return this.form.controls.residualRiskRating.value === 'CRITICAL'; }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    if (v.residualRiskRating === 'CRITICAL' && !v.criticalRiskJustification.trim()) {
      this.snack.open($localize`:@@nis2-measures.plan.critical-required:Justification obligatoire pour un risque résiduel CRITICAL.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const userId = this.auth.snapshot()?.userId;
    if (!userId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.plan({
      reference: v.reference.trim(),
      category: v.category,
      title: v.title.trim(),
      description: v.description?.trim() || undefined,
      ownerUserId: v.ownerUserId?.trim() || undefined,
      maturityLevel: v.maturityLevel,
      residualRiskRating: v.residualRiskRating,
      criticalRiskJustification: v.criticalRiskJustification?.trim() || undefined,
      reviewIntervalDays: v.reviewIntervalDays,
      evidenceUrls: this.lines(v.evidenceUrlsRaw),
      linkedProcessingActivityIds: this.lines(v.linkedActivitiesRaw),
      linkedProcessorAgreementIds: this.lines(v.linkedAgreementsRaw),
      notes: v.notes?.trim() || undefined,
      createdByUserId: userId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: m => this.dialogRef.close(m),
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[nis2m-plan] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@nis2-measures.plan.plan-failed:Planification impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }

  private lines(raw: string): string[] {
    return (raw ?? '').split(/\r?\n/).map(s => s.trim()).filter(s => s);
  }
}
