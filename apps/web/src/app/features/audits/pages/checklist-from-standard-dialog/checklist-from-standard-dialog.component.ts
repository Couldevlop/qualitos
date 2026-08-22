import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { finalize, map, shareReplay } from 'rxjs/operators';

import { StandardsService } from '../../../standards/standards.service';
import { StandardSummary } from '../../../standards/standards.types';
import { AuditsService } from '../../audits.service';

export interface ChecklistFromStandardDialogData {
  planId: string;
}

/**
 * Génération de la checklist d'un audit depuis un référentiel du catalogue (§8).
 *
 * Le catalogue propose ici les procédures internes du tenant COMME les normes
 * livrées : auditer sa propre procédure et auditer une ISO se pilotent de la
 * même façon, et distinguer les deux dans l'écran obligerait l'utilisateur à
 * savoir d'avance dans quelle liste chercher.
 */
@Component({
  selector: 'qos-checklist-from-standard-dialog',
  templateUrl: './checklist-from-standard-dialog.component.html',
  styleUrls: ['./checklist-from-standard-dialog.component.scss'],
  standalone: false
})
export class ChecklistFromStandardDialogComponent implements OnInit {

  submitting = false;
  standards$!: Observable<StandardSummary[]>;

  readonly form = this.fb.nonNullable.group({
    standardId: ['', [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly standards: StandardsService,
    private readonly audits: AuditsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<ChecklistFromStandardDialogComponent, number>,
    @Inject(MAT_DIALOG_DATA) private readonly data: ChecklistFromStandardDialogData
  ) {}

  ngOnInit(): void {
    this.standards$ = this.standards.listCatalog(0, 200).pipe(
      map(page => page.content),
      shareReplay(1)
    );
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.audits
      .generateChecklistFromStandard(this.data.planId, this.form.getRawValue().standardId)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: items => {
          this.snack.open(
            items.length
              ? $localize`:@@audits.from-standard.created:${items.length}:count: questions générées.`
              // Un référentiel vide n'est pas une panne : c'est un référentiel
              // dont les exigences restent à saisir, et le dire évite de
              // chercher une erreur qui n'existe pas.
              : $localize`:@@audits.from-standard.empty:Ce référentiel ne contient encore aucune exigence.`,
            $localize`:@@common.ok:OK`, { duration: 3500 });
          this.dialogRef.close(items.length);
        },
        error: err => this.snack.open(this.messageFor(err?.status),
          $localize`:@@common.ok:OK`, { duration: 4000 })
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  private messageFor(status: number | undefined): string {
    if (status === 409) {
      return $localize`:@@audits.from-standard.conflict:La checklist n'est pas vide, ou cet audit n'est plus au stade de la préparation.`;
    }
    if (status === 404) {
      return $localize`:@@audits.from-standard.not-found:Ce référentiel est introuvable.`;
    }
    return $localize`:@@audits.from-standard.error:Génération impossible pour le moment.`;
  }
}
