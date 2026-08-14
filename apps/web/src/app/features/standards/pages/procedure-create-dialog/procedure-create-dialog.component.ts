import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { finalize, map, shareReplay } from 'rxjs/operators';

import { DocumentsService } from '../../../documents/documents.service';
import { DocumentResponse } from '../../../documents/documents.types';
import { StandardsService } from '../../standards.service';

/**
 * Choix de la procédure de la GED dont naîtra un référentiel d'audit (§8).
 *
 * On ne propose QUE les procédures approuvées — celles qui portent une version
 * publiée. Les autres seraient refusées par le serveur (422) : les afficher
 * reviendrait à promettre un geste qui échoue, et l'utilisateur en conclurait
 * que la fonctionnalité est cassée plutôt que sa procédure inachevée.
 */
@Component({
  selector: 'qos-procedure-create-dialog',
  templateUrl: './procedure-create-dialog.component.html',
  styleUrls: ['./procedure-create-dialog.component.scss'],
  standalone: false
})
export class ProcedureCreateDialogComponent implements OnInit {

  submitting = false;
  procedures$!: Observable<DocumentResponse[]>;

  readonly form = this.fb.nonNullable.group({
    documentId: ['', [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly documents: DocumentsService,
    private readonly standards: StandardsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<ProcedureCreateDialogComponent, boolean>
  ) {}

  ngOnInit(): void {
    this.procedures$ = this.documents.list(0, 200).pipe(
      map(page => page.content.filter(d => d.type === 'PROCEDURE' && !!d.currentVersionId)),
      shareReplay(1)
    );
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.standards.createProcedureReferential(this.form.getRawValue().documentId)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.snack.open(
            $localize`:@@standards.procedure.created:Référentiel créé — saisissez maintenant ses exigences.`,
            $localize`:@@common.ok:OK`, { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: err => this.snack.open(this.messageFor(err?.status),
          $localize`:@@common.ok:OK`, { duration: 4000 })
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  /**
   * Chaque refus du serveur dit une chose différente, et l'utilisateur n'a pas
   * la même action à mener : rejoindre le référentiel existant, publier une
   * version, ou demander les droits. Un message unique les priverait tous les
   * trois de la suite à donner.
   */
  private messageFor(status: number | undefined): string {
    if (status === 409) {
      return $localize`:@@standards.procedure.conflict:Un référentiel existe déjà pour cette procédure.`;
    }
    if (status === 422) {
      return $localize`:@@standards.procedure.unapproved:Cette procédure doit être approuvée avant de servir de référentiel.`;
    }
    if (status === 403) {
      return $localize`:@@standards.procedure.forbidden:Vous n'avez pas les droits pour créer un référentiel.`;
    }
    return $localize`:@@standards.procedure.error:Création impossible pour le moment.`;
  }
}
