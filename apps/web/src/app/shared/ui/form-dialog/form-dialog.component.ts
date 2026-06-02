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
  @Input() submitLabel = 'Enregistrer';
  @Input() submitIcon = 'check';
  @Input() cancelLabel = 'Annuler';

  /** Émis à la soumission (le parent appelle son service). */
  @Output() submitted = new EventEmitter<void>();
  /** Émis à l'annulation (le parent ferme le dialogRef). */
  @Output() cancelled = new EventEmitter<void>();
}
