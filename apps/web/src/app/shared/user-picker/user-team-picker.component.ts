import {
  ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output,
  ViewChild, booleanAttribute, forwardRef
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { DirectoryUser } from '../../core/directory/directory.types';
import { UserPickerComponent } from './user-picker.component';

/**
 * Une personne de l'équipe : son compte de connexion, et son nom à la saisie.
 *
 * <p>Les deux, et pas seulement le compte : un rapport relu des années plus tard
 * doit porter les noms de l'époque, même si la personne a quitté l'annuaire.
 */
export interface TeamMemberRef {
  accountId: string;
  displayName: string;
}

let nextTeamId = 0;

/**
 * Désigner PLUSIEURS personnes : l'équipe qui prend un écart en charge (D1 du
 * 8D), les participants d'un cercle, les destinataires d'une revue.
 *
 * <p>Champ de formulaire à part entière (`ControlValueAccessor`) dont la valeur
 * est la liste des personnes retenues, <b>dans leur ordre de saisie</b> — le
 * premier nommé pilote, par convention d'atelier, et un tri alphabétique le
 * perdrait.
 *
 * <p>Il s'appuie sur {@link UserPickerComponent} au lieu de refaire la recherche
 * dans l'annuaire : une seconde implémentation divergerait sur la saisie libre,
 * les personnes inconnues et le temps de frappe. À chaque choix, la personne
 * rejoint la liste, le champ se vide, et elle n'est plus proposée.
 *
 * <pre>
 *   &lt;qos-user-team-picker formControlName="teamMembers" required
 *                         label="Équipe en charge"&gt;&lt;/qos-user-team-picker&gt;
 * </pre>
 */
@Component({
  selector: 'qos-user-team-picker',
  templateUrl: './user-team-picker.component.html',
  styleUrls: ['./user-team-picker.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => UserTeamPickerComponent),
    multi: true
  }],
  standalone: false
})
export class UserTeamPickerComponent implements ControlValueAccessor {

  @Input() label = $localize`:@@directory.team.label:Équipe`;
  @Input() placeholder = $localize`:@@directory.team.placeholder:Rechercher par nom ou adresse`;
  @Input() hint = '';
  @Input({ transform: booleanAttribute }) required = false;
  /** Au-delà, on n'ajoute plus : une équipe de résolution n'est pas une liste de diffusion. */
  @Input() max = 12;

  /** La liste retenue, à chaque changement. */
  @Output() readonly teamChanged = new EventEmitter<TeamMemberRef[]>();

  @ViewChild(UserPickerComponent) private picker?: UserPickerComponent;

  readonly listId = `qos-user-team-${nextTeamId++}`;

  members: TeamMemberRef[] = [];
  disabled = false;

  private onChange: (value: TeamMemberRef[]) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  constructor(private readonly cdr: ChangeDetectorRef) {}

  // ---------- ControlValueAccessor ----------

  writeValue(value: readonly TeamMemberRef[] | null | undefined): void {
    // Copie défensive : le formulaire ne doit pas voir sa valeur changer sous lui
    // parce que ce composant a ajouté ou réordonné en place.
    this.members = (value ?? [])
      .filter(m => !!m && !!m.accountId)
      .map(m => ({ accountId: m.accountId, displayName: m.displayName || m.accountId }));
    this.cdr.markForCheck();
  }

  registerOnChange(fn: (value: TeamMemberRef[]) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.disabled = disabled;
    this.cdr.markForCheck();
  }

  // ---------- la liste ----------

  /** Les comptes déjà retenus : le sélecteur ne les propose plus. */
  get excluded(): string[] {
    return this.members.map(m => m.accountId);
  }

  get full(): boolean {
    return this.members.length >= this.max;
  }

  get empty(): boolean {
    return this.members.length === 0;
  }

  trackByAccount(_: number, member: TeamMemberRef): string {
    return member.accountId;
  }

  /**
   * Une personne a été choisie dans l'annuaire.
   *
   * <p>Le sélecteur interne est vidé aussitôt : garder le nom affiché laisserait
   * croire qu'il reste à valider, alors qu'il est déjà dans la liste.
   */
  onPicked(user: DirectoryUser | null): void {
    if (!user || this.disabled || this.full) {
      return;
    }
    if (this.members.some(m => m.accountId === user.accountId)) {
      return;
    }
    this.members = [...this.members, {
      accountId: user.accountId,
      displayName: user.displayName || user.accountId
    }];
    this.picker?.clear();
    this.publish();
  }

  remove(index: number): void {
    if (this.disabled || index < 0 || index >= this.members.length) {
      return;
    }
    this.members = this.members.filter((_, i) => i !== index);
    this.publish();
  }

  /** Remonte une personne d'un rang : c'est ainsi qu'on désigne le pilote. */
  promote(index: number): void {
    if (this.disabled || index <= 0 || index >= this.members.length) {
      return;
    }
    const suite = [...this.members];
    [suite[index - 1], suite[index]] = [suite[index], suite[index - 1]];
    this.members = suite;
    this.publish();
  }

  removeLabel(member: TeamMemberRef): string {
    return $localize`:@@directory.team.remove-one:Retirer ${member.displayName}:NAME: de l'équipe`;
  }

  promoteLabel(member: TeamMemberRef): string {
    return $localize`:@@directory.team.promote-one:Remonter ${member.displayName}:NAME: d'un rang`;
  }

  private publish(): void {
    this.onChange(this.members);
    this.onTouched();
    this.teamChanged.emit(this.members);
    this.cdr.markForCheck();
  }
}
