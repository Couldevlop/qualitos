import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { NlqService } from '../nlq/nlq.service';
import { NlqAskResponse } from '../nlq/nlq.types';
import { NlqChartPlan } from './nlq-explore.types';

/**
 * Service d'exploration NLQ → graphique (CLAUDE.md §7.3).
 *
 * Il NE refait PAS l'appel HTTP : il délègue à {@link NlqService} (POST
 * /api/v1/ai/nlq/ask), seul détenteur du contrat d'API NLQ (text-to-SQL validé,
 * filtre tenant dérivé du JWT côté serveur — jamais dans le body, cf. règle 18.2).
 *
 * Sa valeur ajoutée est purement front : déduire des lignes renvoyées un plan de
 * graphe (catégories = 1re colonne textuelle, séries = colonnes numériques) afin
 * que la page restitue un graphique ECharts en plus de la table.
 */
@Injectable({ providedIn: 'root' })
export class NlqExploreService {

  constructor(private readonly nlq: NlqService) {}

  /** Pose la question au backend NLQ (délégation, pas de nouvel endpoint). */
  ask(question: string, maxRows = 100): Observable<NlqAskResponse> {
    return this.nlq.ask(question, maxRows);
  }

  /**
   * Déduit un plan de graphe d'un jeu de lignes.
   *
   * Heuristique (déterministe, sans IA) :
   *  - axe X = 1re colonne dont toutes les valeurs non nulles sont textuelles ;
   *  - séries = colonnes dont toutes les valeurs non nulles sont numériques ;
   *  - graphable si au moins 1 colonne numérique ET au moins 1 ligne. La colonne
   *    de catégories est optionnelle (à défaut, les points sont indexés 1..n).
   */
  buildPlan(rows: Array<Record<string, unknown>>): NlqChartPlan {
    if (!rows.length) {
      return { graphable: false, categoryColumn: null, valueColumns: [] };
    }
    const columns = Object.keys(rows[0]);
    const categoryColumn = columns.find(c => this.isTextColumn(rows, c)) ?? null;
    const valueColumns = columns.filter(
      c => c !== categoryColumn && this.isNumericColumn(rows, c)
    );
    return {
      graphable: valueColumns.length > 0,
      categoryColumn,
      valueColumns
    };
  }

  /** Toutes les valeurs non nulles de la colonne sont des nombres finis. */
  private isNumericColumn(rows: Array<Record<string, unknown>>, col: string): boolean {
    let seen = false;
    for (const r of rows) {
      const v = r[col];
      if (v === null || v === undefined) {
        continue;
      }
      if (typeof v !== 'number' || !Number.isFinite(v)) {
        return false;
      }
      seen = true;
    }
    return seen;
  }

  /** Toutes les valeurs non nulles de la colonne sont des chaînes non vides. */
  private isTextColumn(rows: Array<Record<string, unknown>>, col: string): boolean {
    let seen = false;
    for (const r of rows) {
      const v = r[col];
      if (v === null || v === undefined || v === '') {
        continue;
      }
      if (typeof v !== 'string') {
        return false;
      }
      seen = true;
    }
    return seen;
  }
}
