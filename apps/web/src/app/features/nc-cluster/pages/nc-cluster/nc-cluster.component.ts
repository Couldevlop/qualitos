import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { NcService } from '../../../nc/nc.service';
import { SuppliersService } from '../../../suppliers/suppliers.service';
import { NcClusterService } from '../../nc-cluster.service';
import { NcCluster, NcClusterResponse } from '../../nc-cluster.types';

/** Deux registres, deux fenêtres : les NC fournisseurs (externes) et les NC internes. */
export type NcScope = 'external' | 'internal';

/** NC lues du registre pour un regroupement : les plus récentes, une page serveur. */
export const REGISTER_SIZE = 100;

interface ScopeState { text: string; texts: string[]; result: NcClusterResponse | null; error: string | null; }

/**
 * Clustering de non-conformités (§4.3, §12.1) : l'utilisateur colle une liste de NC (une par
 * ligne) ; l'IA les regroupe par similarité de densité (TF-IDF + DBSCAN) pour révéler les
 * patterns récurrents, avec termes représentatifs par cluster et le bruit (NC isolées).
 */
@Component({
  selector: 'qos-nc-cluster',
  templateUrl: './nc-cluster.component.html',
  styleUrls: ['./nc-cluster.component.scss'],
  standalone: false
})
export class NcClusterComponent {

  readonly form = this.fb.group({
    textsText: ['', [Validators.required]],
    threshold: [0.35, [Validators.min(0.01), Validators.max(0.99)]],
    minSamples: [2, [Validators.min(2), Validators.max(100)]]
  });

  /** Jeu de démonstration : deux familles récurrentes + une NC isolée (bruit). */
  readonly example = [
    "Fuite d'huile sur la presse hydraulique ligne 2",
    "Fuite d'huile détectée presse hydraulique ligne 3",
    "Fuite d'huile presse hydraulique atelier",
    'Étiquette produit manquante sur carton expédition',
    'Étiquette produit manquante sur palette expédition',
    'Capteur de température défaillant chambre froide'
  ].join('\n');

  scope: NcScope = 'external';
  loading = false;
  loadingRegister = false;
  result: NcClusterResponse | null = null;
  error: string | null = null;
  private texts: string[] = [];
  /** Ce que chaque fenêtre garde quand on passe à l'autre. */
  private readonly saved: Partial<Record<NcScope, ScopeState>> = {};

  constructor(
    private readonly fb: FormBuilder,
    private readonly nc: NcClusterService,
    private readonly suppliers: SuppliersService,
    private readonly internal: NcService
  ) {}

  setScope(scope: NcScope): void {
    if (scope === this.scope || this.loading) return;
    this.saved[this.scope] = { text: this.form.value.textsText ?? '', texts: this.texts, result: this.result, error: this.error };
    this.scope = scope;
    const back = this.saved[scope];
    this.form.patchValue({ textsText: back?.text ?? '' });
    this.texts = back?.texts ?? [];
    this.result = back?.result ?? null;
    this.error = back?.error ?? null;
  }

  /** Reprend les NC du registre de la fenêtre : une par ligne, prêtes à regrouper. */
  loadRegister(): void {
    if (this.loadingRegister) return;
    this.loadingRegister = true;
    this.error = null;
    const scope = this.scope;
    this.registerTexts(scope).subscribe({
      next: texts => {
        this.loadingRegister = false;
        if (scope !== this.scope) return;
        if (!texts.length) {
          this.error = $localize`:@@nccluster.register-empty:Le registre ne contient aucune non-conformité.`;
          return;
        }
        this.form.patchValue({ textsText: texts.join('\n') });
      },
      error: () => {
        this.loadingRegister = false;
        if (scope === this.scope) {
          this.error = $localize`:@@nccluster.register-error:Le registre ne se lit pas pour le moment.`;
        }
      }
    });
  }

  private registerTexts(scope: NcScope): Observable<string[]> {
    const line = (...parts: (string | null | undefined)[]) =>
      parts.map(p => (p ?? '').replace(/\s+/g, ' ').trim()).filter(p => p.length > 0).join(' — ');
    return scope === 'external'
      ? this.suppliers.listAllNcs({}, 0, REGISTER_SIZE).pipe(
          map(page => page.content.map(n => line(n.description, n.partNumber)).filter(t => t.length > 0)))
      : this.internal.listNcs(0, REGISTER_SIZE).pipe(
          map(page => page.content.map(n => line(n.title, n.description)).filter(t => t.length > 0)));
  }

  loadExample(): void {
    this.form.patchValue({ textsText: this.example, threshold: 0.35, minSamples: 2 });
  }

  /** Une ligne non vide = une NC. */
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
    if (texts.length < 2) {
      this.error = $localize`:@@nccluster.err-min-texts:Saisissez au moins 2 non-conformités (une par ligne).`;
      return;
    }
    const threshold = this.form.value.threshold ?? 0.35;
    const minSamples = this.form.value.minSamples ?? 2;

    this.texts = texts;
    this.loading = true;
    this.error = null;
    this.result = null;

    this.nc.cluster({ texts, threshold, minSamples }).subscribe({
      next: res => { this.result = res; this.loading = false; },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = this.messageFor(err);
      }
    });
  }

  /** Textes membres d'un cluster (dans l'ordre des indices). */
  membersOf(c: NcCluster): string[] {
    return c.indices.map(i => this.texts[i]).filter(t => t != null);
  }

  /** Textes du bruit (NC isolées). */
  noiseTexts(res: NcClusterResponse): string[] {
    return res.noiseIndices.map(i => this.texts[i]).filter(t => t != null);
  }

  clusteredPct(ratio: number): string {
    return `${Math.round(ratio * 100)} %`;
  }

  private messageFor(err: HttpErrorResponse): string {
    switch (err.status) {
      case 0: return $localize`:@@nccluster.err-backend:Backend injoignable (engine sur 8082 ?).`;
      case 400: return $localize`:@@nccluster.err-invalid:Requête invalide (2 à 2000 NC attendues).`;
      case 413: return $localize`:@@nccluster.err-too-large:Liste trop volumineuse (garde-fou IA).`;
      case 429: return $localize`:@@nccluster.err-quota:Débit/quota IA dépassé pour ce tenant — réessayez plus tard.`;
      case 502: return $localize`:@@nccluster.err-gateway:Passerelle IA indisponible (ai-service injoignable).`;
      case 503: return $localize`:@@nccluster.err-unavailable:Service IA momentanément indisponible (disjoncteur ouvert).`;
      default:  return $localize`:@@nccluster.err-generic:Échec du clustering (HTTP ${err.status}:status:).`;
    }
  }
}
