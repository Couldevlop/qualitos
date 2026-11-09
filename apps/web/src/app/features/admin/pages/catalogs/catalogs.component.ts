import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CatalogService } from '../../../../core/catalogs/catalog.service';
import {
  Catalog,
  CatalogEntry,
  CatalogMode,
  CatalogName,
  CatalogSummary
} from '../../../../core/catalogs/catalog.types';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';

/** La forme d'un code, comme le serveur l'admet : capitales, chiffres, tiret, souligné. */
const CODE = /^[A-Z0-9][A-Z0-9_-]{0,39}$/;

/** Les bornes du serveur (`CatalogEntry`, `CatalogId`). */
export const CATALOG_LIMITS = { code: 40, label: 120, entries: 200 } as const;

/**
 * « Listes déroulantes », réservé à l'administrateur du client : les mots que
 * prennent les listes de l'application, dans la maison.
 *
 * <p>Deux degrés de liberté, et c'est le code qui les impose, pas une
 * précaution. Une liste <b>adossée au code</b> (type d'audit, nature de CAPA,
 * catégorie d'écart) se renomme et se réordonne : ajouter une valeur qu'aucun
 * chemin de code ne connaît ferait accepter une donnée que la plateforme ne
 * saurait pas traiter. Une <b>taxonomie libre</b> (zones d'audit 5S, secteurs
 * de parties intéressées) appartient entièrement au client.
 *
 * <p>Une entrée garde son CODE : en changer le libellé ne déclasse rien ; la
 * retirer laisse l'historique porter son code, affiché tel quel — effacer un
 * classement passé réécrirait l'histoire.
 */
@Component({
  selector: 'qos-catalogs',
  templateUrl: './catalogs.component.html',
  styleUrls: ['./catalogs.component.scss'],
  standalone: false
})
export class CatalogsComponent implements OnInit {

  readonly limits = CATALOG_LIMITS;

  summaries: CatalogSummary[] = [];

  /** La liste en cours d'édition : une à la fois, pour rester lisible. */
  selected: CatalogName | null = null;
  mode: CatalogMode = 'OPEN';
  entries: CatalogEntry[] = [];
  defaults = true;

  loading = false;
  failed = false;
  saving = false;

  private savedJson = '';

  constructor(
    private readonly service: CatalogService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    this.service.summaries().subscribe({
      next: list => {
        this.loading = false;
        this.summaries = list;
        const premiere = this.selected ?? list[0]?.catalogue ?? null;
        if (premiere) {
          this.ouvrir(premiere);
        }
      },
      error: err => {
        this.loading = false;
        this.failed = true;
        this.echouer(err, $localize`:@@catalogs.load-failed:Les listes déroulantes n'ont pas pu être chargées.`);
      }
    });
  }

  /** Ouvre une liste. Un changement non enregistré est confirmé avant d'être perdu. */
  ouvrir(name: CatalogName): void {
    if (this.selected === name && this.entries.length) {
      return;
    }
    if (this.dirty) {
      this.dialog.open(ConfirmDialogComponent, {
        data: {
          title: $localize`:@@catalogs.leave-title:Abandonner vos changements ?`,
          message: $localize`:@@catalogs.leave-message:Cette liste a été modifiée sans être enregistrée. Ouvrir une autre liste perdra ces changements.`,
          confirmLabel: $localize`:@@catalogs.leave:Abandonner`,
          cancelLabel: $localize`:@@common.cancel:Annuler`,
          danger: true
        }
      }).afterClosed().subscribe(ok => {
        if (ok) {
          this.recuperer(name);
        }
      });
      return;
    }
    this.recuperer(name);
  }

  get dirty(): boolean {
    return !!this.selected && JSON.stringify(this.entries) !== this.savedJson;
  }

  /** Vrai quand les codes appartiennent au code de la plateforme. */
  get renameOnly(): boolean {
    return this.mode === 'RENAME';
  }

  /**
   * Ce qui empêche d'enregistrer, dit en clair.
   *
   * <p>Les mêmes règles que le serveur, énoncées ici pour qu'une liste de
   * soixante lignes ne reparte pas en erreur sur une faute de frappe en ligne 42.
   */
  get problemes(): string[] {
    const p: string[] = [];
    if (!this.entries.length) {
      p.push($localize`:@@catalogs.problem.empty:La liste ne peut pas être vide. Rétablissez celle de la plateforme.`);
    }
    if (this.entries.length > this.limits.entries) {
      p.push($localize`:@@catalogs.problem.too-many:Pas plus de ${this.limits.entries}:max: entrées.`);
    }
    if (this.entries.some(e => !CODE.test(e.code))) {
      p.push($localize`:@@catalogs.problem.code:Un code s'écrit en capitales, chiffres, tiret ou souligné (ex. : BLOC-A).`);
    }
    if (this.entries.some(e => !e.label.trim())) {
      p.push($localize`:@@catalogs.problem.label:Chaque entrée a un libellé.`);
    }
    const codes = this.entries.map(e => e.code.toUpperCase());
    if (new Set(codes).size !== codes.length) {
      p.push($localize`:@@catalogs.problem.duplicate:Deux entrées portent le même code.`);
    }
    if (this.entries.length && !this.entries.some(e => e.active)) {
      p.push($localize`:@@catalogs.problem.none-active:Au moins une entrée doit rester en service.`);
    }
    return p;
  }

  ajouter(): void {
    if (this.renameOnly || this.entries.length >= this.limits.entries) {
      return;
    }
    this.entries = [...this.entries, { code: '', label: '', active: true }];
  }

  retirer(entry: CatalogEntry): void {
    if (this.renameOnly) {
      return;
    }
    this.entries = this.entries.filter(e => e !== entry);
  }

  /** Monte une entrée d'un rang : l'ordre d'affichage est un choix du client. */
  monter(index: number): void {
    if (index <= 0) {
      return;
    }
    const copie = [...this.entries];
    [copie[index - 1], copie[index]] = [copie[index], copie[index - 1]];
    this.entries = copie;
  }

  descendre(index: number): void {
    if (index >= this.entries.length - 1) {
      return;
    }
    const copie = [...this.entries];
    [copie[index], copie[index + 1]] = [copie[index + 1], copie[index]];
    this.entries = copie;
  }

  /** Un code se saisit en capitales, quoi qu'on tape : le serveur compare ainsi. */
  majCode(entry: CatalogEntry, value: string): void {
    if (this.renameOnly) {
      return;
    }
    entry.code = value.toUpperCase().trim();
    this.entries = [...this.entries];
  }

  majLibelle(entry: CatalogEntry, value: string): void {
    entry.label = value;
    this.entries = [...this.entries];
  }

  /** Retire une valeur des nouvelles saisies sans toucher à l'historique. */
  basculerService(entry: CatalogEntry): void {
    if (this.renameOnly) {
      return;
    }
    entry.active = !entry.active;
    this.entries = [...this.entries];
  }

  annuler(): void {
    this.entries = JSON.parse(this.savedJson) as CatalogEntry[];
  }

  enregistrer(): void {
    if (!this.selected || !this.dirty || this.problemes.length || this.saving) {
      return;
    }
    this.saving = true;
    this.service.replace(this.selected, this.entries.map(e => ({
      code: e.code.trim(), label: e.label.trim(), active: e.active
    }))).subscribe({
      next: c => {
        this.saving = false;
        this.reprendre(c);
        this.rafraichirResume(c);
        this.snack.open($localize`:@@catalogs.saved:Liste enregistrée.`, undefined, { duration: 3000 });
      },
      error: err => {
        this.saving = false;
        this.echouer(err, $localize`:@@catalogs.save-failed:La liste n'a pas pu être enregistrée.`);
      }
    });
  }

  retablir(): void {
    const name = this.selected;
    if (!name) {
      return;
    }
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@catalogs.reset-title:Rétablir la liste de la plateforme ?`,
        message: $localize`:@@catalogs.reset-message:Vos libellés seront remplacés par ceux de la plateforme. Les données déjà enregistrées gardent leur code.`,
        confirmLabel: $localize`:@@catalogs.reset:Rétablir`,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        danger: true
      }
    }).afterClosed().subscribe(ok => {
      if (!ok) {
        return;
      }
      this.saving = true;
      this.service.reset(name).subscribe({
        next: c => {
          this.saving = false;
          this.reprendre(c);
          this.rafraichirResume(c);
          this.snack.open($localize`:@@catalogs.reset-done:Liste de la plateforme rétablie.`, undefined, { duration: 3000 });
        },
        error: err => {
          this.saving = false;
          this.echouer(err, $localize`:@@catalogs.reset-failed:La liste n'a pas pu être rétablie.`);
        }
      });
    });
  }

  /** Le nom d'une liste dans la langue de l'écran. */
  titre(name: CatalogName): string {
    switch (name) {
      case 'nc-category': return $localize`:@@catalogs.name.nc-category:Catégories de non-conformité`;
      case 'audit-type': return $localize`:@@catalogs.name.audit-type:Types d'audit`;
      case 'supplier-type': return $localize`:@@catalogs.name.supplier-type:Familles de fournisseurs`;
      case 'ehs-incident-type': return $localize`:@@catalogs.name.ehs-incident-type:Types d'incident EHS`;
      case 'apqp-project-type': return $localize`:@@catalogs.name.apqp-project-type:Types de projet APQP`;
      case 'capa-nature': return $localize`:@@catalogs.name.capa-nature:Natures de dossier CAPA`;
      case 'moc-change-type': return $localize`:@@catalogs.name.moc-change-type:Types de modification`;
      case 'ishikawa-category': return $localize`:@@catalogs.name.ishikawa-category:Branches d'Ishikawa`;
      case 'kpi-frequency': return $localize`:@@catalogs.name.kpi-frequency:Fréquences de mesure`;
      case 'pokayoke-mechanism': return $localize`:@@catalogs.name.pokayoke-mechanism:Mécanismes de détrompeur`;
      case 'fives-zone': return $localize`:@@catalogs.name.fives-zone:Zones d'audit 5S`;
      case 'stakeholder-sector': return $localize`:@@catalogs.name.stakeholder-sector:Secteurs de parties intéressées`;
      default: return name;
    }
  }

  /** Ce que le client peut faire de cette liste, dit avant qu'il l'essaie. */
  liberte(mode: CatalogMode): string {
    return mode === 'OPEN'
      ? $localize`:@@catalogs.freedom.open:Liste libre : ajoutez, retirez, renommez, réordonnez.`
      : $localize`:@@catalogs.freedom.rename:Codes de la plateforme : renommez et réordonnez. Les valeurs commandent le comportement de l'application, elles ne s'ajoutent ni ne se retirent.`;
  }

  /** Ce que fera le bouton de mise en service, dit au lecteur d'écran. */
  serviceLabel(entry: CatalogEntry): string {
    return entry.active
      ? $localize`:@@catalogs.retire:Retirer des nouvelles saisies`
      : $localize`:@@catalogs.reinstate:Remettre en service`;
  }

  trackByIndex(index: number): number {
    return index;
  }

  private recuperer(name: CatalogName): void {
    this.loading = true;
    this.failed = false;
    this.selected = name;
    this.service.catalog(name).subscribe({
      next: c => {
        this.loading = false;
        this.reprendre(c);
      },
      error: err => {
        this.loading = false;
        this.failed = true;
        this.echouer(err, $localize`:@@catalogs.load-one-failed:Cette liste n'a pas pu être chargée.`);
      }
    });
  }

  private reprendre(c: Catalog): void {
    this.selected = c.catalogue;
    this.mode = c.mode;
    this.defaults = c.defaults;
    this.entries = c.entries.map(e => ({ ...e }));
    this.savedJson = JSON.stringify(this.entries);
  }

  /** Le résumé porte l'origine de chaque liste : il doit suivre l'enregistrement. */
  private rafraichirResume(c: Catalog): void {
    this.summaries = this.summaries.map(s => s.catalogue === c.catalogue
      ? { ...s, defaults: c.defaults, entryCount: c.entries.length }
      : s);
  }

  private echouer(err: unknown, repli: string): void {
    this.snack.open(safeErrorMessage(err, repli), undefined, { duration: 5000 });
  }
}
