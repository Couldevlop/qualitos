import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';

import { FiveWhysService } from '../../five-whys.service';
import { FiveWhysAnalysis } from '../../five-whys.types';

/**
 * Déroulé d'une analyse des 5 Pourquoi.
 *
 * <p>L'écran suit la méthode plutôt qu'il ne la décore : on ajoute un pourquoi à
 * la suite du précédent, on ne peut retirer que le dernier — ôter un maillon du
 * milieu casserait la chaîne —, et la cause racine ne se conclut pas avant le
 * troisième pourquoi. Le serveur applique ces mêmes règles : l'écran ne fait que
 * les rendre visibles au lieu de les faire découvrir par un refus.
 */
@Component({
  selector: 'qos-five-whys-detail',
  templateUrl: './five-whys-detail.component.html',
  styleUrls: ['./five-whys-detail.component.scss'],
  standalone: false
})
export class FiveWhysDetailComponent implements OnInit {

  /** Borne haute de la chaîne, alignée sur la règle du serveur. */
  readonly maxWhys = 7;
  /** En deçà, la « cause racine » est encore un symptôme. */
  readonly minWhysBeforeRootCause = 3;

  analysis: FiveWhysAnalysis | null = null;
  loading = false;

  newAnswer = '';
  rootCauseDraft = '';
  /** Étape en cours de correction (une seule à la fois). */
  editingStepId: string | null = null;
  editAnswer = '';
  saving = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly service: FiveWhysService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    this.load(id);
  }

  private load(id: string): void {
    this.loading = true;
    this.service.get(id).subscribe({
      next: analysis => {
        this.analysis = analysis;
        this.rootCauseDraft = analysis.rootCause ?? '';
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.fail($localize`:@@fivewhys.load-failed:Analyse introuvable.`);
        this.router.navigate(['/five-whys']);
      }
    });
  }

  get steps() { return this.analysis?.steps ?? []; }

  /** Nombre de pourquoi déjà posés — c'est lui qui ouvre ou ferme les actions. */
  get depth(): number { return this.steps.length; }

  get canAddWhy(): boolean { return this.depth < this.maxWhys; }

  get canConcludeRootCause(): boolean { return this.depth >= this.minWhysBeforeRootCause; }

  /** Le « pourquoi ? » posé à l'utilisateur porte le rang à venir. */
  get nextQuestion(): string {
    return $localize`:@@fivewhys.next-question:Pourquoi ?` + ' (' + (this.depth + 1) + ')';
  }

  addWhy(): void {
    const answer = this.newAnswer.trim();
    if (!answer || !this.analysis || !this.canAddWhy) {
      return;
    }
    this.saving = true;
    this.service.addStep(this.analysis.id, answer).subscribe({
      next: step => {
        this.analysis!.steps = [...this.steps, step];
        this.newAnswer = '';
        this.saving = false;
      },
      error: () => {
        this.saving = false;
        this.fail($localize`:@@fivewhys.add-failed:Ajout du pourquoi refusé.`);
      }
    });
  }

  startEdit(stepId: string, answer: string): void {
    this.editingStepId = stepId;
    this.editAnswer = answer;
  }

  cancelEdit(): void {
    this.editingStepId = null;
    this.editAnswer = '';
  }

  commitEdit(stepId: string, previous: string): void {
    const answer = this.editAnswer.trim();
    if (!answer) {
      this.fail($localize`:@@fivewhys.answer-required:La réponse au pourquoi est obligatoire.`);
      return;
    }
    if (answer === previous) {
      this.cancelEdit();   // un clic à côté ne doit pas produire un appel inutile
      return;
    }
    this.service.updateStep(stepId, answer).subscribe({
      next: updated => {
        this.analysis!.steps = this.steps.map(s => (s.id === updated.id ? updated : s));
        this.cancelEdit();
      },
      error: () => {
        this.cancelEdit();
        this.fail($localize`:@@fivewhys.update-failed:Modification refusée.`);
      }
    });
  }

  /** Seul le dernier maillon peut partir : le gabarit ne propose que celui-là. */
  removeLastWhy(): void {
    const last = this.steps[this.steps.length - 1];
    if (!last) {
      return;
    }
    this.service.deleteStep(last.id).subscribe({
      next: () => { this.analysis!.steps = this.steps.filter(s => s.id !== last.id); },
      error: () => this.fail($localize`:@@fivewhys.delete-step-failed:Suppression refusée.`)
    });
  }

  concludeRootCause(): void {
    const rootCause = this.rootCauseDraft.trim();
    if (!rootCause || !this.analysis || !this.canConcludeRootCause) {
      return;
    }
    this.saving = true;
    this.service.setRootCause(this.analysis.id, rootCause).subscribe({
      next: updated => {
        // La réponse du serveur porte la chaîne : on la garde plutôt que de
        // recopier notre état, qui pourrait avoir dérivé.
        this.analysis = updated;
        this.rootCauseDraft = updated.rootCause ?? '';
        this.saving = false;
      },
      error: () => {
        this.saving = false;
        this.fail($localize`:@@fivewhys.root-cause-failed:Conclusion refusée.`);
      }
    });
  }

  goBack(): void { this.router.navigate(['/five-whys']); }

  /** Retour à l'écart constaté d'où part l'analyse (§3.6 — référentiel commun). */
  openNc(): void {
    if (this.analysis) {
      this.router.navigate(['/nc', this.analysis.ncId]);
    }
  }

  private fail(message: string): void {
    this.snack.open(message, $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
