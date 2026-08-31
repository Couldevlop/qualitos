import { Component, OnInit } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { FMEA_EXAMPLE_ROWS, FMEA_EXAMPLE_TITLE } from '../../fmea.reference';
import { FmeaService } from '../../fmea.service';
import { FmeaScaleKind, FmeaScaleRow, FmeaScaleView } from '../../fmea.types';

/**
 * Le référentiel de cotation FMEA, consultable sans quitter l'analyse en cours,
 * et modifiable par ceux qui en répondent.
 *
 * <p>Coter en Sévérité, Occurrence et Détection ne veut rien dire sans l'échelle
 * qui donne le sens des chiffres : un « 8 » de l'un n'est pas le « 8 » de
 * l'autre, et deux RPN cotés sur des barèmes différents ne se comparent pas.
 *
 * <p>Le barème appartient au TENANT. « Perturbation majeure du service » ne
 * recouvre pas la même réalité dans un atelier de sertissage, un laboratoire
 * d'analyses et un centre d'appels ; un barème imposé produit des cotations que
 * personne ne croit. Tant qu'une organisation n'a rien redéfini, elle voit —
 * et cote sur — le barème de référence.
 *
 * <p>L'exemple de PFMEA, lui, reste commun : c'est un modèle de rédaction, pas
 * une règle de cotation.
 */
@Component({
  selector: 'qos-fmea-reference-dialog',
  templateUrl: './fmea-reference-dialog.component.html',
  styleUrls: ['./fmea-reference-dialog.component.scss'],
  standalone: false
})
export class FmeaReferenceDialogComponent implements OnInit {

  readonly exampleTitle = FMEA_EXAMPLE_TITLE;
  readonly exampleRows = FMEA_EXAMPLE_ROWS;

  /** Les trois échelles, dans l'ordre où le RPN les multiplie. */
  readonly kinds: FmeaScaleKind[] = ['SEVERITY', 'OCCURRENCE', 'DETECTION'];

  // Les intitulés d'onglet se calculent en TypeScript : `i18n-label` sur un
  // `mat-tab` répété ne peut pas varier avec la ligne.
  readonly severityLabel = $localize`:@@fmea.reference.tab-severity:Sévérité`;
  readonly occurrenceLabel = $localize`:@@fmea.reference.tab-occurrence:Occurrence`;
  readonly detectionLabel = $localize`:@@fmea.reference.tab-detection:Détection`;

  readonly loading$ = new BehaviorSubject<boolean>(false);
  readonly saving$ = new BehaviorSubject<boolean>(false);
  readonly error$ = new BehaviorSubject<string | null>(null);

  /** L'échelle en cours d'édition ; aucune en lecture seule. */
  editing: FmeaScaleKind | null = null;

  /** Copie de travail : rien n'est modifié tant que rien n'est enregistré. */
  draft: FmeaScaleRow[] = [];

  private scalesByKind = new Map<FmeaScaleKind, FmeaScaleView>();

  constructor(
    private readonly fmea: FmeaService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<FmeaReferenceDialogComponent>
  ) {}

  ngOnInit(): void {
    this.load();
  }

  /**
   * Redéfinir un barème rend incomparables les RPN cotés avant et après : c'est
   * une décision de politique qualité, réservée à la direction qualité et à
   * l'administration du tenant. Le serveur reste l'autorité et répondra 403 ;
   * cacher le bouton évite seulement une promesse non tenue.
   */
  get canEdit(): boolean {
    return this.auth.hasAnyRole(['DIRECTOR_QUALITY', 'QUALITY_DIRECTOR',
      'ADMIN_TENANT', 'SUPER_ADMIN']);
  }

  scale(kind: FmeaScaleKind): FmeaScaleView | undefined {
    return this.scalesByKind.get(kind);
  }

  rows(kind: FmeaScaleKind): FmeaScaleRow[] {
    return this.editing === kind ? this.draft : (this.scale(kind)?.rows ?? []);
  }

  /** Vrai quand l'organisation cote sur SON barème et non sur celui de référence. */
  isCustom(kind: FmeaScaleKind): boolean {
    return this.scale(kind)?.custom === true;
  }

  isEditing(kind: FmeaScaleKind): boolean {
    return this.editing === kind;
  }

  startEditing(kind: FmeaScaleKind): void {
    // Copie profonde : abandonner l'édition doit rendre l'écran exactement tel
    // qu'il était, et non tel que les champs l'ont laissé.
    this.draft = (this.scale(kind)?.rows ?? []).map(row => ({ ...row }));
    this.editing = kind;
  }

  cancelEditing(): void {
    this.editing = null;
    this.draft = [];
  }

  /**
   * Un barème se lit ligne à ligne : chaque score doit porter son intitulé.
   * Le référentiel d'origine en regroupe certains sous l'intitulé précédent —
   * ce qui se lit sur un tableau imprimé, mais laisse ici des cases vides que
   * l'organisation doit nommer avant d'adopter le barème.
   */
  get incompleteScores(): number[] {
    return this.draft
      .filter(row => !row.label || !row.label.trim())
      .map(row => row.score);
  }

  get canSave(): boolean {
    return this.editing !== null && this.incompleteScores.length === 0;
  }

  save(): void {
    const kind = this.editing;
    if (!kind || !this.canSave || this.saving$.value) return;

    this.saving$.next(true);
    this.fmea.replaceRatingScale(kind, this.draft.map(row => ({
      score: row.score,
      label: (row.label ?? '').trim(),
      description: row.description?.trim() || undefined,
      timePeriod: row.timePeriod?.trim() || undefined,
      failureRate: row.failureRate?.trim() || undefined
    })))
      .pipe(finalize(() => this.saving$.next(false)))
      .subscribe({
        next: saved => {
          this.scalesByKind.set(kind, saved);
          this.cancelEditing();
          this.snack.open(
            $localize`:@@fmea.reference.saved:Barème enregistré.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fmea-reference] save failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@fmea.reference.save-failed:Barème refusé.`),
            $localize`:@@common.ok:OK`, { duration: 5000 });
        }
      });
  }

  /** Revient au barème de référence : on supprime, on ne recopie pas. */
  revert(kind: FmeaScaleKind): void {
    if (this.saving$.value) return;
    this.saving$.next(true);
    this.fmea.revertRatingScale(kind)
      .pipe(finalize(() => this.saving$.next(false)))
      .subscribe({
        next: reverted => {
          this.scalesByKind.set(kind, reverted);
          this.cancelEditing();
          this.snack.open(
            $localize`:@@fmea.reference.reverted:Barème de référence rétabli.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
        },
        error: err => this.snack.open(
          safeErrorMessage(err, $localize`:@@fmea.reference.save-failed:Barème refusé.`),
          $localize`:@@common.ok:OK`, { duration: 5000 })
      });
  }

  close(): void {
    this.dialogRef.close();
  }

  /**
   * Classe de la pastille de score : un barème se lit d'abord par la couleur,
   * du plus grave au plus anodin. Les mêmes seuils que la criticité des items.
   */
  scoreClass(score: number): string {
    if (score >= 9) return 'score score--critical';
    if (score >= 7) return 'score score--high';
    if (score >= 4) return 'score score--medium';
    return 'score score--low';
  }

  /** Un RPN élevé se signale : c'est le seul chiffre qui hiérarchise l'exemple. */
  rpnClass(rpn: number): string {
    return rpn >= 200 ? 'rpn rpn--critical' : rpn >= 100 ? 'rpn rpn--high' : 'rpn';
  }

  private load(): void {
    this.loading$.next(true);
    this.error$.next(null);
    this.fmea.ratingScales()
      .pipe(finalize(() => this.loading$.next(false)))
      .subscribe({
        next: reference => {
          const next = new Map<FmeaScaleKind, FmeaScaleView>();
          for (const scale of reference.scales) next.set(scale.kind, scale);
          this.scalesByKind = next;
        },
        error: err => this.error$.next(safeErrorMessage(err,
          $localize`:@@fmea.reference.load-failed:Référentiel de cotation indisponible.`))
      });
  }
}
