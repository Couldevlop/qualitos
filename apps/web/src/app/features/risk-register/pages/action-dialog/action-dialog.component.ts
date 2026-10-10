import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ACTION_STATUSES, CAPA_KINDS } from '../../risk-register.labels';
import { CapaKind, OpportunityActionStatus } from '../../risk-register.types';

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
  /** En mode CAPA : le responsable proposé (le propriétaire du risque) et les noms déjà employés. */
  assignee?: string;
  assignees?: string[];
}

export interface ActionDialogResult {
  title: string;
  description: string | null;
  dueDate: string | null;
  status: OpportunityActionStatus;
  /** Renseignés en mode CAPA seulement. */
  kind: CapaKind | null;
  assignee: string | null;
}

/**
 * La fenêtre d'une action : intitulé, échéance, et selon le cas une description
 * (dossier CAPA) ou un statut (action d'opportunité). Elle ne parle pas au
 * serveur : elle rend sa saisie, la fiche l'envoie.
 *
 * <p>En mode CAPA, la nature (corrective ou préventive), le responsable et
 * l'échéance sont obligatoires : le dossier ouvert depuis un risque porte une
 * seule action, et une action sans responsable ni date ne se pilote pas. Les
 * validateurs sont posés à la construction, une fois pour toutes — le mode ne
 * change pas pendant la vie de la fenêtre.
 */
@Component({
  selector: 'qos-risk-action-dialog',
  templateUrl: './action-dialog.component.html',
  styleUrls: ['./action-dialog.component.scss'],
  standalone: false
})
export class ActionDialogComponent {

  readonly statuses = ACTION_STATUSES;
  readonly kinds = CAPA_KINDS;
  /** Pas d'échéance dans le passé : le dossier naîtrait en retard (le serveur le refuse aussi). */
  readonly today = localIsoDate(new Date());

  readonly form = this.fb.nonNullable.group({
    title: [this.data.title ?? '', [Validators.required, Validators.maxLength(255)]],
    description: ['', Validators.maxLength(4000)],
    kind: ['PREVENTIVE' as CapaKind, this.data.mode === 'capa' ? Validators.required : []],
    assignee: [this.data.assignee ?? '',
      this.data.mode === 'capa' ? [Validators.required, Validators.maxLength(255)] : []],
    dueDate: [this.data.dueDate ?? '', this.data.mode === 'capa' ? Validators.required : []],
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

  /** Les noms déjà employés qui contiennent la saisie. */
  get assigneeOptions(): string[] {
    const saisie = this.form.controls.assignee.value.trim().toLowerCase();
    return (this.data.assignees ?? []).filter(n => n.toLowerCase().includes(saisie)).slice(0, 8);
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
      status: v.status,
      kind: this.isCapa ? v.kind : null,
      assignee: this.isCapa ? v.assignee.trim() : null
    });
  }
}

/** La date du jour au fuseau du navigateur, au format d'un `input type=date`. */
export function localIsoDate(d: Date): string {
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}
