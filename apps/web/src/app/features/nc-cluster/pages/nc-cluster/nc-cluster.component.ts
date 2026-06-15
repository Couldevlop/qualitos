import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';

import { NcClusterService } from '../../nc-cluster.service';
import { NcCluster, NcClusterResponse } from '../../nc-cluster.types';

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

  loading = false;
  result: NcClusterResponse | null = null;
  error: string | null = null;
  private texts: string[] = [];

  constructor(private readonly fb: FormBuilder, private readonly nc: NcClusterService) {}

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
