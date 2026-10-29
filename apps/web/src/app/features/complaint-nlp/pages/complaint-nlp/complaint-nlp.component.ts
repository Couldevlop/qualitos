import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';

import { complaintLine } from '../../../../shared/ai-sources/series';
import { NcService } from '../../../nc/nc.service';

import { ComplaintNlpService } from '../../complaint-nlp.service';
import { ComplaintAnalyzeResponse, ComplaintInsight } from '../../complaint-nlp.types';

/**
 * Analyse NLP des réclamations clients (§4.9, §12.1) : l'utilisateur colle une liste de
 * réclamations (une par ligne) ; l'IA évalue le sentiment, la catégorie et la criticité de
 * chacune (sentiment lexical + classification par termes-graines). Met en avant les
 * réclamations critiques (sécurité, juridique, sentiment très négatif).
 */
@Component({
  selector: 'qos-complaint-nlp',
  templateUrl: './complaint-nlp.component.html',
  styleUrls: ['./complaint-nlp.component.scss'],
  standalone: false
})
export class ComplaintNlpComponent {

  readonly form = this.fb.group({
    textsText: ['', [Validators.required]]
  });

  /** Jeu de démonstration : critique, négatif, positif, neutre. */
  readonly example = [
    'Produit dangereux, risque de blessure, je demande un rappel urgent',
    'Livraison en retard de 5 jours, colis endommagé, très déçu',
    'Service après-vente excellent, conseiller parfait, merci',
    'Commande numéro 88421 reçue le 3 mars'
  ].join('\n');

  loading = false;
  result: ComplaintAnalyzeResponse | null = null;
  error: string | null = null;
  private texts: string[] = [];

  /** Nombre de réclamations reprises au plus : la page d'analyse reste lisible. */
  static readonly SOURCE_SIZE = 50;

  sourceLoading = false;
  sourceInfo: string | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly nlp: ComplaintNlpService,
    private readonly nc: NcService
  ) {}

  /**
   * Reprend les réclamations clients enregistrées (NC d'origine externe), les
   * plus récentes d'abord : titre et description, une réclamation par ligne.
   */
  loadComplaints(): void {
    this.sourceLoading = true;
    this.sourceInfo = null;
    this.error = null;
    this.nc.listNcs(0, ComplaintNlpComponent.SOURCE_SIZE, { origin: 'EXTERNAL' })
      .pipe(finalize(() => (this.sourceLoading = false)))
      .subscribe({
        next: page => {
          const lines = page.content.map(n => complaintLine(n.title, n.description));
          this.form.patchValue({ textsText: lines.join('\n') });
          const count = lines.length;
          const total = page.totalElements;
          this.sourceInfo = count === 0
            ? $localize`:@@complaintnlp.source-none:Aucune réclamation client enregistrée pour l'instant.`
            : $localize`:@@complaintnlp.source-loaded:${count}:count: réclamation(s) reprise(s) sur ${total}:total: enregistrée(s).`;
        },
        error: () => {
          this.error = $localize`:@@complaintnlp.source-error:Impossible de charger les réclamations enregistrées.`;
        }
      });
  }

  loadExample(): void {
    this.form.patchValue({ textsText: this.example });
  }

  private parseTexts(): string[] {
    return (this.form.value.textsText ?? '')
      .split(/\r?\n/)
      .map(t => t.trim())
      .filter(t => t.length > 0);
  }

  run(): void {
    if (this.loading) {
      return;
    }
    const texts = this.parseTexts();
    if (texts.length === 0) {
      this.error = $localize`:@@complaintnlp.err-min:Saisissez au moins une réclamation (une par ligne).`;
      return;
    }
    this.texts = texts;
    this.loading = true;
    this.error = null;
    this.result = null;

    this.nlp.analyze({ texts }).subscribe({
      next: res => { this.result = res; this.loading = false; },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = this.messageFor(err);
      }
    });
  }

  textOf(i: ComplaintInsight): string {
    return this.texts[i.index] ?? '';
  }

  /** Insights triés : critiques d'abord, puis sentiment croissant (plus négatif en tête). */
  sorted(res: ComplaintAnalyzeResponse): ComplaintInsight[] {
    return [...res.insights].sort((a, b) =>
      Number(b.critical) - Number(a.critical) || a.sentiment - b.sentiment);
  }

  sentimentClass(label: string): string {
    return label === 'negative' ? 'sent-neg' : label === 'positive' ? 'sent-pos' : 'sent-neu';
  }

  sentimentLabel(label: string): string {
    if (label === 'negative') {
      return $localize`:@@complaintnlp.sent-negative:Négatif`;
    }
    if (label === 'positive') {
      return $localize`:@@complaintnlp.sent-positive:Positif`;
    }
    return $localize`:@@complaintnlp.sent-neutral:Neutre`;
  }

  private messageFor(err: HttpErrorResponse): string {
    switch (err.status) {
      case 0: return $localize`:@@complaintnlp.err-backend:Backend injoignable (engine sur 8082 ?).`;
      case 400: return $localize`:@@complaintnlp.err-invalid:Requête invalide (1 à 2000 réclamations).`;
      case 413: return $localize`:@@complaintnlp.err-too-large:Lot trop volumineux (garde-fou IA).`;
      case 429: return $localize`:@@complaintnlp.err-quota:Débit/quota IA dépassé pour ce tenant — réessayez plus tard.`;
      case 502: return $localize`:@@complaintnlp.err-gateway:Passerelle IA indisponible (ai-service injoignable).`;
      case 503: return $localize`:@@complaintnlp.err-unavailable:Service IA momentanément indisponible (disjoncteur ouvert).`;
      default:  return $localize`:@@complaintnlp.err-generic:Échec de l'analyse (HTTP ${err.status}:status:).`;
    }
  }
}
