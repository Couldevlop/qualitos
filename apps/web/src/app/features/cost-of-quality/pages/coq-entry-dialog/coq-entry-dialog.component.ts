import { Component, Inject, OnDestroy } from '@angular/core';
import { FormBuilder, FormControl, ValidatorFn, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Subscription, merge } from 'rxjs';

import { nonBlank } from '../../../apqp/apqp.validators';
import { CATEGORY_TEXT, coqLabelText } from '../../cost-of-quality.labels';
import { CoqCategory, CoqEntryRequest, CoqLabel, CoqLine } from '../../cost-of-quality.types';

export interface CoqEntryDialogData {
  category: CoqCategory;
  /** Les libellés de CETTE famille, dans l'ordre du catalogue. */
  labels: CoqLabel[];
  /** La ligne ouverte ; absente pour une saisie neuve. */
  line?: CoqLine;
  /** Le libellé proposé d'office : celui de la ligne à zéro sur laquelle on a cliqué. */
  presetLabelId?: string;
  /** « aaaa-mm-jj » proposé pour une saisie neuve : un jour du mois affiché. */
  defaultDate: string;
  readOnly: boolean;
  currency: string;
}

/**
 * Ce que rend la fenêtre. Le libellé est soit un libellé EXISTANT (`labelId`),
 * soit un texte libre à ajouter au catalogue (`newLabel`) — c'est l'écran qui
 * le crée avant d'écrire la ligne, pour qu'un échec de l'un n'écrive pas l'autre
 * à moitié.
 */
export interface CoqEntryDialogResult {
  labelId?: string;
  newLabel?: { name: string; partControl: boolean };
  entry: Omit<CoqEntryRequest, 'labelId'>;
}

/** Les quatre champs qui ne valent que pour un contrôle de pièces. */
const CHAMPS_PIECES = ['partReference', 'partQuantity', 'lot', 'receivedOrMadeOn'] as const;

/**
 * Le détail d'une ligne de coût.
 *
 * <p>Le libellé se choisit dans la liste, ou se tape : un texte qui ne
 * correspond à aucun libellé devient un libellé neuf, et une case dit alors
 * s'il porte sur des pièces. Dès que le libellé retenu est un contrôle de
 * pièces, la référence, le nombre de pièces, le lot et la date de réception
 * ou de fabrication deviennent obligatoires — et disparaissent sinon.
 */
@Component({
  selector: 'qos-coq-entry-dialog',
  templateUrl: './coq-entry-dialog.component.html',
  styleUrls: ['./coq-entry-dialog.component.scss'],
  standalone: false
})
export class CoqEntryDialogComponent implements OnDestroy {

  readonly form = this.fb.group({
    label: this.fb.nonNullable.control('', [Validators.required, nonBlank, Validators.maxLength(150)]),
    newPartControl: this.fb.nonNullable.control(false),
    amount: new FormControl<number | null>(null, [Validators.required, Validators.min(0), Validators.max(999999999999.99)]),
    responsible: this.fb.nonNullable.control('', [Validators.required, nonBlank, Validators.maxLength(150)]),
    imputationDate: new FormControl<Date | null>(null, Validators.required),
    comment: this.fb.nonNullable.control('', Validators.maxLength(2000)),
    partReference: this.fb.nonNullable.control(''),
    partQuantity: new FormControl<number | null>(null),
    lot: this.fb.nonNullable.control(''),
    receivedOrMadeOn: new FormControl<Date | null>(null)
  });

  readonly categoryTitle: string;
  readonly title: string;
  readonly labelText = (l: CoqLabel): string => coqLabelText(l.code, l.name);

  /** Le libellé que désigne le texte saisi, ou `undefined` s'il est neuf. */
  selected?: CoqLabel;
  /** Les libellés que la liste propose pour le texte en cours. */
  suggestions: CoqLabel[];
  partControl = false;
  /** Faux tant que les validateurs pièces n'ont jamais été posés. */
  private partControlApplied = false;

  private readonly subscription: Subscription;

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<CoqEntryDialogComponent, CoqEntryDialogResult>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CoqEntryDialogData
  ) {
    this.categoryTitle = CATEGORY_TEXT[data.category].title;
    this.suggestions = data.labels;

    const line = data.line;
    const preset = data.labels.find(l => l.id === (line?.labelId ?? data.presetLabelId));
    this.form.patchValue({
      label: preset ? this.labelText(preset) : '',
      amount: line ? line.amount : null,
      responsible: line?.responsible ?? '',
      imputationDate: CoqEntryDialogComponent.enDateLocale(line?.imputationDate ?? data.defaultDate),
      comment: line?.comment ?? '',
      partReference: line?.partReference ?? '',
      partQuantity: line?.partQuantity ?? null,
      lot: line?.lot ?? '',
      receivedOrMadeOn: CoqEntryDialogComponent.enDateLocale(line?.receivedOrMadeOn)
    });
    this.title = line
      ? coqLabelText(line.labelCode, line.labelName)
      : $localize`:@@coq.dialog.new-title:Nouvelle ligne`;

    this.appliquerLibelle();
    const libelle = this.form.controls.label;
    this.subscription = merge(libelle.valueChanges, this.form.controls.newPartControl.valueChanges)
      .subscribe(() => this.appliquerLibelle());

    if (data.readOnly) {
      this.form.disable({ emitEvent: false });
    }
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  /** Vrai quand le texte saisi ne désigne aucun libellé : il sera ajouté à la liste. */
  get isNewLabel(): boolean {
    return !this.selected && this.form.controls.label.value.trim().length > 0;
  }

  /** Ce qui empêche de valider, nommé plutôt que laissé à deviner. */
  get blockedReason(): string | undefined {
    const c = this.form.controls;
    if (c.label.invalid) return $localize`:@@coq.dialog.blocked-label:Choisissez ou saisissez un libellé.`;
    if (c.amount.invalid) return $localize`:@@coq.dialog.blocked-amount:Indiquez un montant positif ou nul.`;
    if (c.responsible.invalid) return $localize`:@@coq.dialog.blocked-responsible:Indiquez le responsable.`;
    if (c.imputationDate.invalid) return $localize`:@@coq.dialog.blocked-date:Indiquez la date d'imputation.`;
    if (this.partControl && CHAMPS_PIECES.some(n => c[n].invalid)) {
      return $localize`:@@coq.dialog.blocked-parts:Un contrôle de pièces exige la référence, le nombre de pièces, le lot et la date de réception ou de fabrication.`;
    }
    return undefined;
  }

  submit(): void {
    if (this.data.readOnly) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const entry: Omit<CoqEntryRequest, 'labelId'> = {
      amount: Number(v.amount),
      responsible: v.responsible.trim(),
      imputationDate: CoqEntryDialogComponent.enDateIso(v.imputationDate) ?? '',
      comment: v.comment.trim() || undefined
    };
    if (this.partControl) {
      entry.partReference = v.partReference.trim();
      entry.partQuantity = Number(v.partQuantity);
      entry.lot = v.lot.trim();
      entry.receivedOrMadeOn = CoqEntryDialogComponent.enDateIso(v.receivedOrMadeOn) ?? undefined;
    }
    this.dialogRef.close(this.selected
      ? { labelId: this.selected.id, entry }
      : { newLabel: { name: v.label.trim(), partControl: v.newPartControl }, entry });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  /**
   * Relit le libellé saisi : quel libellé il désigne, ce que la liste
   * propose, et si les champs pièces s'imposent.
   */
  private appliquerLibelle(): void {
    const texte = this.form.controls.label.value.trim().toLocaleLowerCase();
    this.selected = this.data.labels.find(l => this.labelText(l).toLocaleLowerCase() === texte);
    this.suggestions = texte && !this.selected
      ? this.data.labels.filter(l => this.labelText(l).toLocaleLowerCase().includes(texte))
      : this.data.labels;

    const pieces = this.selected ? this.selected.partControl : this.form.controls.newPartControl.value;
    if (pieces === this.partControl && this.partControlApplied) return;
    this.partControl = pieces;
    this.partControlApplied = true;

    const c = this.form.controls;
    const exiger = (validators: ValidatorFn[]): ValidatorFn[] => (pieces ? validators : []);
    c.partReference.setValidators(exiger([Validators.required, nonBlank, Validators.maxLength(120)]));
    c.partQuantity.setValidators(exiger([Validators.required, Validators.min(1), Validators.pattern(/^\d+$/)]));
    c.lot.setValidators(exiger([Validators.required, nonBlank, Validators.maxLength(120)]));
    c.receivedOrMadeOn.setValidators(exiger([Validators.required]));
    for (const nom of CHAMPS_PIECES) {
      c[nom].updateValueAndValidity({ emitEvent: false });
    }
  }


  /**
   * Un `Date` du calendrier devient « aaaa-mm-jj » sur ses composantes LOCALES.
   * Surtout pas `toISOString()`, qui repasse par UTC et recule d'un jour à
   * l'est de Greenwich (cf. le même piège dans le dialogue de livrable APQP).
   */
  static enDateIso(valeur: Date | null | undefined): string | null {
    if (!(valeur instanceof Date) || Number.isNaN(valeur.getTime())) return null;
    const mois = String(valeur.getMonth() + 1).padStart(2, '0');
    const jour = String(valeur.getDate()).padStart(2, '0');
    return `${valeur.getFullYear()}-${mois}-${jour}`;
  }

  /** L'inverse, composante par composante : `new Date('2026-09-18')` serait lu en UTC. */
  static enDateLocale(valeur: string | null | undefined): Date | null {
    if (!valeur) return null;
    const p = /^(\d{4})-(\d{2})-(\d{2})/.exec(valeur);
    return p ? new Date(Number(p[1]), Number(p[2]) - 1, Number(p[3])) : null;
  }
}
