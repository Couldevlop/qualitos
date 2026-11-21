import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ObjectiveProposal } from '../../objectives.types';

export interface ObjectiveRejectDialogData {
  proposal: ObjectiveProposal;
}

/**
 * « Rejeter avec justification ».
 *
 * <p>Le motif est OBLIGATOIRE, et c'est le seul champ. Sans lui, un objectif
 * critique sans dossier ne se distinguerait pas d'un objectif critique que
 * personne n'a regardé — et c'est précisément la question qu'un auditeur pose.
 *
 * <p>La fenêtre rappelle ce qu'on écarte : rejeter de mémoire, après avoir fermé
 * le panneau, ferait écrire un motif sur un écart qu'on ne relit plus.
 */
@Component({
  selector: 'qos-objective-reject-dialog',
  templateUrl: './objective-reject-dialog.component.html',
  styleUrls: ['./objective-reject-dialog.component.scss'],
  standalone: false
})
export class ObjectiveRejectDialogComponent {

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<ObjectiveRejectDialogComponent, string>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ObjectiveRejectDialogData
  ) {}

  submit(): void {
    const motif = this.form.controls.reason.value.trim();
    if (!motif) {
      return;
    }
    this.dialogRef.close(motif);
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
