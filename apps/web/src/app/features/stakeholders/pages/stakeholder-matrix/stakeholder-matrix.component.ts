import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { categoryLabel, impactChip, impactLabel } from '../../stakeholders.labels';
import { StakeholdersService } from '../../stakeholders.service';
import {
  IMPACT_LEVELS,
  ImpactLevel,
  StakeholderRating,
  StakeholderRow
} from '../../stakeholders.types';

/** Les deux notes cotables d'une ligne. La criticité, elle, n'est pas saisie. */
export type RatedField = 'qualityImpact' | 'regulatoryImpact';

/** Une case de la grille de répartition : combien de lignes s'y trouvent. */
export interface MatrixCell {
  quality: ImpactLevel;
  regulatory: ImpactLevel;
  count: number;
}

/**
 * La matrice d'évaluation : deux notes par partie prenante — impact qualité,
 * impact réglementaire — cotées **en lot**.
 *
 * <p>Coter est un geste de revue : on parcourt le registre, on ajuste quinze
 * lignes, on enregistre une fois. Envoyer chaque case au serveur produirait
 * quinze appels, quinze recalculs de criticité et quinze lignes de journal pour
 * une seule décision.
 *
 * <p>La criticité n'est jamais saisie : le serveur la déduit des deux impacts (le
 * plus élevé des deux, ADR 0104). Elle s'affiche en lecture seule, et une ligne
 * encore en attente le dit plutôt que d'afficher une valeur recalculée à
 * l'avance qui ne correspondrait à rien d'enregistré.
 */
@Component({
  selector: 'qos-stakeholder-matrix',
  templateUrl: './stakeholder-matrix.component.html',
  styleUrls: ['./stakeholder-matrix.component.scss'],
  standalone: false
})
export class StakeholderMatrixComponent implements OnChanges {

  @Input() rows: StakeholderRow[] = [];
  @Input() canManage = false;

  @Output() readonly rated = new EventEmitter<StakeholderRow[]>();

  readonly impacts = IMPACT_LEVELS;
  readonly categoryLabel = categoryLabel;
  readonly impactLabel = impactLabel;
  readonly impactChip = impactChip;

  saving = false;

  /** Les lignes RÉELLEMENT modifiées, et elles seules : c'est ce qui partira. */
  private readonly edits = new Map<string, StakeholderRating>();

  constructor(
    private readonly svc: StakeholdersService,
    private readonly snack: MatSnackBar
  ) {}

  /**
   * Le registre a été rechargé : les cotations en attente sont abandonnées.
   *
   * <p>Elles portaient sur les valeurs d'avant. Les garder reviendrait à
   * réappliquer, sur des lignes rafraîchies, des notes dont l'utilisateur ne voit
   * plus l'origine — et à écraser silencieusement ce qu'un collègue vient de coter.
   */
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['rows']) {
      this.edits.clear();
    }
  }

  // ---------- cotation ----------

  /** La note affichée : celle en attente si elle existe, sinon celle du serveur. */
  level(row: StakeholderRow, field: RatedField): ImpactLevel {
    return this.edits.get(row.id)?.[field] ?? row[field];
  }

  /**
   * Note une case. Une note ramenée à sa valeur d'origine n'est plus une
   * modification : la ligne quitte le lot, et n'est pas renvoyée au serveur.
   */
  onLevel(row: StakeholderRow, field: RatedField, value: ImpactLevel): void {
    if (!this.canManage) {
      return;
    }
    const shown: StakeholderRating = {
      qualityImpact: this.level(row, 'qualityImpact'),
      regulatoryImpact: this.level(row, 'regulatoryImpact')
    };
    const rating: StakeholderRating = field === 'qualityImpact'
      ? { ...shown, qualityImpact: value }
      : { ...shown, regulatoryImpact: value };
    if (rating.qualityImpact === row.qualityImpact
        && rating.regulatoryImpact === row.regulatoryImpact) {
      this.edits.delete(row.id);
      return;
    }
    this.edits.set(row.id, rating);
  }

  pending(row: StakeholderRow): boolean {
    return this.edits.has(row.id);
  }

  get pendingCount(): number {
    return this.edits.size;
  }

  get pendingMessage(): string {
    return $localize`:@@stakeholders.matrix.pending:Lignes recotées, en attente d'enregistrement : ${this.edits.size}:nombre:`;
  }

  save(): void {
    if (!this.canManage || !this.edits.size || this.saving) {
      return;
    }
    this.saving = true;
    const ratings: Record<string, StakeholderRating> = {};
    this.edits.forEach((rating, id) => (ratings[id] = rating));
    this.svc.rate(ratings)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: rows => {
          this.edits.clear();
          this.snack.open($localize`:@@stakeholders.matrix.saved:Cotation enregistrée ; la criticité a été recalculée.`,
            undefined, { duration: 3000 });
          // Le registre reprend les lignes que le serveur rend : lui seul connaît
          // la criticité, et il vient de la recalculer.
          this.rated.emit(rows);
        },
        error: err => this.snack.open(
          safeErrorMessage(err, $localize`:@@stakeholders.matrix.save-failed:La cotation n'a pas pu être enregistrée.`),
          $localize`:@@common.ok:OK`, { duration: 5000 })
      });
  }

  reset(): void {
    this.edits.clear();
  }

  // ---------- répartition ----------

  /**
   * Les neuf cases de la grille, dans l'ordre de lecture : le réglementaire en
   * lignes, du plus élevé au plus faible, la qualité en colonnes.
   */
  get grid(): MatrixCell[][] {
    const regulatory = [...IMPACT_LEVELS].reverse();
    return regulatory.map(r => IMPACT_LEVELS.map(q => ({
      quality: q,
      regulatory: r,
      count: this.rows.filter(row => this.level(row, 'qualityImpact') === q
                                  && this.level(row, 'regulatoryImpact') === r).length
    })));
  }

  trackById(_index: number, row: StakeholderRow): string {
    return row.id;
  }

  trackByCell(_index: number, cell: MatrixCell): string {
    return `${cell.quality}-${cell.regulatory}`;
  }
}
