import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import {
  AiPrediction, AlignmentBar, ComplianceHeatCell, DefectByCategory,
  KpiCard, QualityTrendPoint, TopRisk
} from './dashboard.types';

/**
 * Service du dashboard exécutif.
 *
 * Pour le MVP, les KPIs sont mockés (le pipeline KPI engine + Kafka Streams
 * arrive en V2). API cible : `/api/v1/kpis/executive`.
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {

  getExecutiveKpis(): Observable<KpiCard[]> {
    const data: KpiCard[] = [
      {
        id: 'coq', label: 'Coût d\'obtention qualité',
        value: 2.8, unit: '% CA', target: 3.2, trend: -0.4,
        description: 'Prévention + détection + défaillances',
        icon: 'paid', state: 'good', trendInvertedIsGood: true
      },
      {
        id: 'fpy', label: 'First Pass Yield',
        value: 94.2, unit: '%', target: 95, trend: 2.1,
        description: 'Réussite en premier passage sur 30j',
        icon: 'verified', state: 'good'
      },
      {
        id: 'nc', label: 'Non-conformités ouvertes',
        value: 127, unit: '', target: 100, trend: -12,
        description: 'Sur les 30 derniers jours',
        icon: 'warning', state: 'good', trendInvertedIsGood: true
      },
      {
        id: 'capa', label: 'Délai moyen clôture CAPA',
        value: 22, unit: 'jours', target: 30, trend: -4.3,
        description: 'Actions critiques + hautes',
        icon: 'engineering', state: 'good', trendInvertedIsGood: true
      },
      {
        id: 'cpk', label: 'Cp / Cpk procédés clés',
        value: '1.42', unit: '', target: 1.33, trend: 0.08,
        description: 'Capabilité moyenne 12 processus',
        icon: 'analytics', state: 'good'
      },
      {
        id: 'audits', label: 'Audits réalisés vs planifiés',
        value: 87, unit: '%', target: 90, trend: 5,
        description: 'Année en cours',
        icon: 'fact_check', state: 'warn'
      },
      {
        id: 'iso9001', label: 'Alignement ISO 9001',
        value: 76, unit: '%', target: 90, trend: 3.2,
        description: 'Couverture des exigences obligatoires',
        icon: 'workspace_premium', state: 'warn'
      },
      {
        id: 'aiact', label: 'AI Act readiness',
        value: 68, unit: '%', target: 100, trend: 8.5,
        description: 'Systèmes HIGH risk documentés',
        icon: 'smart_toy', state: 'warn'
      }
    ];
    return of(data).pipe(delay(120));
  }

  getQualityTrend(): Observable<QualityTrendPoint[]> {
    const labels = ['Juin', 'Juil', 'Août', 'Sept', 'Oct', 'Nov',
                    'Déc', 'Janv', 'Févr', 'Mars', 'Avr', 'Mai'];
    const values = [88.4, 89.1, 90.2, 89.7, 91.5, 92.0,
                    92.8, 93.1, 93.4, 93.6, 93.8, 94.2];
    return of(labels.map((m, i) => ({ month: m, value: values[i], target: 95 })))
      .pipe(delay(120));
  }

  getDefectsByCategory(): Observable<DefectByCategory[]> {
    return of([
      { category: 'Matière',    count: 42 },
      { category: 'Méthode',    count: 31 },
      { category: 'Main d\'œuvre', count: 27 },
      { category: 'Machine',    count: 19 },
      { category: 'Milieu',     count: 14 },
      { category: 'Mesure',     count: 9  }
    ]).pipe(delay(120));
  }

  /**
   * Drill-down niveau 2 : sous-causes d'une catégorie 6M de défaut.
   * Synchrone (données du référentiel local) — alimente la vue détail quand
   * une catégorie est sélectionnée par cross-filtering (§7.3).
   */
  getDefectSubcategoriesSync(category: string): DefectByCategory[] {
    const map: Record<string, DefectByCategory[]> = {
      'Matière': [
        { category: 'Lot fournisseur hors spéc.', count: 18 },
        { category: 'Stockage / humidité', count: 14 },
        { category: 'Mélange / dosage', count: 10 }
      ],
      'Méthode': [
        { category: 'Gamme obsolète', count: 13 },
        { category: 'Réglage opérateur', count: 11 },
        { category: 'Instruction manquante', count: 7 }
      ],
      'Main d\'œuvre': [
        { category: 'Formation insuffisante', count: 12 },
        { category: 'Erreur de saisie', count: 9 },
        { category: 'Fatigue / 3x8', count: 6 }
      ],
      'Machine': [
        { category: 'Usure outil', count: 9 },
        { category: 'Dérive capteur', count: 6 },
        { category: 'Maintenance en retard', count: 4 }
      ],
      'Milieu': [
        { category: 'Température atelier', count: 7 },
        { category: 'Poussière / propreté', count: 5 },
        { category: 'Vibrations', count: 2 }
      ],
      'Mesure': [
        { category: 'Calibration expirée', count: 5 },
        { category: 'Méthode de contrôle', count: 3 },
        { category: 'Incertitude élevée', count: 1 }
      ]
    };
    return map[category] ?? [];
  }

  /** Heatmap conformité — score 0..100 par clause de norme. */
  getComplianceHeatmap(): Observable<ComplianceHeatCell[]> {
    const norms = ['ISO 9001', 'ISO 27001', 'ISO 14001', 'IATF 16949', 'AI Act'];
    const clauses = ['§4', '§5', '§6', '§7', '§8', '§9', '§10'];
    const seed = [
      [88, 92, 78, 84, 71, 65, 70],
      [62, 58, 72, 80, 55, 60, 65],
      [70, 75, 68, 78, 82, 60, 72],
      [85, 80, 72, 88, 90, 75, 70],
      [45, 55, 62, 70, 50, 68, 72]
    ];
    const cells: ComplianceHeatCell[] = [];
    norms.forEach((norm, i) =>
      clauses.forEach((cl, j) => cells.push({ norm, clause: cl, score: seed[i][j] })));
    return of(cells).pipe(delay(120));
  }

  getTopRisks(): Observable<TopRisk[]> {
    return of<TopRisk[]>([
      { id: 'r1', title: 'Dérive SPC ligne d\'extrusion B',
        source: 'IoT Hub · Sigma déviation', severity: 'critical',
        due: '2026-05-19', owner: 'Atelier 3' },
      { id: 'r2', title: 'Audit ISO 27001 en retard',
        source: 'Standards Hub', severity: 'high',
        due: '2026-05-22', owner: 'RSSI' },
      { id: 'r3', title: 'CAPA récidive — fournisseur Alpha',
        source: 'Supplier Quality', severity: 'high',
        due: '2026-05-25', owner: 'Achats' },
      { id: 'r4', title: 'FRIA manquante — système RH (HIGH risk)',
        source: 'AI Act · Art. 27', severity: 'critical',
        due: '2026-05-30', owner: 'DPO' },
      { id: 'r5', title: 'Calibration multimètre M-2241 expirée',
        source: 'Calibration', severity: 'medium',
        due: '2026-05-21', owner: 'Métrologie' }
    ]).pipe(delay(120));
  }

  getAiPredictions(): Observable<AiPrediction[]> {
    return of<AiPrediction[]>([
      { id: 'p1', kind: 'drift',
        title: 'Sortie limites SPC — ligne B',
        detail: 'Probabilité 87 % d\'excursion 2σ sur la prochaine semaine.',
        confidence: 0.87, horizon: '7 jours', state: 'bad' },
      { id: 'p2', kind: 'objective',
        title: 'Objectif FPY atteint',
        detail: 'Probabilité 92 % d\'atteindre 95 % de FPY au 30 juin.',
        confidence: 0.92, horizon: '45 jours', state: 'good' },
      { id: 'p3', kind: 'supplier',
        title: 'Risque fournisseur Beta',
        detail: 'Hausse du risque NC matière (modèle XGBoost).',
        confidence: 0.71, horizon: '30 jours', state: 'warn' },
      { id: 'p4', kind: 'complaint',
        title: 'Cluster réclamations cosmétique',
        detail: 'NLP détecte un pattern émergent sur la finition (n=14).',
        confidence: 0.68, horizon: '14 jours', state: 'warn' }
    ]).pipe(delay(120));
  }

  getAlignmentBars(): Observable<AlignmentBar[]> {
    return of<AlignmentBar[]>([
      { standardCode: 'iso-9001',   standardName: 'ISO 9001:2015',       score: 76, status: 'IN_PROGRESS' },
      { standardCode: 'iso-27001',  standardName: 'ISO/IEC 27001:2022',  score: 62, status: 'IN_PROGRESS' },
      { standardCode: 'iso-14001',  standardName: 'ISO 14001:2015',      score: 71, status: 'IN_PROGRESS' },
      { standardCode: 'iatf-16949', standardName: 'IATF 16949:2016',     score: 80, status: 'IN_PROGRESS' },
      { standardCode: 'ai-act',     standardName: 'EU AI Act 2024/1689', score: 58, status: 'PLANNING'    }
    ]).pipe(delay(120));
  }
}
