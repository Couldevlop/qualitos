import { Component, Inject, Optional } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { KpisService } from '../../kpis.service';
import { KpiDirection, KpiFrequency, KpiResponse } from '../../kpis.types';

export interface KpisDialogData { kpi?: KpiResponse; }

@Component({
  selector: 'qos-kpis-dialog',
  templateUrl: './kpis-dialog.component.html',
  styleUrls: ['./kpis-dialog.component.scss'],
  standalone: false
})
export class KpisDialogComponent {

  submitting = false;
  readonly isEdit: boolean;
  readonly form;

  readonly directions: { value: KpiDirection; label: string }[] = [
    { value: 'HIGHER_IS_BETTER', label: $localize`:@@kpis.dialog.dir-higher:↑ Plus haut = mieux (FPY, OEE, satisfaction…)` },
    { value: 'LOWER_IS_BETTER',  label: $localize`:@@kpis.dialog.dir-lower:↓ Plus bas  = mieux (DPMO, scrap, MTTR…)` }
  ];

  get dialogTitle(): string {
    return this.isEdit
      ? $localize`:@@kpis.dialog.edit-title:Modifier le KPI`
      : $localize`:@@kpis.dialog.create-title:Nouveau KPI`;
  }
  get submitLabel(): string {
    return this.isEdit
      ? $localize`:@@common.save:Enregistrer`
      : $localize`:@@common.create:Créer`;
  }
  readonly frequencies: KpiFrequency[] = [
    'REALTIME', 'DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY', 'ON_DEMAND'
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: KpisService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<KpisDialogComponent, KpiResponse>,
    @Optional() @Inject(MAT_DIALOG_DATA) public readonly data: KpisDialogData | null
  ) {
    this.isEdit = !!data?.kpi;
    const k = data?.kpi;
    this.form = this.fb.nonNullable.group({
      // OWASP A03 — regex + length mirror backend @Pattern + @Size
      code: [
        { value: k?.code ?? '', disabled: this.isEdit },
        [Validators.required, Validators.maxLength(100), Validators.pattern(/^[a-z0-9][a-z0-9_-]{1,99}$/)]
      ],
      name:        [k?.name ?? '',        [Validators.required, Validators.maxLength(250)]],
      description: [k?.description ?? '', [Validators.maxLength(2000)]],
      category:    [k?.category ?? '',    [Validators.maxLength(64)]],
      unit:        [k?.unit ?? '',        [Validators.maxLength(32)]],
      direction:   [
        { value: (k?.direction ?? 'HIGHER_IS_BETTER') as KpiDirection, disabled: this.isEdit },
        [Validators.required]
      ],
      frequency:   [(k?.frequency ?? 'MONTHLY') as KpiFrequency],
      targetValue:       [k?.targetValue       ?? null as number | null],
      thresholdWarning:  [k?.thresholdWarning  ?? null as number | null],
      thresholdCritical: [k?.thresholdCritical ?? null as number | null],
      applicableIndustriesCsv: [k?.applicableIndustriesCsv ?? '', [Validators.maxLength(1000)]]
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    // Cross-field sanity (A04 — refuse logically broken threshold combinations)
    const v = this.form.getRawValue();
    if (v.targetValue !== null && v.thresholdWarning !== null && v.thresholdCritical !== null) {
      const ordered = v.direction === 'HIGHER_IS_BETTER'
        ? v.targetValue >= v.thresholdWarning && v.thresholdWarning >= v.thresholdCritical
        : v.targetValue <= v.thresholdWarning && v.thresholdWarning <= v.thresholdCritical;
      if (!ordered) {
        this.snack.open(
          $localize`:@@kpis.dialog.threshold-order:Cible / Warning / Critical doivent être ordonnés selon le sens du KPI.`,
          $localize`:@@common.ok:OK`, { duration: 4500 }
        );
        return;
      }
    }
    this.submitting = true;

    const op$ = this.isEdit
      ? this.svc.update(this.data!.kpi!.id, {
          name: v.name.trim(),
          description: v.description?.trim() || undefined,
          category: v.category?.trim() || undefined,
          unit:     v.unit?.trim()     || undefined,
          frequency: v.frequency,
          targetValue:       v.targetValue       ?? undefined,
          thresholdWarning:  v.thresholdWarning  ?? undefined,
          thresholdCritical: v.thresholdCritical ?? undefined,
          applicableIndustriesCsv: v.applicableIndustriesCsv?.trim() || undefined
        })
      : (() => {
          const createdBy = this.auth.snapshot()?.userId;
          if (!createdBy) {
            this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
            this.submitting = false;
            throw new Error('No session');
          }
          return this.svc.create({
            code: v.code.trim(),
            name: v.name.trim(),
            description: v.description?.trim() || undefined,
            category: v.category?.trim() || undefined,
            unit:     v.unit?.trim()     || undefined,
            direction: v.direction,
            frequency: v.frequency,
            targetValue:       v.targetValue       ?? undefined,
            thresholdWarning:  v.thresholdWarning  ?? undefined,
            thresholdCritical: v.thresholdCritical ?? undefined,
            applicableIndustriesCsv: v.applicableIndustriesCsv?.trim() || undefined,
            createdBy
          });
        })();

    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: k => {
          this.snack.open(this.isEdit ? $localize`:@@kpis.dialog.updated:KPI mis à jour.` : $localize`:@@kpis.dialog.created:KPI créé.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(k);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[kpis-dialog] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@kpis.dialog.save-error:Erreur lors de l'enregistrement.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
