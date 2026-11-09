import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { NcService } from '../../../nc/nc.service';
import { NcCategory, NcSubcategory } from '../../../nc/nc.types';
import { AuditsService } from '../../audits.service';
import { FindingNcResponse, FindingResponse } from '../../audits.types';

export interface AuditsDeclareNcDialogData {
  planId: string;
  finding: FindingResponse;
  /** La citation de l'audit, telle que le serveur la calculera : « AUD-… / constat 2 ». */
  sourceRef: string;
}

/**
 * Déclarer la non-conformité d'un constat d'audit.
 *
 * <p>Trois champs, pas plus. La description, la date de relevé, la clause, la
 * gravité et l'origine sont REPRISES du constat par le serveur : les redemander
 * ici ouvrirait la porte à deux versions du même fait, et c'est exactement ce
 * que ce chemin existe pour fermer. L'écran les montre donc en lecture, pour
 * que l'auteur voie ce qui va être inscrit sans pouvoir le réécrire.
 *
 * <p>Le titre, lui, se saisit : la description d'un constat est une phrase
 * d'auditeur (« les enregistrements de formation ne sont pas signés »), et le
 * registre a besoin d'un intitulé court et citable. Il est pré-rempli par la
 * description tronquée plutôt que laissé vide.
 */
@Component({
  selector: 'qos-audits-declare-nc-dialog',
  templateUrl: './audits-declare-nc-dialog.component.html',
  styleUrls: ['./audits-declare-nc-dialog.component.scss'],
  standalone: false
})
export class AuditsDeclareNcDialogComponent {

  submitting = false;

  /** Les catégories du registre, dans l'ordre où elles se lisent. */
  readonly categories: { value: NcCategory; label: string }[] = [
    { value: 'PROCESS', label: $localize`:@@nc.category.process:Processus` },
    { value: 'DOCUMENTATION', label: $localize`:@@nc.category.documentation:Documentation` },
    { value: 'PRODUCT', label: $localize`:@@nc.category.product:Produit` },
    { value: 'SUPPLIER', label: $localize`:@@nc.category.supplier:Fournisseur` },
    { value: 'SAFETY', label: $localize`:@@nc.category.safety:Sécurité` },
    { value: 'ENVIRONMENT', label: $localize`:@@nc.category.environment:Environnement` },
    { value: 'OTHER', label: $localize`:@@nc.category.other:Autre` }
  ];

  /** Le catalogue du client, toutes catégories ; la liste est filtrée à l'affichage. */
  private subcategories: NcSubcategory[] = [];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    // Un écart relevé pendant un audit porte, le plus souvent, sur le
    // fonctionnement du système de management : « Processus » est le défaut le
    // moins faux. Il reste changeable, et rien n'est deviné en silence.
    category: ['PROCESS' as NcCategory, [Validators.required]],
    subcategoryCode: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly audits: AuditsService,
    private readonly nc: NcService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<AuditsDeclareNcDialogComponent, FindingNcResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AuditsDeclareNcDialogData
  ) {
    this.form.controls.title.setValue(data.finding.description.slice(0, 255));
    // Un catalogue illisible n'empêche pas de déclarer l'écart : la nature fine
    // est facultative, et un registre qui refuse la saisie parce qu'un
    // paramétrage manque est pire qu'un écart mal classé.
    this.nc.subcategories().pipe(catchError(() => of({ entries: [] })))
      .subscribe(c => (this.subcategories = c.entries ?? []));
    // Changer de catégorie invalide la nature choisie sous l'ancienne : le
    // serveur refuse un code rattaché à une autre catégorie (400).
    this.form.controls.category.valueChanges
      .subscribe(() => this.form.controls.subcategoryCode.setValue(''));
  }

  /** Les natures du catalogue sous la catégorie retenue. */
  get subcategoryChoices(): NcSubcategory[] {
    const category = this.form.getRawValue().category;
    return this.subcategories.filter(s => s.category === category);
  }

  /** La gravité que le serveur déduira du type de constat, montrée en lecture. */
  get severityLabel(): string {
    return this.data.finding.type === 'MAJOR_NC'
      ? $localize`:@@severity.major:Majeure`
      : $localize`:@@severity.minor:Mineure`;
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.audits
      .declareFindingNc(this.data.planId, this.data.finding.id, {
        title: v.title.trim(),
        category: v.category,
        subcategoryCode: v.subcategoryCode || undefined
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: r => {
          this.snack.open(
            $localize`:@@audits.declare-nc.saved:Écart ${r.ncReference}:reference: inscrit au registre.`,
            $localize`:@@common.ok:OK`, { duration: 4000 });
          this.dialogRef.close(r);
        },
        error: err => this.snack.open(this.messageDErreur(err), $localize`:@@common.ok:OK`, { duration: 6000 })
      });
  }

  cancel(): void { this.dialogRef.close(); }

  /**
   * Le 409 dit ici une chose précise : le constat a déjà son écart. Le message
   * générique (« rechargez la page ») enverrait recharger un écran qui refuse
   * toujours, sans dire que le travail est déjà fait.
   */
  private messageDErreur(err: unknown): string {
    if ((err as { status?: number })?.status === 409) {
      return $localize`:@@audits.declare-nc.already:Ce constat a déjà son écart au registre.`;
    }
    return safeErrorMessage(err,
      $localize`:@@audits.declare-nc.error:L'écart n'a pas pu être inscrit au registre.`);
  }
}
