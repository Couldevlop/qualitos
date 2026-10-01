import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ACTION_STATUSES } from '../../risk-register.labels';
import { OpportunityActionStatus } from '../../risk-register.types';

export interface ActionDialogData {
  /** `capa` : ouvrir un dossier CAPA depuis un risque ; `action` : une action d'opportunité (ACT-n). */
  mode: 'capa' | 'action';
  /** En modification d'une action existante. */
  title?: string;
  dueDate?: string | null;
  status?: OpportunityActionStatus;
  number?: number;
  /** Référence de la fiche, rappelée dans l'en-tête. */
  reference: string;
}

export interface ActionDialogResult {
  title: string;
  description: string | null;
  dueDate: string | null;
  status: OpportunityActionStatus;
}

/**
 * La fenêtre d'une action : intitulé, échéance, et selon le cas une description
 * (dossier CAPA) ou un statut (action d'opportunité). Elle ne parle pas au
 * serveur : elle rend sa saisie, la fiche l'envoie.
 */
@Component({
  selector: 'qos-risk-action-dialog',
  templateUrl: './action-dialog.component.html',
  styleUrls: ['./action-dialog.component.scss'],
  standalone: false
})
export class ActionDialogComponent {

  readonly statuses = ACTION_STATUSES;

  readonly form = this.fb.nonNullable.group({
    title: [this.data.title ?? '', [Validators.required, Validators.maxLength(255)]],
    description: ['', Validators.maxLength(4000)],
    dueDate: [this.data.dueDate ?? ''],
    status: [this.data.status ?? ('TO_START' as OpportunityActionStatus)]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<ActionDialogComponent, ActionDialogResult>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ActionDialogData
  ) {}

  get isCapa(): boolean {
    return this.data.mode === 'capa';
  }

  get isEdit(): boolean {
    return this.data.number !== undefined;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.dialogRef.close({
      title: v.title.trim(),
      description: v.description.trim() || null,
      dueDate: v.dueDate || null,
      status: v.status
    });
  }
}
