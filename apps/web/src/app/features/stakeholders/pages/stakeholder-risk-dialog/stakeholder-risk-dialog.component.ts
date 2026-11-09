import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { catchError, map, of } from 'rxjs';

import { DirectoryUser } from '../../../../core/directory/directory.types';
import { nonBlank } from '../../../apqp/apqp.validators';
import { Choice, PROBABILITIES, SEVERITIES, TYPES } from '../../../risk-register/risk-register.labels';
import { RiskRegisterService } from '../../../risk-register/risk-register.service';
import { ImpactLevel } from '../../stakeholders.types';

/** Ce que l'écran rend : les seuls champs qui ne se déduisent pas de l'exigence. */
export interface StakeholderRiskChoice {
  type: string;
  process: string;
  severity: number;
  probability: number;
  ownerId: string;
  ownerName: string;
}

export interface StakeholderRiskDialogData {
  /** L'exigence, rappelée en sous-titre : c'est elle qui sera la cause du risque. */
  requirement: string;
  /** Le risque énoncé sur l'exigence : il devient l'intitulé de la ligne. */
  risk: string;
  /** L'impact estimé sur l'exigence, s'il a été coté : il préremplit la gravité. */
  riskLevel: ImpactLevel | null;
}

/**
 * Inscrire au registre des risques le risque qu'une exigence fait courir.
 *
 * <p>La fenêtre ne redemande ni l'intitulé, ni la cause, ni l'effet : ils sont
 * déjà écrits sur l'exigence, et les ressaisir produirait deux énoncés du même
 * risque qui divergeraient à la première révision. Elle demande ce que
 * l'exigence ne contient pas et que le registre exige de chacune de ses lignes :
 * la nature du risque, ses deux notes, le processus où il se matérialise et la
 * personne qui en répond.
 *
 * <p>Le processus propose les valeurs déjà employées dans le registre, pour que
 * « Production » ne devienne pas « production » trois lignes plus bas. La liste
 * est un confort : si le registre ne répond pas, le champ reste libre.
 *
 * <p>La gravité est préremplie par l'impact estimé de l'exigence quand il a été
 * coté : l'échelle du registre va de 1 à 5, celle des parties intéressées de
 * faible à fort, donc on prend le cran correspondant plutôt que d'imposer une
 * note médiane. Elle reste modifiable : c'est le registre qui cote.
 */
@Component({
  selector: 'qos-stakeholder-risk-dialog',
  templateUrl: './stakeholder-risk-dialog.component.html',
  styleUrls: ['./stakeholder-risk-dialog.component.scss'],
  standalone: false
})
export class StakeholderRiskDialogComponent {

  /**
   * Les natures et les deux échelles viennent du registre lui-même, pas d'une
   * copie : si le registre ajoute une nature ou renomme un cran, cette fenêtre
   * suit. Deux listes qui divergent, et la même ligne se lirait autrement selon
   * l'écran qui l'a créée.
   */
  readonly types: ReadonlyArray<Choice<string>> = TYPES;
  readonly severities: ReadonlyArray<Choice<number>> = SEVERITIES;
  readonly probabilities: ReadonlyArray<Choice<number>> = PROBABILITIES;

  /** Les processus déjà employés dans le registre des risques. */
  processes: string[] = [];

  readonly form = this.fb.nonNullable.group({
    type: ['QUALITY', [Validators.required]],
    process: ['', [Validators.required, nonBlank, Validators.maxLength(120)]],
    severity: [3, [Validators.required]],
    probability: [3, [Validators.required]],
    // Le registre affiche un propriétaire sur chaque ligne et refuse une ligne
    // sans lui : une ligne dont personne ne répond ne se revoit jamais.
    ownerId: ['', [Validators.required]]
  });

  /** Le nom d'annuaire du propriétaire choisi, recopié sur la ligne. */
  ownerName = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly register: RiskRegisterService,
    private readonly dialogRef: MatDialogRef<StakeholderRiskDialogComponent, StakeholderRiskChoice>,
    @Inject(MAT_DIALOG_DATA) public readonly data: StakeholderRiskDialogData
  ) {
    const depuisImpact = StakeholderRiskDialogComponent.gravite(data.riskLevel);
    if (depuisImpact) {
      this.form.controls.severity.setValue(depuisImpact);
    }
    this.register.suggestions().pipe(
      map(s => s.processes ?? []),
      catchError(() => of<string[]>([]))
    ).subscribe(processes => this.processes = processes);
  }

  /**
   * L'impact estimé de l'exigence, dans l'échelle de cotation du registre.
   *
   * <p>Trois crans vers cinq : faible → 2, moyen → 3, fort → 4. On n'utilise ni
   * 1 ni 5, parce qu'une estimation à trois crans ne dit ni « négligeable » ni
   * « maximal » : prétendre le contraire serait inventer de la précision.
   */
  private static gravite(niveau: ImpactLevel | null): number | null {
    switch (niveau) {
      case 'LOW': return 2;
      case 'MEDIUM': return 3;
      case 'HIGH': return 4;
      default: return null;
    }
  }

  /** L'exigence rappelée : c'est elle qui devient la cause de la ligne. */
  get subtitle(): string {
    return $localize`:@@stakeholders.risk.subtitle:Exigence : ${this.data.requirement}:exigence:`;
  }

  /** La cotation brute telle que le registre la calculera : gravité × probabilité. */
  get score(): number {
    const v = this.form.getRawValue();
    return v.severity * v.probability;
  }

  ownerPicked(user: DirectoryUser | null): void {
    this.ownerName = user?.displayName ?? '';
  }

  submit(): void {
    if (this.form.invalid || !this.ownerName) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.dialogRef.close({
      type: v.type,
      process: v.process.trim(),
      severity: v.severity,
      probability: v.probability,
      ownerId: v.ownerId,
      ownerName: this.ownerName
    });
  }

  cancel(): void { this.dialogRef.close(); }
}
