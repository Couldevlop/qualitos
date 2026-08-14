import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import {
  ClauseDetail, ClauseRequest, ObligationLevel, RequirementDetail, RequirementRequest,
  RiskLevel, SectionDetail, SectionRequest
} from '../../standards.types';

/** Niveau de l'arborescence que la boîte sert. */
export type TreeLevel = 'SECTION' | 'CLAUSE' | 'REQUIREMENT';

export interface TreeNodeDialogData {
  level: TreeLevel;
  /** Nœud existant en modification ; absent en création. */
  node?: SectionDetail | ClauseDetail | RequirementDetail;
}

export type TreeNodeResult = SectionRequest | ClauseRequest | RequirementRequest;

/** Longueurs des colonnes correspondantes côté serveur. */
const CODE_MAX: Record<TreeLevel, number> = { SECTION: 20, CLAUSE: 30, REQUIREMENT: 30 };

/**
 * Saisie d'un nœud du référentiel : section, clause ou exigence (§8).
 *
 * Une seule boîte pour les trois niveaux plutôt que trois presque identiques :
 * ils partagent le code et le libellé, et seul le niveau « exigence » ajoute
 * l'obligation, la preuve attendue et le risque. Trois composants auraient
 * triplé le même formulaire — et la première correction n'en aurait touché qu'un.
 *
 * La boîte ne fait qu'établir la saisie : elle rend une requête et laisse
 * l'écran appeler le serveur, qui seul sait dire 409 (code déjà pris) et 403
 * (norme de la plateforme).
 */
@Component({
  selector: 'qos-tree-node-dialog',
  templateUrl: './tree-node-dialog.component.html',
  styleUrls: ['./tree-node-dialog.component.scss'],
  standalone: false
})
export class TreeNodeDialogComponent {

  readonly obligations: ObligationLevel[] = ['MUST', 'SHOULD', 'MAY'];
  readonly risks: RiskLevel[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(CODE_MAX[this.data.level])]],
    title: [''],
    description: [''],
    text: [''],
    obligation: ['MUST' as ObligationLevel],
    evidenceTypes: [''],
    measurableCriteria: [''],
    riskIfMissing: ['' as RiskLevel | '']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<TreeNodeDialogComponent, TreeNodeResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: TreeNodeDialogData
  ) {
    if (this.isRequirement) {
      this.form.controls.text.addValidators(Validators.required);
    } else {
      this.form.controls.title.addValidators([Validators.required, Validators.maxLength(500)]);
    }
    this.prefill();
  }

  get isRequirement(): boolean {
    return this.data.level === 'REQUIREMENT';
  }

  get editing(): boolean {
    return !!this.data.node;
  }

  get codeMaxLength(): number {
    return CODE_MAX[this.data.level];
  }

  /**
   * Le titre nomme le niveau ET le geste : « Nouvelle clause » et « Modifier la
   * clause » n'appellent pas la même vigilance, et l'écran est le seul endroit
   * où l'utilisateur peut encore vérifier qu'il n'édite pas la mauvaise ligne.
   */
  get title(): string {
    if (this.data.level === 'SECTION') {
      return this.editing
        ? $localize`:@@standards.tree.title-edit-section:Modifier la section`
        : $localize`:@@standards.tree.title-new-section:Nouvelle section`;
    }
    if (this.data.level === 'CLAUSE') {
      return this.editing
        ? $localize`:@@standards.tree.title-edit-clause:Modifier la clause`
        : $localize`:@@standards.tree.title-new-clause:Nouvelle clause`;
    }
    return this.editing
      ? $localize`:@@standards.tree.title-edit-requirement:Modifier l'exigence`
      : $localize`:@@standards.tree.title-new-requirement:Nouvelle exigence`;
  }

  private prefill(): void {
    const node = this.data.node;
    if (!node) return;
    this.form.patchValue({ code: node.code });
    if (this.isRequirement) {
      const r = node as RequirementDetail;
      this.form.patchValue({
        text: r.text,
        obligation: r.obligation,
        evidenceTypes: r.evidenceTypes ?? '',
        measurableCriteria: r.measurableCriteria ?? '',
        riskIfMissing: r.riskIfMissing ?? ''
      });
    } else {
      const s = node as SectionDetail | ClauseDetail;
      this.form.patchValue({ title: s.title, description: s.description ?? '' });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close(this.toRequest());
  }

  /**
   * Les champs facultatifs laissés vides partent à `undefined`, pas en chaîne
   * vide : le serveur les efface, et une preuve attendue supprimée doit
   * réellement disparaître de l'écran plutôt que d'y rester en blanc.
   */
  private toRequest(): TreeNodeResult {
    const v = this.form.getRawValue();
    const code = v.code.trim();
    if (this.isRequirement) {
      return {
        code,
        text: v.text.trim(),
        obligation: v.obligation,
        evidenceTypes: v.evidenceTypes.trim() || undefined,
        measurableCriteria: v.measurableCriteria.trim() || undefined,
        riskIfMissing: v.riskIfMissing || undefined
      } as RequirementRequest;
    }
    return {
      code,
      title: v.title.trim(),
      description: v.description.trim() || undefined
    } as SectionRequest;
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
