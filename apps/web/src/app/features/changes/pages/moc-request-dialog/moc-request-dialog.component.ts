import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { mocErrorMessage, mocTypeLabel } from '../../moc.labels';
import { MocService } from '../../moc.service';
import { MocDetail, MocRequestForm, MocType } from '../../moc.types';

/** Création (sans `detail`) ou modification (avec) d'une demande. */
export interface MocRequestDialogData {
  detail?: MocDetail;
}

/**
 * « Nouvelle demande » : titre, nature (permanent ou temporaire), site,
 * description et justification. Ni code ni demandeur : le serveur attribue le
 * code MOC-AAAA-NNN et prend le demandeur dans le jeton.
 */
@Component({
  selector: 'qos-moc-request-dialog',
  templateUrl: './moc-request-dialog.component.html',
  styleUrls: ['./moc-request-dialog.component.scss'],
  standalone: false
})
export class MocRequestDialogComponent {

  readonly types: { value: MocType; label: string }[] = [
    { value: 'PERMANENT', label: mocTypeLabel('PERMANENT') },
    { value: 'TEMPORARY', label: mocTypeLabel('TEMPORARY') }
  ];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(250)]],
    type: ['PERMANENT' as MocType, Validators.required],
    site: ['', Validators.maxLength(200)],
    description: ['', Validators.maxLength(4000)],
    justification: ['', Validators.maxLength(4000)],
    plannedFor: ['']
  });

  readonly createLabel = $localize`:@@moc.dialog.create:Créer la demande`;
  readonly saveLabel = $localize`:@@common.save:Enregistrer`;

  saving = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly service: MocService,
    private readonly snack: MatSnackBar,
    private readonly ref: MatDialogRef<MocRequestDialogComponent, MocDetail>,
    @Inject(MAT_DIALOG_DATA) readonly data: MocRequestDialogData
  ) {
    const d = data.detail;
    if (d) {
      this.form.setValue({
        title: d.title, type: d.type, site: d.site ?? '', description: d.description ?? '',
        justification: d.justification ?? '', plannedFor: d.plannedFor ?? ''
      });
    }
  }

  get editing(): boolean {
    return !!this.data.detail;
  }

  get dialogTitle(): string {
    return this.editing
      ? $localize`:@@moc.dialog.edit-title:Modifier la demande ${this.data.detail?.code}:code:`
      : $localize`:@@moc.dialog.new-title:Nouvelle demande de changement`;
  }

  get blockedReason(): string | undefined {
    return this.form.controls.title.value.trim()
      ? undefined
      : $localize`:@@moc.dialog.title-required:Donnez un titre à la demande.`;
  }

  submit(): void {
    if (this.form.invalid || this.saving || !this.form.controls.title.value.trim()) return;
    const v = this.form.getRawValue();
    const body: MocRequestForm = {
      title: v.title.trim(),
      type: v.type,
      site: v.site.trim() || null,
      description: v.description.trim() || null,
      justification: v.justification.trim() || null,
      plannedFor: v.plannedFor || null
    };
    this.saving = true;
    const call = this.data.detail ? this.service.update(this.data.detail.id, body) : this.service.create(body);
    call.subscribe({
      next: d => {
        this.saving = false;
        this.snack.open(this.editing
          ? $localize`:@@moc.dialog.updated:Demande modifiée.`
          : $localize`:@@moc.dialog.created:Demande ${d.code}:code: créée.`, undefined, { duration: 3000 });
        this.ref.close(d);
      },
      error: err => {
        this.saving = false;
        this.snack.open(mocErrorMessage(err, $localize`:@@moc.dialog.failed:La demande n'a pas été enregistrée.`),
          undefined, { duration: 5000 });
      }
    });
  }

  cancel(): void {
    this.ref.close();
  }
}
