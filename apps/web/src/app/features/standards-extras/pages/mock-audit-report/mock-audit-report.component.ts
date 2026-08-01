import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import { StatusTone } from '../../../../shared/ui/status-pill/status-pill.component';
import { StandardsExtrasService } from '../../standards-extras.service';
import {
  MockAuditCriticality, MockAuditGap, MockAuditQuestion, MockAuditRemediation, MockAuditReport
} from '../../standards-extras.types';

/** Filtre de criticité de la vue des écarts. */
export type CriticalityFilter = MockAuditCriticality | 'ALL';

/**
 * Rapport d'un audit blanc IA (Standards Hub §8.4 onglet 7).
 *
 * Page à part entière plutôt que volet : un rapport porte 30 à 100 questions, la
 * gap analysis et le plan de remédiation — le lire dans un onglet réduirait
 * l'exercice à un aperçu. L'URL contient l'adoption et l'exécution, si bien qu'un
 * rapport se partage tel quel avec l'auditeur.
 *
 * Ce rapport est PERSISTÉ : deux exécutions successives restent comparables. Il ne
 * remplace pas l'audit blanc « règles » de la fiche de norme, qui n'est qu'un calcul.
 */
@Component({
  selector: 'qos-mock-audit-report',
  templateUrl: './mock-audit-report.component.html',
  styleUrls: ['./mock-audit-report.component.scss'],
  standalone: false
})
export class MockAuditReportComponent implements OnInit {

  report: MockAuditReport | null = null;
  visibleGaps: MockAuditGap[] = [];
  filter: CriticalityFilter = 'ALL';

  /** Titre affiché tant que le rapport n'est pas chargé (l'entrée du header est liée). */
  readonly reportFallbackTitle =
    $localize`:@@standards-extras.report.title-fallback:Rapport d'audit blanc IA`;

  /** Clauses dont les questions ciblées sont dépliées. */
  private readonly expanded = new Set<string>();

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = this.errorState$.asObservable();

  private adoptionId = '';
  private runId = '';

  constructor(
    private readonly svc: StandardsExtrasService,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Les paramètres sont suivis en continu : passer d'un rapport à l'autre ne doit
    // pas dépendre d'une reconstruction du composant.
    this.route.paramMap.subscribe(params => {
      this.adoptionId = params.get('adoptionId') ?? '';
      this.runId = params.get('runId') ?? '';
      this.load();
    });
  }

  load(): void {
    if (!this.adoptionId || !this.runId) {
      this.errorState$.next(
        $localize`:@@standards-extras.report.error-params:Rapport introuvable : lien incomplet.`);
      return;
    }
    this.errorState$.next(null);
    this.loadingState$.next(true);
    this.svc.mockAuditReport(this.adoptionId, this.runId)
      .pipe(finalize(() => this.loadingState$.next(false)))
      .subscribe({
        next: report => {
          this.report = report;
          this.applyFilter();
        },
        error: err => {
          this.report = null;
          this.visibleGaps = [];
          this.errorState$.next(safeErrorMessage(err,
            $localize`:@@standards-extras.report.error:Ce rapport d'audit blanc IA est introuvable.`));
        }
      });
  }

  // ---- Filtre de criticité ---------------------------------------------------

  setFilter(filter: CriticalityFilter): void {
    this.filter = filter;
    this.applyFilter();
  }

  private applyFilter(): void {
    const gaps = this.report?.gaps ?? [];
    this.visibleGaps = this.filter === 'ALL' ? gaps : gaps.filter(g => g.criticality === this.filter);
  }

  /** Nombre d'écarts d'une criticité : sert à n'afficher un filtre que s'il ramène quelque chose. */
  countFor(filter: CriticalityFilter): number {
    const gaps = this.report?.gaps ?? [];
    return filter === 'ALL' ? gaps.length : gaps.filter(g => g.criticality === filter).length;
  }

  // ---- Dépliage des questions ciblées ---------------------------------------

  toggleGap(gap: MockAuditGap): void {
    if (this.expanded.has(gap.clauseCode)) {
      this.expanded.delete(gap.clauseCode);
    } else {
      this.expanded.add(gap.clauseCode);
    }
  }

  isExpanded(gap: MockAuditGap): boolean {
    return this.expanded.has(gap.clauseCode);
  }

  // ---- Présentation ----------------------------------------------------------

  criticalityLabel(criticality: MockAuditCriticality): string {
    switch (criticality) {
      case 'MAJOR': return $localize`:@@standards-extras.criticality.major:NC majeure`;
      case 'MINOR': return $localize`:@@standards-extras.criticality.minor:NC mineure`;
      default: return $localize`:@@standards-extras.criticality.observation:Observation`;
    }
  }

  criticalityTone(criticality: MockAuditCriticality): StatusTone {
    switch (criticality) {
      case 'MAJOR': return 'danger';
      case 'MINOR': return 'warn';
      default: return 'neutral';
    }
  }

  /** Priorité CAPA telle que renvoyée par le serveur (high / medium / low). */
  priorityLabel(priority: string): string {
    switch (priority) {
      case 'high': return $localize`:@@standards-extras.priority.high:Haute`;
      case 'medium': return $localize`:@@standards-extras.priority.medium:Moyenne`;
      case 'low': return $localize`:@@standards-extras.priority.low:Basse`;
      default: return priority;
    }
  }

  readinessLabel(readiness: number): string {
    return `${Math.round(readiness)} %`;
  }

  readinessTone(readiness: number): StatusTone {
    if (readiness >= 80) return 'success';
    if (readiness >= 50) return 'warn';
    return 'danger';
  }

  /** Couverture d'une clause exprimée en pourcentage (le serveur renvoie un ratio [0, 1]). */
  coveragePercent(gap: MockAuditGap): number {
    return Math.round(gap.coverageRatio * 100);
  }

  /** Barre de progression décorative sans libellé propre : on lui en donne un explicite. */
  coverageAria(gap: MockAuditGap): string {
    return $localize`:@@standards-extras.report.coverage-aria:Couverture des exigences de la clause ${gap.clauseCode}:clause:`;
  }

  trackByGap(_index: number, gap: MockAuditGap): string {
    return gap.clauseCode;
  }

  trackByAction(_index: number, action: MockAuditRemediation): string {
    return `${action.clauseCode} ${action.targetModule}`;
  }

  trackByQuestion(_index: number, question: MockAuditQuestion): string {
    return `${question.clauseCode} ${question.question}`;
  }
}
