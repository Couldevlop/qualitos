import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';

import { NlqService } from '../../nlq.service';
import { NlqAskResponse } from '../../nlq.types';

/**
 * Page Natural Language Query (§7.3) : champ de question, exemples, puis restitution
 * du récit, des résultats (tableau + mini-barres), du SQL généré (transparence) et
 * des métadonnées de confiance/sécurité (filtre tenant appliqué).
 */
@Component({
  selector: 'qos-nlq-ask',
  templateUrl: './nlq-ask.component.html',
  styleUrls: ['./nlq-ask.component.scss'],
  standalone: false
})
export class NlqAskComponent {

  readonly form = this.fb.group({
    question: ['', [Validators.required, Validators.maxLength(500)]]
  });

  readonly examples: string[] = [
    $localize`:@@nlq.ask.example-1:Combien de CAPA par statut ?`,
    $localize`:@@nlq.ask.example-2:Nombre de diagrammes Ishikawa par statut`,
    $localize`:@@nlq.ask.example-3:Combien de CAPA par criticité ?`,
    $localize`:@@nlq.ask.example-4:Nombre de fournisseurs par statut`
  ];

  loading = false;
  result: NlqAskResponse | null = null;
  error: string | null = null;
  showSql = false;

  constructor(private readonly fb: FormBuilder, private readonly nlq: NlqService) {}

  ask(preset?: string): void {
    if (preset) {
      this.form.patchValue({ question: preset });
    }
    const question = (this.form.value.question ?? '').trim();
    if (!question || this.loading) {
      return;
    }
    this.loading = true;
    this.error = null;
    this.result = null;
    this.showSql = false;
    this.nlq.ask(question).subscribe({
      next: res => {
        this.result = res;
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = this.messageFor(err);
      }
    });
  }

  /** Colonnes dérivées de la première ligne (les colonnes varient selon la requête). */
  columns(): string[] {
    const rows = this.result?.rows ?? [];
    return rows.length ? Object.keys(rows[0]) : [];
  }

  /** Colonnes dont toutes les valeurs sont numériques — support des mini-barres. */
  numericColumns(): string[] {
    const rows = this.result?.rows ?? [];
    if (!rows.length) {
      return [];
    }
    return this.columns().filter(c => rows.every(r => typeof r[c] === 'number'));
  }

  isNumericColumn(col: string): boolean {
    return this.numericColumns().includes(col);
  }

  /** Largeur (%) de la barre pour une valeur, relative au max de la colonne. */
  barPercent(value: unknown, col: string): number {
    const rows = this.result?.rows ?? [];
    const max = Math.max(...rows.map(r => Math.abs(Number(r[col]) || 0)), 0);
    if (max <= 0 || typeof value !== 'number') {
      return 0;
    }
    return Math.round((Math.abs(value) / max) * 100);
  }

  cell(value: unknown): string {
    if (value === null || value === undefined) {
      return '—';
    }
    return String(value);
  }

  confidencePct(): number {
    return Math.round((this.result?.confidence ?? 0) * 100);
  }

  confidenceClass(): string {
    const c = this.result?.confidence ?? 0;
    return c >= 0.8 ? 'ok' : c >= 0.5 ? 'warn' : 'bad';
  }

  tenantFilterLabel(): string {
    return this.result?.tenantFilterApplied
      ? $localize`:@@nlq.ask.tenant-filtered:Filtré par tenant`
      : $localize`:@@nlq.ask.tenant-unfiltered:Tenant non filtré`;
  }

  sqlToggleLabel(): string {
    return this.showSql
      ? $localize`:@@nlq.ask.hide-sql:Masquer le SQL`
      : $localize`:@@nlq.ask.show-sql:Voir le SQL généré`;
  }

  private messageFor(err: HttpErrorResponse): string {
    if (err.status === 502 || err.status === 503) {
      return $localize`:@@nlq.ask.err-unavailable:L'assistant IA est momentanément indisponible (modèle en cours de chargement ?). Réessaie dans un instant.`;
    }
    if (err.status === 422 || err.status === 400) {
      return $localize`:@@nlq.ask.err-untranslatable:La question n'a pas pu être traduite en requête sûre. Reformule-la plus simplement.`;
    }
    return $localize`:@@nlq.ask.err-generic:Une erreur est survenue lors du traitement de la question.`;
  }
}
