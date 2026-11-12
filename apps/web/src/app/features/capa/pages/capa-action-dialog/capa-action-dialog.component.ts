import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { DirectoryUser } from '../../../../core/directory/directory.types';
import { personName } from '../../../../core/directory/directory-names';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { CAPA_ACTION_TYPES } from '../../capa.labels';
import { CapaService } from '../../capa.service';
import { CapaActionResponse, CapaActionStatus, CapaActionType } from '../../capa.types';

/** Ce qu'il faut pour ouvrir la fenêtre : le dossier, et l'action quand on la relit. */
export interface CapaActionDialogData {
  /** Le dossier qui porte l'action. */
  caseId: string;
  /** L'action à modifier ; absente, la fenêtre en ajoute une. */
  action?: CapaActionResponse;
  /** Le pilote du dossier, proposé comme responsable d'une action nouvelle. */
  ownerId?: string;
  ownerName?: string;
  /** L'échéance du dossier, proposée comme échéance d'une action nouvelle. */
  dueDate?: string;
  /** Vrai quand le dossier est clos ou rejeté : tout est en lecture seule. */
  readOnly?: boolean;
}

/** Les trois statuts, dans l'ordre du déroulé réel. */
const STATUTS: ReadonlyArray<CapaActionStatus> = ['PENDING', 'IN_PROGRESS', 'DONE'];

/**
 * UNE action d'un dossier CAPA : on l'ajoute, on la relit, on la corrige.
 *
 * <p>Avant, l'action ne se saisissait qu'à l'ouverture du dossier, et ne
 * s'avançait ensuite que d'un cran à la fois par un bouton. Rien ne permettait
 * de corriger un intitulé, de réassigner un porteur parti, ni de décaler une
 * échéance : il fallait rouvrir un dossier pour changer une phrase.
 *
 * <p>Une seule fenêtre pour l'ajout et l'édition : deux jumelles divergeraient
 * au premier champ ajouté d'un seul côté. Le statut ne s'affiche qu'à
 * l'édition — une action qui vient d'être décidée est « à faire », et proposer
 * d'autre chose laisserait créer une action déjà terminée sans que personne ne
 * l'ait menée.
 */
@Component({
  selector: 'qos-capa-action-dialog',
  templateUrl: './capa-action-dialog.component.html',
  styleUrls: ['./capa-action-dialog.component.scss'],
  standalone: false
})
export class CapaActionDialogComponent {

  readonly natures = CAPA_ACTION_TYPES;
  readonly statuses = STATUTS;
  readonly submitting$ = new BehaviorSubject<boolean>(false);

  /** Le nom du porteur, retenu avec son compte : une personne, jamais un identifiant. */
  private assigneeName: string | null = null;

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    actionType: ['CORRECTIVE' as CapaActionType, [Validators.required]],
    assigneeId: [''],
    dueDate: [''],
    status: ['PENDING' as CapaActionStatus]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly capa: CapaService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CapaActionDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) readonly data: CapaActionDialogData
  ) {
    const a = data.action;
    if (a) {
      this.assigneeName = a.assigneeName ?? null;
      this.form.patchValue({
        title: a.title,
        description: a.description ?? '',
        actionType: a.actionType || 'CORRECTIVE',
        assigneeId: a.assigneeId ?? '',
        dueDate: a.dueDate ?? '',
        status: a.status
      });
    } else {
      // Le pilote du dossier et son échéance sont PROPOSÉS : c'est le cas le
      // plus fréquent, et les retaper à chaque action serait du recopiage.
      this.assigneeName = data.ownerName ?? null;
      this.form.patchValue({
        assigneeId: data.ownerId ?? '',
        dueDate: data.dueDate ?? ''
      });
    }
    if (data.readOnly) {
      this.form.disable();
    }
  }

  get editing(): boolean {
    return !!this.data.action;
  }

  get dialogTitle(): string {
    return this.editing
      ? $localize`:@@capa.action-dialog.title-edit:Modifier l'action`
      : $localize`:@@capa.action-dialog.title-add:Ajouter une action`;
  }

  statusLabel(status: CapaActionStatus): string {
    return status === 'PENDING'
      ? $localize`:@@capa.action-status.pending:À faire`
      : status === 'IN_PROGRESS'
        ? $localize`:@@capa.action-status.in-progress:En cours`
        : $localize`:@@capa.action-status.done:Faite`;
  }

  /** Le sélecteur rend la personne choisie : son nom part avec son compte (ADR 0052). */
  onAssignee(user: DirectoryUser | null): void {
    this.assigneeName = user ? personName(user) : null;
  }

  submit(): void {
    if (this.form.invalid || this.submitting$.value || this.data.readOnly) {
      return;
    }
    const v = this.form.getRawValue();
    const corps = {
      title: v.title.trim(),
      description: v.description.trim() || undefined,
      actionType: v.actionType,
      assigneeId: v.assigneeId || undefined,
      assigneeName: v.assigneeId ? (this.assigneeName ?? undefined) : undefined,
      dueDate: v.dueDate || undefined
    };
    this.submitting$.next(true);
    const envoi = this.editing
      ? this.capa.updateAction(this.data.caseId, this.data.action!.id, { ...corps, status: v.status })
      : this.capa.addAction(this.data.caseId, corps);
    envoi.pipe(finalize(() => this.submitting$.next(false))).subscribe({
      next: () => {
        this.snack.open(
          this.editing
            ? $localize`:@@capa.action-dialog.saved:Action enregistrée.`
            : $localize`:@@capa.action-dialog.added:Action ajoutée.`,
          $localize`:@@common.ok:OK`, { duration: 2000 });
        this.dialogRef.close(true);
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[capa-action-dialog] save failed', err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(
            err, $localize`:@@capa.action-dialog.failed:L'action n'a pas pu être enregistrée.`),
          $localize`:@@common.close:Fermer`, { duration: 4000 });
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
