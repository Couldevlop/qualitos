import { Component, Inject } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { requirementLabel, riskBasis, riskLabel, roleLabel } from '../../ai-systems.labels';
import { InUseRequirementKey, inUseRequirements } from '../../ai-systems.rules';
import { AiSystemsService } from '../../ai-systems.service';
import {
  AI_SYSTEM_REFERENCE_PATTERN,
  AiRiskClassification,
  AiSystemPayload,
  AiSystemRole,
  AiSystemView,
  HTTP_URL_PATTERN,
  RISK_SEVERITY,
  UUID_PATTERN
} from '../../ai-systems.types';

export interface AiSystemFormData {
  mode: 'create' | 'edit';
  /** Renseigné en édition uniquement. */
  system?: AiSystemView;
}

/** Ligne de la check-list affichée sous la classification. */
export interface RequirementRow {
  key: InUseRequirementKey;
  label: string;
  satisfied: boolean;
}

/**
 * Déclaration / modification d'un système d'IA.
 *
 * Deux points structurants, imposés par le domaine serveur :
 *  - la référence est immuable (elle identifie le système dans la base UE et les
 *    rapports d'audit) : saisie à la création, verrouillée ensuite ;
 *  - une fiche n'est modifiable qu'à l'état brouillon. La check-list des prérequis
 *    de mise en service est donc affichée ICI, au moment où l'on peut encore agir.
 */
@Component({
  selector: 'qos-ai-system-form-dialog',
  templateUrl: './ai-system-form-dialog.component.html',
  styleUrls: ['./ai-system-form-dialog.component.scss'],
  standalone: false
})
export class AiSystemFormDialogComponent {

  readonly risks: AiRiskClassification[] = RISK_SEVERITY;
  readonly roles: AiSystemRole[] = ['PROVIDER', 'DEPLOYER', 'IMPORTER', 'DISTRIBUTOR'];

  readonly editing: boolean;
  submitting = false;
  error: string | null = null;

  readonly form = this.fb.nonNullable.group({
    reference: ['', [
      Validators.required, Validators.maxLength(64),
      Validators.pattern(AI_SYSTEM_REFERENCE_PATTERN)
    ]],
    name: ['', [Validators.required, Validators.maxLength(250)]],
    intendedPurpose: ['', [Validators.required, Validators.maxLength(4000)]],
    description: ['', [Validators.maxLength(4000)]],
    providerName: ['', [Validators.maxLength(250)]],
    riskClassification: ['LIMITED' as AiRiskClassification, [Validators.required]],
    role: ['DEPLOYER' as AiSystemRole, [Validators.required]],
    generalPurpose: [false],
    conformityAssessmentEvidenceUrl: ['', [
      Validators.maxLength(1024), Validators.pattern(HTTP_URL_PATTERN)
    ]],
    ceMarkingNumber: ['', [Validators.maxLength(250)]],
    humanOversightDescription: ['', [Validators.maxLength(4000)]],
    transparencyMeasures: ['', [Validators.maxLength(4000)]],
    dataGovernanceNotes: ['', [Validators.maxLength(4000)]],
    linkedDpiaId: ['', [Validators.pattern(UUID_PATTERN)]],
    linkedProcessingActivityIds: ['', [uuidList]],
    linkedAutomatedDecisionIds: ['', [uuidList]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: AiSystemsService,
    private readonly auth: AuthService,
    private readonly dialogRef: MatDialogRef<AiSystemFormDialogComponent, AiSystemView | undefined>,
    @Inject(MAT_DIALOG_DATA) readonly data: AiSystemFormData
  ) {
    this.editing = data.mode === 'edit' && !!data.system;
    if (this.editing && data.system) {
      this.patch(data.system);
      // La référence n'est pas modifiable côté serveur : la montrer sans laisser
      // croire qu'on peut la corriger ici.
      this.form.controls.reference.disable();
    }
  }

  // ---- Aide à la décision ----------------------------------------------------

  currentRisk(): AiRiskClassification {
    return this.form.controls.riskClassification.value;
  }

  prohibited(): boolean {
    return this.currentRisk() === 'UNACCEPTABLE';
  }

  /**
   * Prérequis de mise en service pour la classification en cours de saisie.
   * Affichés en continu : après enregistrement, la fiche est figée et un prérequis
   * manquant condamne le système au retrait.
   */
  requirements(): RequirementRow[] {
    const v = this.form.getRawValue();
    return inUseRequirements(v.riskClassification, {
      conformityAssessmentEvidenceUrl: v.conformityAssessmentEvidenceUrl,
      humanOversightDescription: v.humanOversightDescription,
      transparencyMeasures: v.transparencyMeasures
    }).map(r => ({ key: r.key, label: requirementLabel(r.key), satisfied: r.satisfied }));
  }

  trackByKey(_index: number, row: RequirementRow): string { return row.key; }

  riskLabel(risk: AiRiskClassification): string { return riskLabel(risk); }
  riskBasis(risk: AiRiskClassification): string { return riskBasis(risk); }
  roleLabel(role: AiSystemRole): string { return roleLabel(role); }

  // ---- Soumission ------------------------------------------------------------

  submit(): void {
    if (this.submitting) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error = $localize`:@@ai-systems.form.invalid:Corrigez les champs signalés avant d'enregistrer.`;
      return;
    }
    this.error = null;
    const payload = this.payload();

    if (this.editing && this.data.system) {
      this.send(this.svc.edit(this.data.system.id, payload));
      return;
    }

    const createdByUserId = this.auth.snapshot()?.userId;
    if (!createdByUserId) {
      this.error = $localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`;
      return;
    }
    this.send(this.svc.draft({
      ...payload,
      reference: this.form.getRawValue().reference.trim(),
      createdByUserId
    }));
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  private send(call$: Observable<AiSystemView>): void {
    this.submitting = true;
    call$.pipe(finalize(() => { this.submitting = false; })).subscribe({
      next: saved => this.dialogRef.close(saved),
      error: err => {
        this.error = safeErrorMessage(err, this.editing
          ? $localize`:@@common.error-update:Erreur lors de la mise à jour.`
          : $localize`:@@common.error-create:Erreur lors de la création.`);
      }
    });
  }

  private payload(): AiSystemPayload {
    const v = this.form.getRawValue();
    return {
      name: v.name.trim(),
      intendedPurpose: v.intendedPurpose.trim(),
      description: blankToNull(v.description),
      providerName: blankToNull(v.providerName),
      riskClassification: v.riskClassification,
      role: v.role,
      generalPurpose: v.generalPurpose,
      conformityAssessmentEvidenceUrl: blankToNull(v.conformityAssessmentEvidenceUrl),
      ceMarkingNumber: blankToNull(v.ceMarkingNumber),
      humanOversightDescription: blankToNull(v.humanOversightDescription),
      transparencyMeasures: blankToNull(v.transparencyMeasures),
      dataGovernanceNotes: blankToNull(v.dataGovernanceNotes),
      linkedDpiaId: blankToNull(v.linkedDpiaId),
      linkedProcessingActivityIds: parseUuidList(v.linkedProcessingActivityIds),
      linkedAutomatedDecisionIds: parseUuidList(v.linkedAutomatedDecisionIds)
    };
  }

  private patch(system: AiSystemView): void {
    this.form.setValue({
      reference: system.reference,
      name: system.name,
      intendedPurpose: system.intendedPurpose,
      description: system.description ?? '',
      providerName: system.providerName ?? '',
      riskClassification: system.riskClassification,
      role: system.role,
      generalPurpose: system.generalPurpose,
      conformityAssessmentEvidenceUrl: system.conformityAssessmentEvidenceUrl ?? '',
      ceMarkingNumber: system.ceMarkingNumber ?? '',
      humanOversightDescription: system.humanOversightDescription ?? '',
      transparencyMeasures: system.transparencyMeasures ?? '',
      dataGovernanceNotes: system.dataGovernanceNotes ?? '',
      linkedDpiaId: system.linkedDpiaId ?? '',
      linkedProcessingActivityIds: (system.linkedProcessingActivityIds ?? []).join('\n'),
      linkedAutomatedDecisionIds: (system.linkedAutomatedDecisionIds ?? []).join('\n')
    });
  }
}

function blankToNull(value: string): string | null {
  const trimmed = (value ?? '').trim();
  return trimmed.length ? trimmed : null;
}

/** Une saisie libre (une ligne ou une virgule par identifiant) devient un tableau d'UUID. */
export function parseUuidList(value: string): string[] {
  return (value ?? '')
    .split(/[\s,;]+/)
    .map(token => token.trim())
    .filter(token => token.length > 0);
}

/**
 * Refuse une liste d'identifiants dont un seul élément n'est pas un UUID : le
 * serveur rejetterait tout le corps de la requête sans dire lequel est fautif.
 */
export function uuidList(control: AbstractControl): ValidationErrors | null {
  const tokens = parseUuidList(control.value as string);
  const invalid = tokens.filter(token => !UUID_PATTERN.test(token));
  return invalid.length ? { uuidList: invalid } : null;
}
