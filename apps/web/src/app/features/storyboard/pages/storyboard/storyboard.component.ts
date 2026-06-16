import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';

import { StoryboardService } from '../../storyboard.service';
import { IndicatorPoint, StoryboardResponse } from '../../storyboard.types';

/**
 * Storyboard IA (§7.4) : l'utilisateur saisit/colle des indicateurs (libellé, valeur,
 * tendance/cible/unité optionnelles) et une période ; l'IA rédige un COURT récit narratif
 * factuel pour une revue de direction. Le récit, le rappel des chiffres source (explicabilité
 * §12.3) et un disclaimer « récit généré par IA, à valider » sont affichés.
 *
 * L'IA suggère, l'humain décide : le texte est explicitement présenté comme à valider.
 */
@Component({
  selector: 'qos-storyboard',
  templateUrl: './storyboard.component.html',
  styleUrls: ['./storyboard.component.scss'],
  standalone: false
})
export class StoryboardComponent {

  readonly form = this.fb.group({
    period: ['', [Validators.required]],
    context: [''],
    points: this.fb.array([this.newPoint()])
  });

  loading = false;
  result: StoryboardResponse | null = null;
  error: string | null = null;

  constructor(private readonly fb: FormBuilder, private readonly storyboard: StoryboardService) {}

  get points(): FormArray {
    return this.form.get('points') as FormArray;
  }

  pointGroup(i: number): FormGroup {
    return this.points.at(i) as FormGroup;
  }

  private newPoint(seed?: Partial<IndicatorPoint>): FormGroup {
    return this.fb.group({
      label: [seed?.label ?? '', [Validators.required]],
      value: [seed?.value ?? '', [Validators.required]],
      unit: [seed?.unit ?? ''],
      trend: [seed?.trend ?? ''],
      target: [seed?.target ?? '']
    });
  }

  addRow(): void {
    this.points.push(this.newPoint());
  }

  removeRow(i: number): void {
    if (this.points.length > 1) {
      this.points.removeAt(i);
    }
  }

  loadExample(): void {
    this.points.clear();
    [
      { label: 'Taux de NC', value: '1,8', unit: '%', trend: '-12 %', target: '< 2' },
      { label: 'Délai moyen de clôture CAPA', value: '26', unit: 'j', trend: 'stable', target: '< 30' },
      { label: 'First Pass Yield', value: '97,4', unit: '%', trend: '+1,2 pt', target: '> 98' },
      { label: 'Audits réalisés vs planifiés', value: '8/10', trend: '2 retards', target: '10/10' }
    ].forEach(p => this.points.push(this.newPoint(p)));
    this.form.patchValue({ period: 'Mai 2026', context: 'Site de Lyon, atelier mécanique' });
  }

  /** Indicateurs saisis, nettoyés : on ne transmet que ceux ayant un libellé ET une valeur. */
  private collectPoints(): IndicatorPoint[] {
    return this.points.controls
      .map(c => c.value as IndicatorPoint)
      .map(p => ({
        label: (p.label ?? '').trim(),
        value: (p.value ?? '').trim(),
        unit: (p.unit ?? '').trim() || undefined,
        trend: (p.trend ?? '').trim() || undefined,
        target: (p.target ?? '').trim() || undefined
      }))
      .filter(p => p.label.length > 0 && p.value.length > 0);
  }

  run(): void {
    if (this.loading) {
      return;
    }
    const period = (this.form.value.period ?? '').trim();
    if (!period) {
      this.error = $localize`:@@storyboard.err-period:Indiquez une période.`;
      return;
    }
    const points = this.collectPoints();
    if (points.length === 0) {
      this.error = $localize`:@@storyboard.err-points:Saisissez au moins un indicateur (libellé + valeur).`;
      return;
    }
    const context = (this.form.value.context ?? '').trim() || undefined;

    this.loading = true;
    this.error = null;
    this.result = null;

    this.storyboard.generate({ period, context, points }).subscribe({
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

  private messageFor(err: HttpErrorResponse): string {
    switch (err.status) {
      case 0: return $localize`:@@storyboard.err-backend:Backend injoignable (engine sur 8082 ?).`;
      case 400: return $localize`:@@storyboard.err-invalid:Requête invalide (période + au moins un indicateur libellé/valeur).`;
      case 413: return $localize`:@@storyboard.err-too-large:Trop d'indicateurs (garde-fou IA).`;
      case 429: return $localize`:@@storyboard.err-quota:Débit/quota IA dépassé pour ce tenant — réessayez plus tard.`;
      case 502: return $localize`:@@storyboard.err-gateway:Passerelle IA indisponible (ai-service injoignable).`;
      case 503: return $localize`:@@storyboard.err-unavailable:Service IA momentanément indisponible (disjoncteur ouvert).`;
      default:  return $localize`:@@storyboard.err-generic:Échec de la génération (HTTP ${err.status}:status:).`;
    }
  }
}
