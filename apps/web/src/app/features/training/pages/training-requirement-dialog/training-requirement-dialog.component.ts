import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { TrainingService } from '../../training.service';
import { SkillRequirementResponse, SkillResponse } from '../../training.types';

export interface TrainingRequirementDialogData {
  pathId: string;
  excludeSkillIds?: string[];   // already-required skills, hidden from the picker
}

const LEVEL_LABELS = ['NONE', 'AWARE', 'PRACTITIONER', 'COMPETENT', 'EXPERT'];

@Component({
  selector: 'qos-training-requirement-dialog',
  templateUrl: './training-requirement-dialog.component.html',
  styleUrls: ['./training-requirement-dialog.component.scss'],
  standalone: false
})
export class TrainingRequirementDialogComponent implements OnInit {

  submitting = false;
  loading = true;
  loadError: string | null = null;

  readonly dialogTitle = $localize`:@@training.requirement-dialog.title:Ajouter une exigence de compétence`;
  readonly dialogSubtitle = $localize`:@@training.requirement-dialog.subtitle:Échelle Dreyfus 0-4 : NONE / AWARE / PRACTITIONER / COMPETENT / EXPERT. Le gap = (cible - acquis) sera utilisé par l'analyse de gap par utilisateur.`;
  readonly submitLabel = $localize`:@@training.requirement-dialog.submit:Rattacher`;

  readonly levels = [0, 1, 2, 3, 4];
  readonly levelLabels = LEVEL_LABELS;

  skills$ = new BehaviorSubject<SkillResponse[]>([]);

  readonly form = this.fb.nonNullable.group({
    skillId:     ['', [Validators.required]],
    targetLevel: [2, [Validators.required, Validators.min(0), Validators.max(4)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: TrainingService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<TrainingRequirementDialogComponent, SkillRequirementResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: TrainingRequirementDialogData
  ) {}

  ngOnInit(): void {
    this.svc.listSkills(0, 200)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: page => {
          const excl = new Set(this.data.excludeSkillIds ?? []);
          this.skills$.next(page.content.filter(s => !excl.has(s.id)));
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[training-req] skills failed', err?.status, err?.error?.title);
          this.loadError = safeErrorMessage(err, $localize`:@@training.requirement-dialog.skills-error:Catalogue compétences indisponible.`);
        }
      });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.attachRequirement(this.data.pathId, {
      skillId: v.skillId,
      targetLevel: v.targetLevel
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: r => {
          this.snack.open($localize`:@@training.requirement-dialog.attached:Compétence rattachée.`, $localize`:@@common.ok:OK`, { duration: 2200 });
          this.dialogRef.close(r);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[training-req] attach failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@training.requirement-dialog.attach-error:Rattachement impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
