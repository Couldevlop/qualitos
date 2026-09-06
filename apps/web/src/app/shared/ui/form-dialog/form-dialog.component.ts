import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';

/**
 * Gabarit de dialog-formulaire du design system (CLAUDE.md §15).
 *
 * Standardise la structure ergonomique de toutes les popups de saisie : titre,
 * contenu projeté (les champs du formulaire) dans l'unique zone défilante, et un
 * pied d'actions fixe « Annuler / valider » avec état de chargement. Combiné à la
 * classe globale `qos-dialog-panel` (styles.scss), il garantit en-tête + actions
 * toujours visibles et un seul ascenseur.
 *
 * Le `formGroup` du composant hôte est appliqué au `<form>` interne : les champs
 * projetés (formControlName) se relient naturellement (contexte de formulaire hérité).
 *
 * @example
 * <qos-form-dialog title="Nouveau plan" [formGroup]="form" [submitting]="saving"
 *                  [submitDisabled]="form.invalid" submitLabel="Créer"
 *                  (submitted)="submit()" (cancelled)="cancel()">
 *   <mat-form-field appearance="outline" class="full">…</mat-form-field>
 * </qos-form-dialog>
 */
@Component({
  selector: 'qos-form-dialog',
  templateUrl: './form-dialog.component.html',
  styleUrls: ['./form-dialog.component.scss'],
  standalone: false
})
export class FormDialogComponent {

  @Input() title = '';
  /** Sous-titre / aide affiché en tête de contenu (optionnel). */
  @Input() subtitle?: string;
  /** FormGroup de l'hôte, appliqué au <form> interne. */
  @Input({ required: true }) formGroup!: FormGroup;
  /** Affiche le spinner et désactive les actions pendant la soumission. */
  @Input() submitting = false;
  /** Désactive le bouton de validation (typiquement form.invalid). */
  @Input() submitDisabled = false;
  @Input() submitLabel = $localize`:@@common.save:Enregistrer`;
  @Input() submitIcon = 'check';
  /** Couleur du bouton de validation ('primary' par défaut, 'warn' pour les actions sensibles). */
  @Input() submitColor: 'primary' | 'accent' | 'warn' = 'primary';
  @Input() cancelLabel = $localize`:@@common.cancel:Annuler`;

  /**
   * Ce qui empêche de valider, dit en clair dans la barre d'actions.
   *
   * <p>Un bouton grisé sans explication est une impasse : l'utilisateur voit
   * qu'il ne peut pas valider, jamais pourquoi, et il lui reste à parcourir le
   * formulaire à la recherche du champ fautif — d'autant que les erreurs des
   * champs restés vides ne s'affichent qu'une fois touchés.
   *
   * <p>Laisser vide pour le repli générique ; le renseigner quand l'hôte sait
   * nommer le champ manquant (« Renseignez la description »), ce qui vaut
   * toujours mieux qu'une formule passe-partout.
   */
  @Input() blockedReason?: string;

  /**
   * Le message affiché, ou `undefined` quand il n'y a rien à dire.
   *
   * <p>Rien pendant l'envoi : le spinner parle déjà, et annoncer un blocage
   * au moment où l'on valide serait un contresens.
   */
  get blockedMessage(): string | undefined {
    if (!this.submitDisabled || this.submitting) {
      return undefined;
    }
    return this.blockedReason
      ?? $localize`:@@common.form-blocked:Des champs obligatoires restent à renseigner.`;
  }

  /** Émis à la soumission (le parent appelle son service). */
  @Output() submitted = new EventEmitter<void>();
  /** Émis à l'annulation (le parent ferme le dialogRef). */
  @Output() cancelled = new EventEmitter<void>();
}

