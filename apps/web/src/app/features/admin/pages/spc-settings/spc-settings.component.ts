import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { SpcCapabilityService } from '../../../spc/capability/spc-capability.service';
import { SpcSettings } from '../../../spc/capability/spc-capability.types';

/** La borne haute d'un seuil, comme le serveur (`CapabilityThresholds.MAX`). */
const MAX_THRESHOLD = 10;

/**
 * « Paramètres SPC » (ADR 0096), réservé à l'administrateur du client : les
 * deux seuils de Cpk qui classent une étude. Au-dessus du seuil de maîtrise,
 * le procédé est maîtrisé (vert) ; entre les deux, à surveiller (jaune) ; en
 * deçà du seuil de surveillance, non maîtrisé (rouge).
 *
 * <p>Les seuils s'appliquent aux études créées après l'enregistrement : une
 * étude garde ceux qu'elle avait à sa création, pour que son verdict ne change
 * pas après coup.
 */
@Component({
  selector: 'qos-spc-settings',
  templateUrl: './spc-settings.component.html',
  styleUrls: ['./spc-settings.component.scss'],
  standalone: false
})
export class SpcSettingsComponent implements OnInit {

  capableThreshold = 1.67;
  watchThreshold = 1.33;
  defaults = true;

  loading = false;
  failed = false;
  saving = false;

  private saved: SpcSettings | null = null;

  constructor(
    private readonly service: SpcCapabilityService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    this.service.settings().subscribe({
      next: s => {
        this.loading = false;
        this.reprendre(s);
      },
      error: err => {
        this.loading = false;
        this.failed = true;
        this.echouer(err, $localize`:@@spc.settings.load-failed:Les paramètres SPC n'ont pas pu être chargés.`);
      }
    });
  }

  /** Ce qui empêche d'enregistrer, dit en clair ; vide si tout va bien. */
  get problems(): string[] {
    const p: string[] = [];
    const valide = (n: number) => typeof n === 'number' && Number.isFinite(n) && n > 0 && n <= MAX_THRESHOLD
      && Math.abs(Math.round(n * 100) - n * 100) < 1e-9;
    if (!valide(this.capableThreshold) || !valide(this.watchThreshold)) {
      p.push($localize`:@@spc.settings.problem.range:Chaque seuil est un nombre de 0,01 à 10, avec deux décimales au plus.`);
    } else if (this.watchThreshold >= this.capableThreshold) {
      p.push($localize`:@@spc.settings.problem.order:Le seuil de surveillance doit rester sous le seuil de maîtrise.`);
    }
    return p;
  }

  get dirty(): boolean {
    return !!this.saved && (this.saved.capableThreshold !== this.capableThreshold
      || this.saved.watchThreshold !== this.watchThreshold);
  }

  /** Les trois zones, comme sur le registre. */
  get summary(): string {
    const c = Number.isFinite(this.capableThreshold) ? this.capableThreshold.toFixed(2) : '–';
    const w = Number.isFinite(this.watchThreshold) ? this.watchThreshold.toFixed(2) : '–';
    return $localize`:@@spc.settings.summary:Non maîtrisé < ${w}:watch: ≤ À surveiller < ${c}:capable: ≤ Maîtrisé`;
  }

  discard(): void {
    if (this.saved) this.reprendre(this.saved);
  }

  save(): void {
    if (!this.dirty || this.problems.length || this.saving) return;
    this.saving = true;
    this.service.updateSettings(this.capableThreshold, this.watchThreshold).subscribe({
      next: s => {
        this.saving = false;
        this.reprendre(s);
        this.snack.open($localize`:@@spc.settings.saved:Paramètres SPC enregistrés. Ils s'appliquent aux prochaines études.`,
          undefined, { duration: 4000 });
      },
      error: err => {
        this.saving = false;
        this.echouer(err, $localize`:@@spc.settings.save-failed:Les paramètres SPC n'ont pas pu être enregistrés.`);
      }
    });
  }

  private reprendre(s: SpcSettings): void {
    this.saved = s;
    this.capableThreshold = s.capableThreshold;
    this.watchThreshold = s.watchThreshold;
    this.defaults = s.defaults;
  }

  private echouer(err: unknown, fallback: string): void {
    this.snack.open(safeErrorMessage(err, fallback), undefined, { duration: 5000 });
  }
}
