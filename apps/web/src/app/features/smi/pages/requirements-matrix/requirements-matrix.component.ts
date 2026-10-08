import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { PageBreadcrumb } from '../../../../shared/ui/page-header/page-header.component';
import { StandardsService } from '../../../standards/standards.service';
import { ClauseAlignment } from '../../../standards/standards.types';
import { chapterLabel, coverageLabel, moduleLabel } from '../../smi.labels';
import { SmiService } from '../../smi.service';
import { ChapterRow, CoverageStatus, MatrixCell, RequirementsMatrix, StandardRef } from '../../smi.types';

/** La case ouverte dans le panneau de détail. */
export interface SelectedCell {
  row: ChapterRow;
  cell: MatrixCell;
  standard: StandardRef;
}

/**
 * La matrice des exigences : « une preuve, plusieurs référentiels » (§8.9).
 *
 * <p>Les chapitres communs de la structure-cadre (4 à 10) en lignes, chaque
 * norme adoptée en colonne, et le module de QualitOS où se fabriquent les
 * preuves. Une case dit si le chapitre est couvert, partiel ou en écart pour
 * la norme ; la choisir ouvre le détail — les clauses encore à prouver, lues
 * dans l'alignement de la norme — et deux suites : compléter les preuves dans
 * le Standards Hub, ou ouvrir une action CAPA.
 */
@Component({
  selector: 'qos-smi-requirements-matrix',
  templateUrl: './requirements-matrix.component.html',
  styleUrls: ['./requirements-matrix.component.scss'],
  standalone: false
})
export class RequirementsMatrixComponent implements OnInit, OnDestroy {

  matrix: RequirementsMatrix | null = null;
  loading = false;
  error: string | null = null;

  selected: SelectedCell | null = null;
  /** Les clauses du chapitre choisi qui ne sont pas entièrement prouvées. */
  gaps: ClauseAlignment[] = [];
  gapsLoading = false;
  gapsFailed = false;

  private gapsSub?: Subscription;

  constructor(
    private readonly service: SmiService,
    private readonly standards: StandardsService
  ) {}

  ngOnInit(): void {
    this.charger();
  }

  ngOnDestroy(): void {
    this.gapsSub?.unsubscribe();
  }

  charger(): void {
    this.loading = true;
    this.error = null;
    this.service.requirementsMatrix().subscribe({
      next: m => {
        this.matrix = m;
        this.loading = false;
        this.selectFirstGap();
      },
      error: err => {
        this.loading = false;
        this.error = safeErrorMessage(err,
          $localize`:@@smi.req.failed:La matrice des exigences n'a pas pu être chargée.`);
      }
    });
  }

  /** À l'ouverture, le détail montre le premier écart (sinon le premier partiel) : c'est là qu'il faut agir. */
  private selectFirstGap(): void {
    const m = this.matrix;
    if (!m) return;
    for (const statut of ['GAP', 'PARTIAL'] as CoverageStatus[]) {
      for (const row of m.rows) {
        const i = row.cells.findIndex(c => c.status === statut);
        if (i >= 0) {
          this.select(row, row.cells[i], i);
          return;
        }
      }
    }
  }

  select(row: ChapterRow, cell: MatrixCell, index: number): void {
    const standard = this.matrix?.standards[index];
    if (!standard || cell.status === 'NOT_APPLICABLE') return;
    this.selected = { row, cell, standard };
    this.gaps = [];
    this.gapsFailed = false;
    if (cell.status === 'COVERED') return;
    this.gapsLoading = true;
    this.gapsSub?.unsubscribe();
    this.gapsSub = this.standards.getAlignment(standard.adoptionId).subscribe({
      next: a => {
        const section = a.sections.find(s => s.sectionCode === row.chapter);
        this.gaps = (section?.clauses ?? []).filter(c => c.coveredRequirements < c.totalRequirements);
        this.gapsLoading = false;
      },
      error: () => {
        this.gapsLoading = false;
        this.gapsFailed = true;
      }
    });
  }

  isSelected(row: ChapterRow, index: number): boolean {
    return !!this.selected && this.selected.row.chapter === row.chapter
      && this.selected.standard.code === this.matrix?.standards[index]?.code;
  }

  /** « Créer une action CAPA » ouvre la création CAPA préremplie ; rien n'est créé sans validation. */
  get capaQuery(): Record<string, string> {
    const s = this.selected;
    if (!s) return {};
    const chapitre = chapterLabel(s.row.chapter);
    return {
      nouveau: '1',
      titre: $localize`:@@smi.req.capa-title:${coverageLabel(s.cell.status)}:status: ${s.standard.name}:standard: · ${s.row.chapter}:chapter: ${chapitre}:title:`,
      ref: `${s.standard.code} §${s.row.chapter}`,
      description: $localize`:@@smi.req.capa-description:${s.cell.covered}:covered: exigence(s) prouvée(s) sur ${s.cell.total}:total: au chapitre ${s.row.chapter}:chapter: de ${s.standard.name}:standard:.`
    };
  }

  /** « 4 · Contexte de l'organisme » */
  chapterText(code: string): string {
    return `${code} · ${chapterLabel(code)}`;
  }

  modulesText(codes: string[]): string {
    return codes.map(moduleLabel).join(' · ');
  }

  coverageText = coverageLabel;

  cellLabel(row: ChapterRow, cell: MatrixCell, index: number): string {
    const norme = this.matrix?.standards[index]?.name ?? cell.standardCode;
    return $localize`:@@smi.req.cell-label:${norme}:standard:, chapitre ${row.chapter}:chapter: : ${coverageLabel(cell.status)}:status:, ${cell.covered}:covered: sur ${cell.total}:total:`;
  }

  get breadcrumbs(): PageBreadcrumb[] {
    return [
      { label: $localize`:@@smi.crumb:Tableau de bord SMI`, route: '/smi' },
      { label: $localize`:@@smi.req.crumb:Matrice des exigences` }
    ];
  }

  trackByChapter(_i: number, r: ChapterRow): string {
    return r.chapter;
  }

  trackByCode(_i: number, s: { code?: string; standardCode?: string }): string {
    return s.code ?? s.standardCode ?? '';
  }

  trackByClause(_i: number, c: ClauseAlignment): string {
    return c.clauseId;
  }
}
