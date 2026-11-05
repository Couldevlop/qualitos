import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { DirectoryUser } from '../../../../core/directory/directory.types';
import { impactLabel, natureLabel, requirementStatusLabel } from '../../stakeholders.labels';
import {
  IMPACT_LEVELS,
  ImpactLevel,
  MANDATORY_NATURES,
  MandatoryNature,
  REQUIREMENT_STATUSES,
  RequirementRequest,
  RequirementStatus,
  STAKEHOLDER_LIMITS
} from '../../stakeholders.types';

export interface StakeholderRequirementDialogData {
  /** Rappelé en sous-titre : on ajoute une exigence À quelqu'un. */
  stakeholderName: string;
}

/**
 * La saisie d'une exigence. Elle ne parle pas au serveur : elle **rend** sa
 * saisie, et la fiche l'envoie — c'est la fiche qui sait à quelle partie
 * prenante l'exigence se rattache, et qui tient la liste à l'écran.
 *
 * <p>Un seul champ est obligatoire, l'exigence elle-même. Tout le reste
 * (origine, nature, risque, maîtrise, indicateur, responsable, preuve) se
 * complète au fil de l'analyse : exiger les douze champs d'emblée reviendrait à
 * refuser l'exigence qu'on vient de lire dans un contrat parce qu'on n'a pas
 * encore décidé qui la suivra.
 */
@Component({
  selector: 'qos-stakeholder-requirement-dialog',
  templateUrl: './stakeholder-requirement-dialog.component.html',
  styleUrls: ['./stakeholder-requirement-dialog.component.scss'],
  standalone: false
})
export class StakeholderRequirementDialogComponent {

  readonly natures = MANDATORY_NATURES;
  readonly impacts = IMPACT_LEVELS;
  readonly statuses = REQUIREMENT_STATUSES;
  readonly limits = STAKEHOLDER_LIMITS;

  readonly natureLabel = natureLabel;
  readonly impactLabel = impactLabel;
  readonly requirementStatusLabel = requirementStatusLabel;

  /**
   * Le nom du responsable, recopié de l'annuaire.
   *
   * <p>Le champ du serveur est un nom, pas un compte : le sélecteur travaille
   * sur l'`accountId`, et c'est le nom affiché qui part — jamais l'identifiant,
   * qui n'apprendrait rien à la lecture de la fiche.
   */
  private responsibleName: string | null = null;

  readonly form = this.fb.nonNullable.group({
    requirement: ['', [Validators.required, Validators.maxLength(STAKEHOLDER_LIMITS.requirement)]],
    need: ['', [Validators.maxLength(STAKEHOLDER_LIMITS.line)]],
    origin: ['', [Validators.maxLength(STAKEHOLDER_LIMITS.line)]],
    nature: [null as MandatoryNature | null],
    risk: ['', [Validators.maxLength(STAKEHOLDER_LIMITS.line)]],
    riskLevel: [null as ImpactLevel | null],
    control: ['', [Validators.maxLength(STAKEHOLDER_LIMITS.line)]],
    indicator: ['', [Validators.maxLength(STAKEHOLDER_LIMITS.line)]],
    responsibleId: [null as string | null],
    evidence: ['', [Validators.maxLength(STAKEHOLDER_LIMITS.line)]],
    status: ['TO_ANALYSE' as RequirementStatus]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<StakeholderRequirementDialogComponent,
                                             RequirementRequest>,
    @Inject(MAT_DIALOG_DATA) public readonly data: StakeholderRequirementDialogData
  ) {}

  get subtitle(): string {
    return $localize`:@@stakeholders.requirement.subtitle:Ce que ${this.data.stakeholderName}:nom: attend, et comment vous vous en assurez.`;
  }

  get blockedReason(): string | undefined {
    if (this.form.controls.requirement.hasError('required')) {
      return $localize`:@@stakeholders.requirement.need-text:Énoncez l'exigence : c'est la seule chose indispensable ici.`;
    }
    if (this.form.controls.requirement.hasError('maxlength')) {
      return $localize`:@@stakeholders.requirement.too-long:L'exigence dépasse la longueur acceptée ; résumez-la et détaillez dans la preuve.`;
    }
    return undefined;
  }

  onResponsible(user: DirectoryUser | null): void {
    this.responsibleName = user ? user.displayName : null;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.dialogRef.close({
      requirement: v.requirement.trim(),
      need: this.clean(v.need),
      origin: this.clean(v.origin),
      nature: v.nature,
      risk: this.clean(v.risk),
      riskLevel: v.riskLevel,
      control: this.clean(v.control),
      indicator: this.clean(v.indicator),
      responsible: this.responsibleName,
      evidence: this.clean(v.evidence),
      status: v.status
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  /** Un champ laissé vide est absent, pas une chaîne vide à enregistrer. */
  private clean(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }
}
