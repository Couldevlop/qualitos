import { Component, Inject } from '@angular/core';
import { catalogCodes } from '../../../../core/catalogs/catalog-overrides';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { CreateDeviceRequest, PokaYokeMechanism, PokaYokeType } from '../../dmaic.types';
import {
  POKAYOKE_MECHANISMS, POKAYOKE_TYPES, pokaYokeMechanismLabel, pokaYokeTypeLabel
} from '../../pokayoke.labels';

export interface PokaYokeCreateDialogData {
  /** Le projet dont l'action est issue : le dispositif y sera déployé. */
  projectId: string;
  /** Son intitulé, rappelé, et cité dans l'origine du dispositif. */
  projectTitle: string;
  /** Le texte de l'action, qui propose le nom et la description du dispositif. */
  action: string;
  /** Le rang de l'action dans le plan, cité dans l'origine. */
  position: number;
}

/** Ce que l'écran a retenu, tel que le serveur l'attend. */
export type PokaYokeCreated = CreateDeviceRequest;

/** Le nom tient en 255 caractères côté serveur ; l'écran ne laisse pas dépasser. */
const NOM_MAX = 255;

/**
 * Inscrire une action d'amélioration à la bibliothèque des détrompeurs.
 *
 * <p>Une action qui empêche l'erreur de se produire n'a pas à être réinventée
 * sur la ligne voisine : elle devient un détrompeur de la bibliothèque du
 * client, réutilisable sur un autre projet.
 *
 * <p>La fenêtre propose le nom à partir du texte de l'action — l'opérateur qui
 * l'a écrite a déjà nommé la solution — et impose les deux choses que l'action
 * ne porte pas : <b>comment le dispositif agit</b> (prévenir l'erreur, ou la
 * détecter) et <b>par quel mécanisme</b>. Sans elles, la bibliothèque devient
 * une liste de phrases qu'on ne peut plus filtrer.
 *
 * <p>L'origine n'est pas saisie : elle est construite à partir du projet et du
 * rang de l'action. Une origine libre finirait vide ou fausse, et un dispositif
 * dont on ne sait plus d'où il vient ne se réutilise pas en confiance.
 */
@Component({
  selector: 'qos-pokayoke-create-dialog',
  templateUrl: './pokayoke-create-dialog.component.html',
  styleUrls: ['./pokayoke-create-dialog.component.scss'],
  standalone: false
})
export class PokaYokeCreateDialogComponent {

  readonly types: readonly PokaYokeType[] = POKAYOKE_TYPES;
  /** Dans l'ordre du client (ADR 0111) ; les mots suivent par `pokaYokeMechanismLabel`. */
  readonly mechanisms = catalogCodes<PokaYokeMechanism>('pokayoke-mechanism', POKAYOKE_MECHANISMS);
  readonly typeLabel = pokaYokeTypeLabel;
  readonly mechanismLabel = pokaYokeMechanismLabel;
  readonly nameMax = NOM_MAX;

  /**
   * L'origine du dispositif, telle qu'elle sera citée. Construite, non saisie,
   * et montrée en lecture : la personne doit voir ce que la bibliothèque
   * retiendra.
   */
  readonly origin: string = PokaYokeCreateDialogComponent.originFor(
    this.data.projectTitle, this.data.position);

  readonly form = this.fb.nonNullable.group({
    name: [PokaYokeCreateDialogComponent.nameFrom(this.data.action),
           [Validators.required, Validators.maxLength(NOM_MAX)]],
    description: [this.data.action?.trim() ?? ''],
    type: ['PREVENTION' as PokaYokeType, [Validators.required]],
    mechanism: ['PHYSICAL_SHAPE' as PokaYokeMechanism, [Validators.required]],
    applicableIndustries: [''],
    examples: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<PokaYokeCreateDialogComponent, PokaYokeCreated>,
    @Inject(MAT_DIALOG_DATA) public readonly data: PokaYokeCreateDialogData
  ) {}

  /**
   * Le nom proposé : la première phrase de l'action, tronquée au besoin.
   *
   * <p>Un nom de bibliothèque se lit en un coup d'œil ; le détail de l'action
   * part dans la description, où il reste lisible en entier.
   */
  static nameFrom(action: string | null | undefined): string {
    const texte = (action ?? '').trim().replace(/\s+/g, ' ');
    if (!texte) return '';
    const fin = texte.search(/[.;!?]\s|[.;!?]$/);
    const phrase = fin > 0 ? texte.slice(0, fin) : texte;
    return phrase.length > NOM_MAX ? phrase.slice(0, NOM_MAX - 1).trimEnd() + '…' : phrase;
  }

  /** L'origine citée : le projet, et le rang de l'action dans son plan. */
  static originFor(projectTitle: string, position: number): string {
    const titre = (projectTitle ?? '').trim();
    const projet = titre.length > 160 ? titre.slice(0, 159).trimEnd() + '…' : titre;
    return $localize`:@@pokayoke.create.origin-text:Projet DMAIC « ${projet}:project: » — action n° ${position}:position: du plan d'actions`;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.dialogRef.close({
      name: v.name.trim(),
      description: v.description.trim() || undefined,
      type: v.type,
      mechanism: v.mechanism,
      applicableIndustries: v.applicableIndustries.trim() || undefined,
      examples: v.examples.trim() || undefined,
      sourceReference: this.origin,
      // Le dispositif est déployé sur le projet dont l'action est issue :
      // l'inscrire sans le poser laisserait l'action à moitié faite.
      projectId: this.data.projectId
    });
  }

  cancel(): void { this.dialogRef.close(); }
}
