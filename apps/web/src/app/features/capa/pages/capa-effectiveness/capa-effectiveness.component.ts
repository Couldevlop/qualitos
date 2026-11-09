import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';

import { CapaService } from '../../capa.service';
import { CapaEffectivenessRow, CapaEffectivenessSummary } from '../../capa.types';

/**
 * L'efficacité mesurée des CAPA closes.
 *
 * <p>Le dossier CAPA porte déjà une case « efficacité vérifiée » : c'est une
 * opinion, datée du jour de la clôture, portée par la personne qui a mené
 * l'action. Cet écran fait répondre le terrain, et il répond plus tard.
 *
 * <p>Il montre donc les deux côte à côte. L'écart entre ce qui a été déclaré et
 * ce qui s'est produit est l'information que personne n'a aujourd'hui — et c'est
 * le premier chiffre qu'un auditeur cherchera.
 */
@Component({
  selector: 'qos-capa-effectiveness',
  templateUrl: './capa-effectiveness.component.html',
  styleUrls: ['./capa-effectiveness.component.scss'],
  standalone: false
})
export class CapaEffectivenessComponent implements OnInit {

  readonly columns = ['title', 'closedAt', 'recurrence', 'rate', 'declared'];
  readonly windows = [3, 6, 12];

  summary?: CapaEffectivenessSummary;
  months = 6;
  loading = false;

  /**
   * L'onglet affiché. 0 = les évaluations, 1 = la mesure par récidive.
   *
   * <p>Les évaluations d'abord parce que c'est l'onglet où l'on AGIT : il porte
   * la liste de ce qui attend un jugement. La mesure, elle, se consulte.
   */
  tab = 0;

  /**
   * Le dossier nommé par `?capa=<id>` — le renvoi de la fiche CAPA l'y met.
   * On arrive alors directement sur l'onglet des évaluations, dossier ouvert et
   * fenêtre de jugement déployée : c'est ce que le lecteur venait faire.
   */
  focusCapaId?: string;

  constructor(
    private readonly service: CapaService,
    private readonly snack: MatSnackBar,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const demande = this.route.snapshot.queryParamMap.get('capa');
    if (demande) {
      this.focusCapaId = demande;
      this.tab = 0;
    }
    this.reload();
  }

  reload(): void {
    // `loading` est un champ simple, posé AVANT l'abonnement : avec un flux
    // synchrone, une micro-tâche le rallumerait sur un tableau déjà chargé.
    this.loading = true;
    this.service.effectiveness(this.months).subscribe({
      next: summary => {
        this.summary = summary;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snack.open(
          $localize`:@@capa.effectiveness.failed:Impossible de charger l'efficacité des CAPA.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  changeWindow(months: number): void {
    if (months === this.months) return;
    this.months = months;
    this.reload();
  }

  /**
   * Mois observés, arrondis à l'entier. « 2 mois sur 6 » se lit ; « 1,97 » non,
   * et la précision serait fausse de toute façon — la fenêtre compte des jours.
   */
  observedMonths(row: CapaEffectivenessRow): number {
    return Math.round(row.daysObserved / 30);
  }

  windowMonths(row: CapaEffectivenessRow): number {
    return Math.round(row.daysInWindow / 30);
  }

  /**
   * Vrai quand le responsable avait déclaré l'action efficace et que le terrain
   * le dément. C'est la ligne qu'on vient chercher sur cet écran.
   */
  contradicted(row: CapaEffectivenessRow): boolean {
    return row.declaredEffective === true && row.status === 'MEASURED' && row.ratePercent === 0;
  }

  rateClass(row: CapaEffectivenessRow): string {
    if (row.ratePercent === undefined) return 'rate-unknown';
    if (row.ratePercent >= 70) return 'rate-good';
    if (row.ratePercent >= 30) return 'rate-fair';
    return 'rate-poor';
  }
}
