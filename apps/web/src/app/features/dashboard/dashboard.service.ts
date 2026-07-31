import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, map, shareReplay, switchMap } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  AiPrediction, AlignmentBar, ComplianceHeatCell, DefectByCategory,
  ExecutiveDashboardResponse, KpiCard, KpiState, QualityTrendPoint, TopRisk
} from './dashboard.types';

/**
 * Service du dashboard exécutif (§7.1).
 *
 * Il consomme `GET /api/v1/dashboards/executive`, qui agrège côté serveur les données
 * RÉELLES du tenant : catalogue KPI et ses mesures, non-conformités ouvertes, items
 * FMEA à RPN élevé, CAPA critiques en retard et scores d'alignement normatif.
 *
 * Historique : cette page était intégralement alimentée par des constantes codées en
 * dur (aucun appel HTTP), ce qui affichait des chiffres fictifs sur la vue de direction
 * et contredisait l'invariant §18.2 #8 — « aucun KPI affiché sans définition explicite ».
 * Les cartes proviennent désormais du catalogue KPI, donc chacune porte sa formule, sa
 * cible, ses seuils et son propriétaire.
 *
 * Un seul aller-retour alimente toutes les sections : la requête est partagée entre les
 * différents flux exposés ci-dessous, et rejouée à chaque appel de {@link refresh}.
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/dashboards/executive`;

  /** Déclencheur de (re)chargement — `refresh()` relance l'unique requête. */
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  /**
   * Requête partagée. `refCount: false` : les sections du dashboard s'abonnent à des
   * instants légèrement différents (certaines sont derrière un `*ngIf`) ; avec
   * `refCount: true`, un abonnement tardif relancerait un appel HTTP complet.
   */
  private readonly overview$: Observable<ExecutiveDashboardResponse> = this.reload$.pipe(
    switchMap(() => this.http.get<ExecutiveDashboardResponse>(this.endpoint).pipe(
      // Dégradation propre : la page reste affichable (sections vides) si l'API
      // est indisponible, plutôt que de laisser des spinners tourner sans fin.
      catchError(() => of(EMPTY_DASHBOARD))
    )),
    shareReplay({ bufferSize: 1, refCount: false })
  );

  constructor(private readonly http: HttpClient) {}

  /** Relance l'agrégation (bouton « rafraîchir », retour sur la page). */
  refresh(): void {
    this.reload$.next();
  }

  getExecutiveKpis(): Observable<KpiCard[]> {
    return this.overview$.pipe(map(d => d.kpis.map(toKpiCard)));
  }

  getQualityTrend(): Observable<QualityTrendPoint[]> {
    return this.overview$.pipe(map(d => d.qualityTrend.map(p => ({
      month: formatPeriod(p.periodStart),
      value: p.value ?? 0,
      target: p.targetValue ?? 0
    }))));
  }

  getDefectsByCategory(): Observable<DefectByCategory[]> {
    return this.overview$.pipe(map(d => d.defectsByCategory.map(x => ({
      category: x.category,
      count: x.count
    }))));
  }

  /**
   * Heatmap de conformité : une ligne par norme adoptée, une colonne par section de la
   * norme. Les scores viennent du moteur d'alignement du Standards Hub (§8.7).
   */
  getComplianceHeatmap(): Observable<ComplianceHeatCell[]> {
    return this.overview$.pipe(map(d => d.alignment.flatMap(bar =>
      (bar.sections ?? []).map(section => ({
        norm: bar.standardCode,
        clause: `§${section.sectionCode}`,
        score: Math.round(section.score)
      }))
    )));
  }

  getTopRisks(): Observable<TopRisk[]> {
    return this.overview$.pipe(map(d => d.topRisks.map(r => ({
      id: r.id,
      title: r.title,
      source: r.rpn != null ? `FMEA · RPN ${r.rpn}` : r.source,
      severity: toSeverity(r.severity),
      due: r.dueDate ?? undefined
    }))));
  }

  /**
   * Prédictions IA (§6.5). Aucune prédiction n'est fabriquée ici : les modèles réels
   * sont exposés par leurs propres pages (`/forecast` pour la prévision KPI,
   * `/anomaly` pour la détection d'anomalies, `/spc` pour la dérive). Tant que
   * l'agrégat exécutif ne les embarque pas, cette section reste vide — une absence
   * assumée plutôt que des chiffres inventés.
   */
  getAiPredictions(): Observable<AiPrediction[]> {
    return of([]);
  }

  getAlignmentBars(): Observable<AlignmentBar[]> {
    return this.overview$.pipe(map(d => d.alignment.map(b => ({
      standardCode: b.standardCode,
      standardName: b.standardName,
      score: Math.round(b.score),
      status: b.status
    }))));
  }

  /**
   * Drill-down niveau 2 (§7.3). Le référentiel NC ne modélise pas de sous-catégorie :
   * il n'y a donc rien de réel à afficher sous une catégorie 6M. La liste est vide tant
   * que le modèle ne porte pas cette dimension.
   */
  getDefectSubcategoriesSync(_category: string): DefectByCategory[] {
    return [];
  }
}

/** Réponse neutre utilisée quand l'API est indisponible. */
const EMPTY_DASHBOARD: ExecutiveDashboardResponse = {
  kpis: [], qualityTrend: [], defectsByCategory: [], topRisks: [], alignment: [],
  generatedAt: ''
};

/** Correspondance santé serveur → état visuel de la carte. */
function toState(health: string): KpiState {
  switch (health) {
    case 'OK': return 'good';
    case 'WARNING': return 'warn';
    case 'CRITICAL': return 'bad';
    default: return 'neutral';
  }
}

function toSeverity(severity: string): TopRisk['severity'] {
  switch (severity) {
    case 'CRITICAL': return 'critical';
    case 'HIGH': return 'high';
    default: return 'medium';
  }
}

/**
 * Icône de la carte, déduite de la catégorie du KPI. Le catalogue ne porte pas d'icône
 * (c'est une préoccupation de présentation, pas de définition d'indicateur).
 */
function iconFor(category: string | null): string {
  switch ((category ?? '').toLowerCase()) {
    case 'capa-actions': return 'engineering';
    case 'compliance': return 'workspace_premium';
    case 'audit': return 'fact_check';
    case 'supplier': return 'local_shipping';
    case 'risk': return 'warning';
    case 'cost': return 'paid';
    default: return 'monitoring';
  }
}

function toKpiCard(k: ExecutiveDashboardResponse['kpis'][number]): KpiCard {
  return {
    id: k.kpiId,
    label: k.name,
    // Une carte sans mesure reste affichée : c'est une information en soi (le KPI est
    // défini mais pas encore mesuré), et la masquer ferait mentir le catalogue.
    value: k.value ?? '—',
    unit: k.unit ?? '',
    trend: k.trendDelta ?? undefined,
    target: k.targetValue ?? undefined,
    description: k.description ?? '',
    icon: iconFor(k.category),
    state: toState(k.health),
    trendInvertedIsGood: k.direction === 'LOWER_IS_BETTER'
  };
}

/** Libellé d'axe : `2026-05` — court, trié naturellement, indépendant de la locale. */
function formatPeriod(iso: string | null): string {
  if (!iso) return '';
  return iso.slice(0, 7);
}
