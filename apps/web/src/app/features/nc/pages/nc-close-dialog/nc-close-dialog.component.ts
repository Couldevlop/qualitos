import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

/** Ce que la fenêtre doit savoir : de quel écart il s'agit, et ce qu'il porte déjà. */
export interface NcCloseDialogData {
  reference: string;
  /** La réponse déjà donnée, si la clôture a déjà été tentée. */
  pfmeaImpacted?: boolean;
  controlPlanImpacted?: boolean;
}

/** Ce qu'elle rend : les deux décisions, ou rien si on renonce. */
export interface NcCloseDialogResult {
  pfmeaImpacted: boolean;
  controlPlanImpacted: boolean;
}

/**
 * CLORE UN ÉCART, C'EST AUSSI DIRE CE QU'IL A ENSEIGNÉ.
 *
 * <p>Un écart traité apprend quelque chose sur l'analyse des risques du
 * produit : un mode de défaillance que le PFMEA n'avait pas prévu, un contrôle
 * que le plan de surveillance ne portait pas. L'IATF 16949 en fait une
 * exigence — toute non-conformité se répercute sur la PFMEA et le plan de
 * surveillance — et c'est l'écart qu'un auditeur cherche en premier.
 *
 * <p>Jusqu'ici la clôture ne demandait rien : l'écart se refermait sans que
 * personne n'ait eu à se prononcer, et le PFMEA restait muet sur un défaut
 * survenu pour de bon.
 *
 * <p>LES DEUX RÉPONSES SONT OBLIGATOIRES, et « non » en est une : datée,
 * imputable, c'est elle que l'auditeur vient lire quand le PFMEA n'a pas bougé.
 * Une case laissée vide, elle, ne prouverait rien. Un « oui » rend la révision
 * du document exigible avant la clôture définitive — et la réponse se corrige,
 * si on s'est trompé.
 */
@Component({
  selector: 'qos-nc-close-dialog',
  templateUrl: './nc-close-dialog.component.html',
  styleUrls: ['./nc-close-dialog.component.scss'],
  standalone: false
})
export class NcCloseDialogComponent {

  /**
   * Aucune valeur par défaut : ni « oui » ni « non » n'est présélectionné.
   *
   * <p>Un « non » proposé d'avance serait coché par le seul effet du clic
   * suivant, et la fiche porterait une décision que personne n'a prise. Le
   * bouton reste donc éteint jusqu'à ce que les deux questions soient
   * tranchées — le formulaire dit ce qui manque, il ne le devine pas.
   */
  readonly form = this.fb.nonNullable.group({
    pfmeaImpacted: this.fb.control<boolean | null>(
      this.data.pfmeaImpacted ?? null, Validators.required),
    controlPlanImpacted: this.fb.control<boolean | null>(
      this.data.controlPlanImpacted ?? null, Validators.required)
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly ref: MatDialogRef<NcCloseDialogComponent, NcCloseDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: NcCloseDialogData
  ) {}

  valider(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.ref.close({
      pfmeaImpacted: v.pfmeaImpacted === true,
      controlPlanImpacted: v.controlPlanImpacted === true
    });
  }

  annuler(): void {
    this.ref.close();
  }

  /** Vrai dès qu'un « oui » est coché : on annonce alors ce que la clôture attendra. */
  exigeUneRevision(): boolean {
    const v = this.form.getRawValue();
    return v.pfmeaImpacted === true || v.controlPlanImpacted === true;
  }
}
