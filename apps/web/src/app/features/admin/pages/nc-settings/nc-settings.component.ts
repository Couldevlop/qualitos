import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { NcService } from '../../../nc/nc.service';
import { NcCategory, NcSubcategory, NcSubcategoryCatalogue } from '../../../nc/nc.types';

/** La forme d'un code, comme le serveur l'admet : capitales, chiffres, tiret, point. */
const CODE = /^[A-Z0-9][A-Z0-9._-]{0,29}$/;

/** Les bornes du serveur (`NcSubcategory`, `NcSubcategoryCatalog`). */
export const NC_SUBCATEGORY_LIMITS = { code: 30, label: 120, entries: 300 } as const;

/** Les sept catégories, dans l'ordre où la saisie les propose. */
const CATEGORIES: readonly NcCategory[] = [
  'PRODUCT', 'PROCESS', 'DOCUMENTATION', 'SUPPLIER', 'SAFETY', 'ENVIRONMENT', 'OTHER'];

/**
 * « Natures de non-conformité », réservé à l'administrateur du client : le
 * catalogue qui dit, sous chaque catégorie, quelles natures d'écart existent
 * dans la maison.
 *
 * <p>Sans réglage, la plateforme en propose un, transverse à tous les secteurs
 * où la qualité intervient — les natures d'écart d'une usine ne sont pas celles
 * d'un CHU, et aucune liste codée en dur ne conviendrait aux deux (§18.2 #9).
 *
 * <p>Le catalogue s'enregistre d'un bloc. Une nature garde son CODE : en changer
 * le libellé ne déclasse pas les écarts déjà classés ; la retirer laisse les
 * anciens porter leur code, affiché tel quel — effacer un classement passé
 * réécrirait l'histoire.
 */
@Component({
  selector: 'qos-nc-settings',
  templateUrl: './nc-settings.component.html',
  styleUrls: ['./nc-settings.component.scss'],
  standalone: false
})
export class NcSettingsComponent implements OnInit {

  readonly categories = CATEGORIES;
  readonly limits = NC_SUBCATEGORY_LIMITS;

  /** La catégorie dont on édite les natures : une à la fois, pour rester lisible. */
  category: NcCategory = 'PRODUCT';
  entries: NcSubcategory[] = [];
  defaults = true;

  loading = false;
  failed = false;
  saving = false;

  private savedJson = '';

  constructor(
    private readonly service: NcService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    this.service.subcategories().subscribe({
      next: c => {
        this.loading = false;
        this.reprendre(c);
      },
      error: err => {
        this.loading = false;
        this.failed = true;
        this.echouer(err, $localize`:@@nc.settings.load-failed:Le catalogue des natures n'a pas pu être chargé.`);
      }
    });
  }

  /** Les natures de la catégorie affichée, dans l'ordre du catalogue. */
  get courantes(): NcSubcategory[] {
    return this.entries.filter(e => e.category === this.category);
  }

  get dirty(): boolean {
    return JSON.stringify(this.entries) !== this.savedJson;
  }

  /**
   * Ce qui empêche d'enregistrer, dit en clair.
   *
   * <p>Les mêmes règles que le serveur, énoncées ici pour qu'un catalogue de
   * soixante lignes ne reparte pas en erreur sur une faute de frappe en ligne 42.
   */
  get problemes(): string[] {
    const p: string[] = [];
    if (!this.entries.length) {
      p.push($localize`:@@nc.settings.problem.empty:Le catalogue ne peut pas être vide. Rétablissez celui de la plateforme.`);
    }
    if (this.entries.length > this.limits.entries) {
      p.push($localize`:@@nc.settings.problem.too-many:Pas plus de ${this.limits.entries}:max: natures.`);
    }
    if (this.entries.some(e => !CODE.test(e.code))) {
      p.push($localize`:@@nc.settings.problem.code:Un code s'écrit en capitales, chiffres, tiret ou point (ex. : PRD-DIM).`);
    }
    if (this.entries.some(e => !e.label.trim())) {
      p.push($localize`:@@nc.settings.problem.label:Chaque nature a un libellé.`);
    }
    const codes = this.entries.map(e => e.code.toUpperCase());
    if (new Set(codes).size !== codes.length) {
      p.push($localize`:@@nc.settings.problem.duplicate:Deux natures portent le même code.`);
    }
    // Une catégorie sans aucune nature est un cul-de-sac à la saisie : le
    // sélecteur de l'écran de déclaration n'y proposerait rien.
    const vides = this.categories.filter(c => !this.entries.some(e => e.category === c));
    if (vides.length) {
      p.push($localize`:@@nc.settings.problem.empty-category:Catégorie sans aucune nature : ${vides.length}:count: — la saisie n'y proposerait rien.`);
    }
    return p;
  }

  ajouter(): void {
    if (this.entries.length >= this.limits.entries) return;
    this.entries = [...this.entries, { code: '', category: this.category, label: '' }];
  }

  retirer(entry: NcSubcategory): void {
    this.entries = this.entries.filter(e => e !== entry);
  }

  /** Un code se saisit en capitales, quoi qu'on tape : le serveur compare ainsi. */
  majCode(entry: NcSubcategory, value: string): void {
    entry.code = value.toUpperCase().trim();
    this.entries = [...this.entries];
  }

  majLibelle(entry: NcSubcategory, value: string): void {
    entry.label = value;
    this.entries = [...this.entries];
  }

  annuler(): void {
    this.entries = JSON.parse(this.savedJson) as NcSubcategory[];
  }

  enregistrer(): void {
    if (!this.dirty || this.problemes.length || this.saving) return;
    this.saving = true;
    this.service.replaceSubcategories(this.entries.map(e => ({
      code: e.code.trim(), category: e.category, label: e.label.trim()
    }))).subscribe({
      next: c => {
        this.saving = false;
        this.reprendre(c);
        this.snack.open($localize`:@@nc.settings.saved:Catalogue des natures enregistré.`, undefined, { duration: 3000 });
      },
      error: err => {
        this.saving = false;
        this.echouer(err, $localize`:@@nc.settings.save-failed:Le catalogue n'a pas pu être enregistré.`);
      }
    });
  }

  retablir(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@nc.settings.reset-title:Rétablir le catalogue de la plateforme ?`,
        message: $localize`:@@nc.settings.reset-message:Votre catalogue sera remplacé par celui de la plateforme. Les écarts déjà classés gardent leur code.`,
        confirmLabel: $localize`:@@nc.settings.reset:Rétablir`,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        danger: true
      }
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.saving = true;
      this.service.resetSubcategories().subscribe({
        next: c => {
          this.saving = false;
          this.reprendre(c);
          this.snack.open($localize`:@@nc.settings.reset-done:Catalogue de la plateforme rétabli.`, undefined, { duration: 3000 });
        },
        error: err => {
          this.saving = false;
          this.echouer(err, $localize`:@@nc.settings.reset-failed:Le catalogue n'a pas pu être rétabli.`);
        }
      });
    });
  }

  /** Le nombre de natures d'une catégorie : l'onglet le porte. */
  compte(category: NcCategory): number {
    return this.entries.filter(e => e.category === category).length;
  }

  trackByIndex(index: number): number {
    return index;
  }

  private reprendre(c: NcSubcategoryCatalogue): void {
    this.entries = c.entries.map(e => ({ ...e }));
    this.defaults = c.defaults;
    this.savedJson = JSON.stringify(this.entries);
  }

  private echouer(err: unknown, repli: string): void {
    this.snack.open(safeErrorMessage(err, repli), undefined, { duration: 5000 });
  }
}
