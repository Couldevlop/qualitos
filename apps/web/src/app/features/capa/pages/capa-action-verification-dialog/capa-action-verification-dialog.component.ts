import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { CapaService } from '../../capa.service';
import { CapaActionResponse, EffectivenessVerdict } from '../../capa.types';

/** Ce qu'il faut pour juger : le dossier, l'action, et le critère à confronter. */
export interface CapaActionVerificationDialogData {
  caseId: string;
  action: CapaActionResponse;
  /**
   * Le critère du DOSSIER, pour les actions antérieures aux colonnes de la
   * V177 : elles n'ont pas le leur, et le serveur retombe sur celui-là. Si les
   * deux manquent, il refuse — on ne juge pas sans critère annoncé (ADR 0108).
   */
  caseCriterion?: string;
}

/**
 * JUGER L'EFFICACITÉ D'UNE ACTION (ADR 0132).
 *
 * <p>Une action « faite » dit que le geste a eu lieu ; elle ne dit pas qu'il a
 * produit son effet. Chaque action porte sa propre exigence de vérification
 * (ADR 0131), et le dossier ne se clôture plus tant qu'une action qui en a
 * promis une ne l'a pas obtenue : c'est ici que cette promesse se solde.
 *
 * <p><b>Le verdict a trois états</b>, comme celui du dossier : une action qui a
 * réduit le problème sans le supprimer n'est ni efficace ni inefficace, et
 * forcer le choix entre les deux ferait écrire une fausse réponse. Seul
 * « efficace » lève l'obstacle à la clôture ; les deux autres consignent ce qui
 * a été vu, et le disent avant d'être enregistrés.
 *
 * <p><b>Le relevé est obligatoire</b> : un verdict sans mesure n'est pas un
 * jugement mais une opinion. Le critère, lui, n'est pas saisissable — il a été
 * posé à l'écriture de l'action, et le rouvrir ici reviendrait à choisir la
 * cible après avoir vu le résultat.
 */
@Component({
  selector: 'qos-capa-action-verification-dialog',
  templateUrl: './capa-action-verification-dialog.component.html',
  styleUrls: ['./capa-action-verification-dialog.component.scss'],
  standalone: false
})
export class CapaActionVerificationDialogComponent {

  readonly verdicts: ReadonlyArray<EffectivenessVerdict> = ['EFFECTIVE', 'PARTIAL', 'INEFFECTIVE'];
  readonly submitting$ = new BehaviorSubject<boolean>(false);

  /**
   * Le verdict vit HORS du formulaire : trois boutons à état pressé disent les
   * trois réponses d'un coup, là où une liste déroulante les cacherait derrière
   * un clic — et c'est la question principale de cet écran.
   */
  verdict?: EffectivenessVerdict;

  readonly form = this.fb.nonNullable.group({
    baseline: ['', [Validators.maxLength(120)]],
    target: ['', [Validators.maxLength(120)]],
    // Obligatoire : le verdict repose sur une mesure. La borne de 120
    // caractères est celle du domaine — un relevé tient sur une ligne de
    // tableau ; au-delà, c'est un commentaire.
    observed: ['', [Validators.required, Validators.maxLength(120)]],
    comment: ['', [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly capa: CapaService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CapaActionVerificationDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) readonly data: CapaActionVerificationDialogData
  ) {}

  /** Le critère contre lequel on juge : celui de l'action, sinon celui du dossier. */
  get critere(): string {
    return (this.data.action.effectivenessCriterion || this.data.caseCriterion || '').trim();
  }

  /**
   * Vrai quand aucun critère n'a été posé, ni sur l'action ni sur le dossier.
   *
   * <p>Le cas des actions antérieures aux colonnes de la V177. Le serveur
   * refuse, et l'écran le dit AVANT le clic plutôt que de faire découvrir le
   * refus après : il faut alors reprendre l'action pour y poser sa mesure.
   */
  get sansCritere(): boolean {
    return this.critere === '';
  }

  choose(verdict: EffectivenessVerdict): void {
    this.verdict = verdict;
  }

  verdictClass(verdict: EffectivenessVerdict): string {
    switch (verdict) {
      // Les memes tons que le plan de travail des evaluations : une couleur
      // qui dirait « efficace » ici et autre chose la-bas se lirait comme deux
      // verdicts differents.
      case 'EFFECTIVE': return 'tone-ok';
      case 'PARTIAL': return 'tone-warn';
      default: return 'tone-bad';
    }
  }

  /** Un verdict ET un relevé, et un critère auquel le confronter. */
  get complete(): boolean {
    return !!this.verdict && this.form.valid && !this.sansCritere;
  }

  submit(): void {
    if (!this.complete || this.submitting$.value) {
      return;
    }
    const v = this.form.getRawValue();
    this.submitting$.next(true);
    this.capa.verifyActionEffectiveness(this.data.caseId, this.data.action.id, {
      verdict: this.verdict!,
      observed: v.observed.trim(),
      baseline: v.baseline.trim() || undefined,
      target: v.target.trim() || undefined,
      comment: v.comment.trim() || undefined
    }).pipe(finalize(() => this.submitting$.next(false))).subscribe({
      next: () => {
        this.snack.open(
          $localize`:@@capa.action-verification.saved:Vérification enregistrée.`,
          $localize`:@@common.ok:OK`, { duration: 2000 });
        this.dialogRef.close(true);
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[capa-action-verification] save failed', err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(
            err,
            $localize`:@@capa.action-verification.failed:La vérification n'a pas pu être enregistrée.`),
          $localize`:@@common.close:Fermer`, { duration: 4000 });
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
