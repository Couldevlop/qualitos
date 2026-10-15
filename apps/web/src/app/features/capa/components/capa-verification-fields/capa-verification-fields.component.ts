import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { BehaviorSubject, Subscription } from 'rxjs';

import { DirectoryUser } from '../../../../core/directory/directory.types';
import { deferredView } from '../../../../core/rx/deferred-view';

/** Les trois champs de la décision de vérification, portés par le formulaire de l'écran hôte. */
export interface CapaVerificationControls {
  /** `null` = la question n'a pas été tranchée ; c'est distinct de `false`. */
  verificationRequired: FormControl<boolean | null>;
  /** Le compte de CONNEXION (`sub`) du vérificateur, rendu par le sélecteur d'annuaire. */
  verificationAssigneeId: FormControl<string | null>;
  verificationInstructions: FormControl<string>;
}

/**
 * La décision de vérification d'efficacité d'un dossier CAPA (ADR 0073) : la
 * question « exigée ? », puis, si oui, à qui elle est confiée et quoi vérifier.
 *
 * <p>UN bloc, partagé par la fenêtre d'ouverture (ADR 0088) et par la fenêtre
 * d'édition : deux copies du même formulaire divergeraient, et c'est sur ce
 * bloc conditionnel que quatre NG0100 avaient été produits. Le remède est ici,
 * une fois — flux différé, validateur posé hors rendu, erreur qui attend
 * `touched`.
 *
 * <p>Les champs appartiennent au formulaire de l'écran hôte : c'est lui qui
 * décide si la question est OBLIGATOIRE (validateur posé à sa construction,
 * donc jamais pendant un rendu) et qui envoie la décision.
 */
@Component({
  selector: 'qos-capa-verification-fields',
  templateUrl: './capa-verification-fields.component.html',
  styleUrls: ['./capa-verification-fields.component.scss'],
  standalone: false
})
export class CapaVerificationFieldsComponent implements OnInit, OnDestroy {

  /** Le groupe de l'écran hôte, ou tout objet qui en expose les trois contrôles. */
  @Input({ required: true }) group!: { controls: CapaVerificationControls };

  /**
   * Vrai quand il faut trancher (ouverture d'un dossier) ; faux quand ne pas
   * répondre laisse la question ouverte (édition). Ne change que le texte
   * d'aide et l'annonce aux lecteurs d'écran : le validateur, lui, est posé
   * par l'hôte.
   */
  @Input() mandatory = false;

  /** La personne choisie comme vérificatrice : l'hôte recopie son nom avec son compte. */
  @Output() readonly verifierSelected = new EventEmitter<DirectoryUser | null>();

  /**
   * Vrai quand le bloc « à qui » et « quoi vérifier » a lieu d'être rempli.
   *
   * <p>Livré en MACROTÂCHE, et non lu directement : le groupe de boutons radio
   * se synchronise PENDANT le premier rendu, si bien qu'une lecture directe
   * change de valeur au milieu de la passe de détection et déclenche NG0100.
   * Un microtour ne suffit pas : sous Zone.js il peut encore retomber dans la
   * même passe. `deferredView` est le remède déjà employé dans ce dépôt.
   */
  private readonly exigenceState$ = new BehaviorSubject<boolean>(false);
  readonly verificationExigee$ = deferredView(this.exigenceState$);

  private readonly subscriptions = new Subscription();

  get controls(): CapaVerificationControls {
    return this.group.controls;
  }

  /** État courant, pour la logique et les bancs — le gabarit passe par le flux. */
  get verificationExigee(): boolean {
    return this.exigenceState$.value;
  }

  ngOnInit(): void {
    const exigence = this.controls.verificationRequired;
    // L'abonnement ne fait QUE publier l'état : aucune mutation du formulaire
    // ici. Toucher au formulaire pendant la synchronisation des boutons radio
    // ferait basculer `form.invalid` — lu par le bouton d'envoi — au milieu de
    // la passe de détection (NG0100).
    this.subscriptions.add(exigence.valueChanges.subscribe(v => this.exigenceState$.next(v === true)));
    this.exigenceState$.next(exigence.value === true);
    // Les mutations, elles, suivent le flux DIFFÉRÉ : jamais pendant un rendu.
    this.subscriptions.add(this.verificationExigee$.subscribe(exigee => this.appliquerExigence(exigee)));
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  onVerifier(user: DirectoryUser | null): void {
    this.verifierSelected.emit(user);
  }

  /**
   * Ce qu'entraîne l'exigence, ou son retrait.
   *
   * <p>Une seule source pour la règle « exiger sans désigner ne veut rien
   * dire » : ce validateur. Le doubler d'un `required` dans le gabarit donnait
   * deux validateurs, dont un seul se retirait — et la directive l'attacherait
   * à la naissance du champ, donc pendant le rendu.
   */
  private appliquerExigence(exigee: boolean): void {
    const qui = this.controls.verificationAssigneeId;

    if (exigee) {
      qui.addValidators(Validators.required);
      qui.updateValueAndValidity({ emitEvent: false });
      return;
    }

    qui.removeValidators(Validators.required);
    // Ne plus exiger efface ce qui n'a plus d'objet : laisser un vérificateur
    // derrière soi laisserait croire qu'une vérification est encore attendue.
    qui.setValue(null, { emitEvent: false });
    this.controls.verificationInstructions.setValue('', { emitEvent: false });
    qui.updateValueAndValidity({ emitEvent: false });
  }
}
