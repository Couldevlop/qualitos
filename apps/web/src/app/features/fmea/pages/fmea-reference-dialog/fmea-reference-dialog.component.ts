import { Component } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';

import {
  FMEA_DETECTION_SCALE,
  FMEA_EXAMPLE_ROWS,
  FMEA_EXAMPLE_TITLE,
  FMEA_OCCURRENCE_SCALE,
  FMEA_SEVERITY_SCALE
} from '../../fmea.reference';

/**
 * Référentiel de cotation FMEA, consultable sans quitter l'analyse en cours.
 *
 * <p>Coter en Sévérité, Occurrence et Détection ne veut rien dire sans l'échelle
 * qui donne le sens des chiffres : un « 8 » de l'un n'est pas le « 8 » de
 * l'autre, et deux RPN cotés sur des barèmes différents ne se comparent pas.
 * L'écran de saisie posait la question sans jamais montrer la règle.
 *
 * <p>Purement documentaire : il ne lit ni n'écrit aucune donnée de tenant.
 */
@Component({
  selector: 'qos-fmea-reference-dialog',
  templateUrl: './fmea-reference-dialog.component.html',
  styleUrls: ['./fmea-reference-dialog.component.scss'],
  standalone: false
})
export class FmeaReferenceDialogComponent {

  readonly severity = FMEA_SEVERITY_SCALE;
  readonly occurrence = FMEA_OCCURRENCE_SCALE;
  readonly detection = FMEA_DETECTION_SCALE;
  readonly exampleTitle = FMEA_EXAMPLE_TITLE;
  readonly exampleRows = FMEA_EXAMPLE_ROWS;

  constructor(private readonly dialogRef: MatDialogRef<FmeaReferenceDialogComponent>) {}

  close(): void {
    this.dialogRef.close();
  }

  /**
   * Classe de la pastille de score : un barème se lit d'abord par la couleur,
   * du plus grave au plus anodin. Les mêmes seuils que la criticité des items.
   */
  scoreClass(score: number): string {
    if (score >= 9) return 'score score--critical';
    if (score >= 7) return 'score score--high';
    if (score >= 4) return 'score score--medium';
    return 'score score--low';
  }

  /** Un RPN élevé se signale : c'est le seul chiffre qui hiérarchise l'exemple. */
  rpnClass(rpn: number): string {
    return rpn >= 200 ? 'rpn rpn--critical' : rpn >= 100 ? 'rpn rpn--high' : 'rpn';
  }
}
