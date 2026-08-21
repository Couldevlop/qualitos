import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TrainingService } from '../../training.service';
import { CompetencyGrid, CompetencyGridRow } from '../../training.types';

/**
 * La matrice de compétences : compétences en lignes, groupées par famille,
 * collaborateurs en colonnes, un niveau à l'intersection.
 *
 * <p>La figure se lit dans les deux sens, et c'est ce qui la justifie. Une
 * colonne montre ce qu'une personne couvre. Une LIGNE montre qui sait faire
 * quoi — et une seule case remplie sur toute une rangée signale une compétence
 * qui ne tient qu'à une personne. Ce n'est pas une donnée, c'est un risque
 * d'organisation, et la matrice le rend visible sans qu'on le cherche.
 *
 * <p>Une case vide reste vide : la confondre avec un zéro affirmerait une
 * incompétence que personne n'a constatée.
 */
@Component({
  selector: 'qos-competency-matrix',
  templateUrl: './competency-matrix.component.html',
  styleUrls: ['./competency-matrix.component.scss'],
  standalone: false
})
export class CompetencyMatrixComponent implements OnInit {

  /**
   * Les niveaux tels qu'ils sont STOCKÉS : zéro à quatre, chacun nommé. La
   * trame papier note de un à cinq ; afficher cette échelle-là supposerait de
   * décaler les valeurs enregistrées, c'est-à-dire d'afficher autre chose que ce
   * que le serveur détient.
   */
  readonly levelNames = ['Aucun', 'Sensibilisé', 'Pratiquant', 'Autonome', 'Expert'];

  grid?: CompetencyGrid;
  loading = false;
  onlyAtRisk = false;

  constructor(
    private readonly service: TrainingService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.service.competencyMatrix().subscribe({
      next: grid => {
        this.grid = grid;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snack.open(
          $localize`:@@training.matrix.failed:Impossible de charger la matrice de compétences.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  /** Les lignes visibles d'un groupe : le filtre travaille sur la grille chargée. */
  rowsOf(rows: CompetencyGridRow[]): CompetencyGridRow[] {
    return this.onlyAtRisk ? rows.filter(row => row.singlePointOfKnowledge) : rows;
  }

  /** Un groupe entièrement filtré ne laisse pas un titre orphelin. */
  hasVisibleRows(rows: CompetencyGridRow[]): boolean {
    return this.rowsOf(rows).length > 0;
  }

  levelLabel(level: number | null): string {
    return level === null || level === undefined ? '' : this.levelNames[level] ?? String(level);
  }

  /** Classe de la cellule : le vide se distingue du zéro, qui est une note. */
  cellClass(level: number | null): string {
    if (level === null || level === undefined) return 'cell-unknown';
    return level === 0 ? 'cell-none' : `cell-l${level}`;
  }

  atRiskCount(): number {
    return (this.grid?.groups ?? [])
      .reduce((total, group) => total + group.rows.filter(r => r.singlePointOfKnowledge).length, 0);
  }
}
