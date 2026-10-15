import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { standardCodeLabel, standardShortName } from '../../smi.labels';
import { SmiService } from '../../smi.service';
import { StandardCandidate, StandardRef } from '../../smi.types';

/**
 * « Ajouter une norme au SMI » : les normes de système de management du
 * catalogue que le client n'a pas encore adoptées (ISO 14001, 45001, 27001,
 * 22301, 50001…), les suggérées en tête et la première d'entre elles déjà
 * choisie — avec une ISO 9001 pilotée, c'est l'ISO 14001.
 *
 * <p>Ajouter crée l'adoption du Standards Hub (feuille de route de
 * certification comprise) ; la fenêtre se ferme sur l'adoption créée, et
 * l'écran qui l'a ouverte se recharge. Réservé à l'administrateur du client :
 * l'écran ne l'ouvre que pour lui, le serveur refuse les autres.
 */
@Component({
  selector: 'qos-smi-add-standard-dialog',
  templateUrl: './add-standard-dialog.component.html',
  styleUrls: ['./add-standard-dialog.component.scss'],
  standalone: false
})
export class AddStandardDialogComponent implements OnInit {

  candidates: StandardCandidate[] = [];
  loading = true;
  loadError: string | null = null;
  submitError: string | null = null;
  submitting = false;

  readonly form = this.fb.nonNullable.group({
    standardId: ['', [Validators.required]]
  });

  readonly blockedReason =
    $localize`:@@smi.add.blocked:Choisissez la norme à ajouter.`;

  constructor(
    private readonly fb: FormBuilder,
    private readonly service: SmiService,
    private readonly dialogRef: MatDialogRef<AddStandardDialogComponent, StandardRef>
  ) {}

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.loadError = null;
    this.service.candidates().subscribe({
      next: c => {
        this.candidates = c;
        this.loading = false;
        // La première suggestion est choisie d'avance : c'est presque toujours elle qu'on vient chercher.
        const premiere = c.find(x => x.suggested) ?? c[0];
        if (premiere && !this.form.controls.standardId.value) {
          this.form.controls.standardId.setValue(premiere.standardId);
        }
      },
      error: err => {
        this.loading = false;
        this.loadError = safeErrorMessage(err,
          $localize`:@@smi.add.load-failed:Les normes du catalogue n'ont pas pu être chargées.`);
      }
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.submitError = null;
    this.service.addStandard(this.form.getRawValue().standardId)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: ref => this.dialogRef.close(ref),
        error: err => (this.submitError = this.messageFor(err))
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  get suggestions(): StandardCandidate[] {
    return this.candidates.filter(c => c.suggested);
  }

  get others(): StandardCandidate[] {
    return this.candidates.filter(c => !c.suggested);
  }

  shortName(c: StandardCandidate): string {
    return standardShortName(c.name, c.code);
  }

  /** « Rattachée à ISO 9001 » : pourquoi elle est suggérée. */
  sharedText(c: StandardCandidate): string {
    const normes = c.sharedWith.map(standardCodeLabel).join(', ');
    return $localize`:@@smi.add.shared-with:Rattachée à ${normes}:standards:`;
  }

  trackById(_i: number, c: StandardCandidate): string {
    return c.standardId;
  }

  /**
   * Chaque refus dit quoi faire : rien (déjà adoptée — un autre l'a fait entre
   * temps), demander le droit, ou réessayer.
   */
  private messageFor(err: { status?: number } | null | undefined): string {
    switch (err?.status) {
      case 409:
        return $localize`:@@smi.add.conflict:Cette norme est déjà adoptée par votre organisation.`;
      case 403:
        return $localize`:@@smi.add.forbidden:Seul l'administrateur de votre organisation peut ajouter une norme.`;
      case 404:
        return $localize`:@@smi.add.not-found:Cette norme n'est pas une norme de système de management du catalogue.`;
      default:
        return safeErrorMessage(err, $localize`:@@smi.add.failed:La norme n'a pas pu être ajoutée.`);
    }
  }
}
