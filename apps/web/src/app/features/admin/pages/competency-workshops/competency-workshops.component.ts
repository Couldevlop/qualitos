import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { normalize } from '../../../training/competency-matrix/competency-matrix.model';
import { CompetencyMatrixService } from '../../../training/competency-matrix/competency-matrix.service';
import { WorkshopsView } from '../../../training/competency-matrix/competency-matrix.types';

/** Bornes du serveur (`Workshops.MAX_COUNT`, `Workshops.MAX_LENGTH`). */
export const MAX_WORKSHOPS = 30;
export const MAX_WORKSHOP_LENGTH = 60;

/**
 * « Ateliers » de la matrice de compétences (ADR 0098), réservé à
 * l'administrateur du client : la liste dans laquelle on range un opérateur,
 * et par laquelle on filtre la matrice.
 *
 * <p>Tant que rien n'est enregistré, ce sont les ateliers de la plateforme.
 * Retirer un atelier ne réaffecte personne : les opérateurs qui y sont rangés
 * le gardent, et le filtre de la matrice le propose encore.
 */
@Component({
  selector: 'qos-competency-workshops',
  templateUrl: './competency-workshops.component.html',
  styleUrls: ['./competency-workshops.component.scss'],
  standalone: false
})
export class CompetencyWorkshopsComponent implements OnInit {

  readonly maxLength = MAX_WORKSHOP_LENGTH;

  names: string[] = [];
  defaults = true;
  newName = '';

  loading = false;
  failed = false;
  saving = false;

  private saved: string[] = [];

  constructor(
    private readonly service: CompetencyMatrixService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    this.service.workshops().subscribe({
      next: w => {
        this.loading = false;
        this.reprendre(w);
      },
      error: err => {
        this.loading = false;
        this.failed = true;
        this.echouer(err, $localize`:@@training.matrix.workshops-load-failed:Les ateliers n'ont pas pu être chargés.`);
      }
    });
  }

  /** Ce qui empêche d'enregistrer, dit en clair ; vide si tout va bien. */
  get problems(): string[] {
    const p: string[] = [];
    const noms = this.names.map(n => n.trim());
    if (!noms.length) {
      p.push($localize`:@@training.matrix.workshops-problem-empty:Il faut au moins un atelier.`);
    }
    if (noms.length > MAX_WORKSHOPS) {
      p.push($localize`:@@training.matrix.workshops-problem-count:${MAX_WORKSHOPS}:max: ateliers au plus.`);
    }
    if (noms.some(n => !n || n.length > MAX_WORKSHOP_LENGTH)) {
      p.push($localize`:@@training.matrix.workshops-problem-length:Chaque atelier a un nom de 1 à ${MAX_WORKSHOP_LENGTH}:max: caractères.`);
    }
    const cles = noms.filter(n => n).map(normalize);
    if (new Set(cles).size !== cles.length) {
      p.push($localize`:@@training.matrix.workshops-problem-duplicate:Deux ateliers portent le même nom.`);
    }
    return p;
  }

  get dirty(): boolean {
    return this.names.length !== this.saved.length || this.names.some((n, i) => n.trim() !== this.saved[i]);
  }

  add(): void {
    const nom = this.newName.trim();
    if (!nom) return;
    this.names = [...this.names, nom];
    this.newName = '';
  }

  rename(index: number, value: string): void {
    this.names = this.names.map((n, i) => i === index ? value : n);
  }

  remove(index: number): void {
    this.names = this.names.filter((_, i) => i !== index);
  }

  move(index: number, delta: -1 | 1): void {
    const cible = index + delta;
    if (cible < 0 || cible >= this.names.length) return;
    const copie = [...this.names];
    [copie[index], copie[cible]] = [copie[cible], copie[index]];
    this.names = copie;
  }

  trackByIndex(index: number): number {
    return index;
  }

  discard(): void {
    this.names = [...this.saved];
    this.newName = '';
  }

  save(): void {
    if (!this.dirty || this.problems.length || this.saving) return;
    this.saving = true;
    this.service.updateWorkshops(this.names.map(n => n.trim())).subscribe({
      next: w => {
        this.saving = false;
        this.reprendre(w);
        this.snack.open($localize`:@@training.matrix.workshops-saved:Ateliers enregistrés.`, undefined, { duration: 4000 });
      },
      error: err => {
        this.saving = false;
        this.echouer(err, $localize`:@@training.matrix.workshops-save-failed:Les ateliers n'ont pas pu être enregistrés.`);
      }
    });
  }

  private reprendre(w: WorkshopsView): void {
    this.saved = [...w.names];
    this.names = [...w.names];
    this.defaults = w.defaults;
  }

  private echouer(err: unknown, fallback: string): void {
    this.snack.open(safeErrorMessage(err, fallback), undefined, { duration: 5000 });
  }
}
