import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { TenantUser } from '../../../admin/admin.types';
import { TenantTeamService } from '../../../admin/tenant-team.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { CapaService } from '../../capa.service';
import { CapaCaseResponse, CapaCriticity } from '../../capa.types';

export interface CapaEditDialogData {
  capa: CapaCaseResponse;
}

@Component({
  selector: 'qos-capa-edit-dialog',
  templateUrl: './capa-edit-dialog.component.html',
  styleUrls: ['./capa-edit-dialog.component.scss'],
  standalone: false
})
export class CapaEditDialogComponent implements OnInit {
  submitting = false;

  /**
   * Les membres de l'organisation, pour désigner le vérificateur.
   *
   * <p>L'annuaire du client (`/api/v1/users`) et non une saisie libre : un
   * vérificateur est un vrai compte, à qui on peut notifier la tâche, et deux
   * orthographes du même nom ne peuvent pas devenir deux personnes.
   */
  membres: TenantUser[] = [];

  /** Vrai quand l'annuaire n'a pas répondu : on le DIT, on ne fait pas semblant. */
  annuaireIndisponible = false;

  readonly criticities: CapaCriticity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    criticity: ['MEDIUM' as CapaCriticity, [Validators.required]],
    sourceRef: ['', [Validators.maxLength(255)]],
    dueDate: [''],
    // `null` = la question n'a pas été tranchée ; c'est distinct de `false`.
    verificationRequired: [null as boolean | null],
    verificationAssigneeId: [null as string | null],
    verificationInstructions: ['', [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly capa: CapaService,
    private readonly equipe: TenantTeamService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CapaEditDialogComponent, CapaCaseResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CapaEditDialogData
  ) {
    this.form.patchValue({
      title: data.capa.title,
      description: data.capa.description ?? '',
      criticity: data.capa.criticity,
      sourceRef: data.capa.sourceRef ?? '',
      dueDate: data.capa.dueDate ?? '',
      verificationRequired: data.capa.verificationRequired ?? null,
      verificationAssigneeId: data.capa.verificationAssigneeId ?? null,
      verificationInstructions: data.capa.verificationInstructions ?? ''
    });

    // Repasser à « non exigée » doit effacer le vérificateur et les consignes.
    this.form.controls.verificationRequired.valueChanges.subscribe(
      exigee => this.appliquerExigence(exigee));
    this.appliquerExigence(this.form.controls.verificationRequired.value);
  }

  ngOnInit(): void {
    this.equipe.list(0, 200).subscribe({
      next: page => (this.membres = page.content.filter(m => m.active)),
      error: () => (this.annuaireIndisponible = true)
    });
  }

  /**
   * Vrai quand le bloc « à qui » et « quoi vérifier » a lieu d'être rempli.
   *
   * <p>Un CHAMP tenu à jour, et non un accesseur qui lirait l'état vivant du
   * formulaire : un accesseur change de valeur au milieu du cycle de détection
   * de changements et déclenche NG0100 — défaut déjà rencontré ailleurs dans ce
   * dépôt.
   */
  verificationExigee = false;

  /**
   * Ce qu'entraîne le passage à « non exigée ».
   *
   * <p>Le caractère OBLIGATOIRE du vérificateur, lui, n'est pas posé ici : il
   * l'est par le gabarit, où `[required]` suit l'affichage du champ. Le tenir
   * aux deux endroits donnait deux validateurs pour une seule règle, dont un
   * seul se retirait — et le formulaire restait invalide sans le dire.
   */
  private appliquerExigence(exigee: boolean | null): void {
    this.verificationExigee = exigee === true;
    if (exigee !== false) {
      return;
    }
    // Ne plus exiger efface ce qui n'a plus d'objet : laisser un vérificateur
    // derrière soi laisserait croire qu'une vérification est encore attendue.
    this.form.controls.verificationAssigneeId.setValue(null, { emitEvent: false });
    this.form.controls.verificationInstructions.setValue('', { emitEvent: false });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.capa
      .updateCase(this.data.capa.id, {
        title: v.title.trim(),
        description: v.description?.trim() || undefined,
        criticity: v.criticity,
        sourceRef: v.sourceRef?.trim() || undefined,
        dueDate: v.dueDate || undefined,
        // `null` signifie « non tranché » : on ne l'envoie pas, sans quoi une
        // mise à jour muette effacerait une décision déjà prise.
        verificationRequired: v.verificationRequired ?? undefined,
        verificationAssigneeId: v.verificationRequired === true
          ? (v.verificationAssigneeId ?? undefined) : undefined,
        // Le nom part avec l'identifiant : il est recopié dans le dossier pour
        // rester lisible même si le compte disparaît de l'annuaire.
        verificationAssigneeName: v.verificationRequired === true
          ? this.nomDu(v.verificationAssigneeId) : undefined,
        verificationInstructions: v.verificationRequired === true
          ? (v.verificationInstructions?.trim() || undefined) : undefined
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: c => {
          this.snack.open($localize`:@@capa.edit.success:Cas mis à jour.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(c);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-edit] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-update:Erreur lors de la mise à jour.`),
            $localize`:@@common.ok:OK`, { duration: 4000 }
          );
        }
      });
  }

  /** Le libellé du membre, tel qu'il sera figé dans le dossier. */
  private nomDu(id: string | null): string | undefined {
    if (!id) return undefined;
    return this.membres.find(m => m.id === id)?.email;
  }

  cancel(): void { this.dialogRef.close(); }
}
