import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { NcMethod, NcSeverity } from '../../nc.types';

/** Ce que la fenêtre rend : de quoi appeler la conversion, rien de plus. */
export interface NcConvertChoice {
  method: NcMethod;
  pilotId: string;
  dueDate: string | null;
}

export interface NcConvertDialogData {
  /** La référence de l'écart, rappelée : c'est elle que le cycle citera. */
  reference: string;
  /** Son intitulé, qui devient celui du cycle ou du projet. */
  title: string;
  /** Sa gravité : c'est elle qui propose la méthode. */
  severity: NcSeverity;
}

/**
 * Ouvrir, depuis un écart, le cycle PDCA ou le projet DMAIC qui le traitera.
 *
 * <p>La fenêtre ne redemande ni l'intitulé, ni l'énoncé du problème : ils sont
 * déjà écrits sur l'écart, et le serveur les reprend. Elle demande les deux
 * choses que l'écart ne porte pas : <b>la méthode</b> — proposée par la gravité,
 * modifiable — et <b>la personne qui mènera le travail</b>, sans laquelle un
 * cycle ou un projet ne se revoit jamais.
 *
 * <p>La méthode proposée est affichée avec sa raison. Un écran qui choisirait
 * en silence laisserait croire que la plateforme décide seule ; or la personne
 * qui tient le dossier en sait plus qu'une table de correspondance — un écart
 * majeur sur un procédé déjà sous carte de contrôle peut mériter un DMAIC.
 *
 * <p>L'échéance n'apparaît que pour un projet DMAIC : un cycle PDCA n'en porte
 * pas, ses étapes ont les leurs. Afficher un champ que le serveur ignore serait
 * une promesse non tenue.
 */
@Component({
  selector: 'qos-nc-convert-dialog',
  templateUrl: './nc-convert-dialog.component.html',
  styleUrls: ['./nc-convert-dialog.component.scss'],
  standalone: false
})
export class NcConvertDialogComponent {

  /** La méthode que la gravité appelle. Même règle que le serveur, §3.6. */
  static methodFor(severity: NcSeverity): NcMethod {
    return severity === 'CRITICAL' ? 'DMAIC' : 'PDCA';
  }

  readonly proposed: NcMethod = NcConvertDialogComponent.methodFor(this.data.severity);

  readonly form = this.fb.nonNullable.group({
    method: [this.proposed as NcMethod, [Validators.required]],
    // Un cycle ou un projet dont personne ne répond ne se revoit jamais : le
    // serveur refuse sans lui, et l'écran ne laisse pas partir une requête
    // qu'il refusera.
    pilotId: ['', [Validators.required]],
    dueDate: [null as Date | null]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<NcConvertDialogComponent, NcConvertChoice>,
    @Inject(MAT_DIALOG_DATA) public readonly data: NcConvertDialogData
  ) {}

  /** Vrai quand la méthode retenue n'est pas celle que la gravité proposait. */
  get overridden(): boolean {
    return this.form.getRawValue().method !== this.proposed;
  }

  /** L'échéance n'a de sens que pour un projet DMAIC. */
  get dmaic(): boolean {
    return this.form.getRawValue().method === 'DMAIC';
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.dialogRef.close({
      method: v.method,
      pilotId: v.pilotId,
      // Une échéance saisie puis la méthode rebasculée sur PDCA ne doit pas
      // partir : le serveur l'ignorerait, et l'utilisateur croirait l'avoir posée.
      dueDate: v.method === 'DMAIC' && v.dueDate ? NcConvertDialogComponent.jour(v.dueDate) : null
    });
  }

  cancel(): void { this.dialogRef.close(); }

  /** Une date sans fuseau : une échéance est un jour, pas un instant. */
  private static jour(d: Date): string {
    const mois = String(d.getMonth() + 1).padStart(2, '0');
    const jour = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${mois}-${jour}`;
  }
}
