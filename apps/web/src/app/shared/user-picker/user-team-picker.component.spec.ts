import { Component } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { DirectoryService } from '../../core/directory/directory.service';
import { DirectoryUser } from '../../core/directory/directory.types';
import { USER_PICKER_DEBOUNCE_MS, UserPickerComponent } from './user-picker.component';
import { TeamMemberRef, UserTeamPickerComponent } from './user-team-picker.component';
import { UserPickerModule } from './user-picker.module';

function user(accountId: string, displayName: string): DirectoryUser {
  return { id: `id-${accountId}`, accountId, displayName, email: `${accountId}@exemple.fr`, active: true };
}

const ANNUAIRE = [
  user('alice', 'Alice Martin'),
  user('bruno', 'Bruno Lefèvre'),
  user('chloe', 'Chloé Nguyen')
];

@Component({
  template: `<qos-user-team-picker [formControl]="control"
                                   [required]="required"
                                   [max]="max"></qos-user-team-picker>`,
  standalone: false
})
class HoteComponent {
  readonly control = new FormControl<TeamMemberRef[]>([], { nonNullable: true });
  required = true;
  max = 12;
}

/**
 * Le sélecteur d'équipe : désigner plusieurs personnes de l'annuaire, dans leur
 * ordre de saisie.
 *
 * <p>Deux promesses tiennent tout le reste : l'ordre est de l'information — le
 * premier nommé pilote, et l'écran le dit — et <b>jamais un identifiant à
 * l'écran</b>, seulement des noms.
 */
describe('UserTeamPickerComponent', () => {

  let fixture: ComponentFixture<HoteComponent>;
  let hote: HoteComponent;
  let directory: jasmine.SpyObj<DirectoryService>;

  const racine = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const un = (sel: string) => racine().querySelector<HTMLElement>(`[data-test="${sel}"]`);
  const tous = (sel: string) => Array.from(racine().querySelectorAll<HTMLElement>(`[data-test="${sel}"]`));
  const equipe = (): UserTeamPickerComponent =>
    fixture.debugElement.children[0].componentInstance as UserTeamPickerComponent;

  beforeEach(async () => {
    directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    directory.search.and.returnValue(of({
      content: ANNUAIRE, page: 0, size: 20, totalElements: ANNUAIRE.length, totalPages: 1
    }));
    directory.getByAccount.and.callFake((accountId: string) => {
      const trouve = ANNUAIRE.find(u => u.accountId === accountId);
      return trouve ? of(trouve) : throwError(() => ({ status: 404 }));
    });

    await TestBed.configureTestingModule({
      declarations: [HoteComponent],
      imports: [UserPickerModule, ReactiveFormsModule, NoopAnimationsModule],
      providers: [{ provide: DirectoryService, useValue: directory }]
    }).compileComponents();

    fixture = TestBed.createComponent(HoteComponent);
    hote = fixture.componentInstance;
    fixture.detectChanges();
  });

  /** Choisit une personne comme le ferait l'autocomplétion du sélecteur interne. */
  function choisir(accountId: string): void {
    const trouve = ANNUAIRE.find(u => u.accountId === accountId)!;
    equipe().onPicked(trouve);
    fixture.detectChanges();
  }

  // ---------- ajouter ----------

  it('démarre sans personne et le dit', () => {
    expect(equipe().members).toEqual([]);
    expect(un('team-empty')).not.toBeNull();
    expect(un('team-list')).toBeNull();
  });

  it('ajoute la personne choisie et la montre par son nom', () => {
    choisir('alice');
    const lignes = tous('team-member');
    expect(lignes.length).toBe(1);
    expect(lignes[0].textContent).toContain('Alice Martin');
    // Jamais l'identifiant de compte à l'écran.
    expect(racine().textContent).not.toContain('alice@');
    expect(lignes[0].textContent).not.toContain('accountId');
  });

  it('remonte la liste au formulaire', () => {
    choisir('alice');
    choisir('bruno');
    expect(hote.control.value).toEqual([
      { accountId: 'alice', displayName: 'Alice Martin' },
      { accountId: 'bruno', displayName: 'Bruno Lefèvre' }
    ]);
  });

  // L'ordre de saisie est de l'information : un tri alphabétique perdrait le pilote.
  it('conserve l’ordre de saisie et nomme le premier pilote', () => {
    choisir('chloe');
    choisir('alice');
    expect(hote.control.value.map(m => m.accountId)).toEqual(['chloe', 'alice']);
    expect(tous('team-member')[0].textContent).toContain('Chloé Nguyen');
    expect(tous('team-lead').length).toBe(1);
    expect(tous('team-member')[0].querySelector('[data-test="team-lead"]')).not.toBeNull();
  });

  it('vide le champ de recherche après chaque ajout', () => {
    const picker = fixture.debugElement.query(
      (n): boolean => n.componentInstance instanceof UserPickerComponent
    ).componentInstance as UserPickerComponent;
    const vide = spyOn(picker, 'clear').and.callThrough();
    choisir('alice');
    expect(vide).toHaveBeenCalled();
  });

  it('ignore une personne déjà nommée', () => {
    choisir('alice');
    choisir('alice');
    expect(equipe().members.length).toBe(1);
  });

  // Ce que le sélecteur interne ne propose plus : sinon on clique un nom déjà pris.
  it('exclut les personnes déjà nommées des propositions', fakeAsync(() => {
    choisir('alice');
    const picker = fixture.debugElement.query(
      (n): boolean => n.componentInstance instanceof UserPickerComponent
    ).componentInstance as UserPickerComponent;
    picker.onFocus();
    tick(USER_PICKER_DEBOUNCE_MS);
    fixture.detectChanges();
    expect(picker.options.map(u => u.accountId)).not.toContain('alice');
    expect(picker.options.map(u => u.accountId)).toContain('bruno');
  }));

  it('ne nomme personne quand l’annuaire ne rend rien', () => {
    equipe().onPicked(null);
    expect(equipe().members).toEqual([]);
    expect(hote.control.value).toEqual([]);
  });

  // ---------- la borne haute ----------

  it('refuse d’ajouter au-delà du maximum et remplace le champ par une explication', () => {
    hote.max = 2;
    fixture.detectChanges();
    choisir('alice');
    choisir('bruno');
    choisir('chloe');
    expect(equipe().members.length).toBe(2);
    expect(un('team-full')).not.toBeNull();
    expect(un('team-picker-input')).toBeNull();
  });

  // ---------- retirer et réordonner ----------

  it('retire la personne de la ligne cliquée, pas une autre', () => {
    choisir('alice');
    choisir('bruno');
    choisir('chloe');
    tous('team-remove')[1].click();
    fixture.detectChanges();
    expect(hote.control.value.map(m => m.accountId)).toEqual(['alice', 'chloe']);
  });

  it('remonte une personne d’un rang : c’est ainsi qu’on change de pilote', () => {
    choisir('alice');
    choisir('bruno');
    // La première ligne n'a pas de bouton « remonter » : elle est déjà en tête.
    expect(tous('team-promote').length).toBe(1);
    tous('team-promote')[0].click();
    fixture.detectChanges();
    expect(hote.control.value.map(m => m.accountId)).toEqual(['bruno', 'alice']);
    expect(tous('team-member')[0].textContent).toContain('Bruno');
  });

  it('ne bouge pas quand on remonte une position hors de la liste', () => {
    choisir('alice');
    equipe().promote(0);
    equipe().promote(7);
    expect(hote.control.value.map(m => m.accountId)).toEqual(['alice']);
  });

  it('redit « personne n’est nommée » quand la dernière est retirée', () => {
    choisir('alice');
    tous('team-remove')[0].click();
    fixture.detectChanges();
    expect(un('team-empty')).not.toBeNull();
    expect(hote.control.value).toEqual([]);
  });

  // ---------- le formulaire pilote ----------

  it('affiche l’équipe que le formulaire lui donne', () => {
    hote.control.setValue([
      { accountId: 'bruno', displayName: 'Bruno Lefèvre' },
      { accountId: 'alice', displayName: 'Alice Martin' }
    ]);
    fixture.detectChanges();
    expect(tous('team-member').length).toBe(2);
    expect(tous('team-member')[0].textContent).toContain('Bruno');
  });

  // Une valeur partagée que le composant trierait en place changerait sous le
  // formulaire, qui ne verrait jamais passer de modification.
  it('ne modifie pas en place la liste reçue du formulaire', () => {
    const recue: TeamMemberRef[] = [
      { accountId: 'alice', displayName: 'Alice Martin' },
      { accountId: 'bruno', displayName: 'Bruno Lefèvre' }
    ];
    hote.control.setValue(recue);
    fixture.detectChanges();
    equipe().promote(1);
    expect(recue.map(m => m.accountId)).toEqual(['alice', 'bruno']);
  });

  it('écarte les lignes sans compte que le serveur pourrait rendre', () => {
    hote.control.setValue([
      { accountId: '', displayName: 'Fantôme' },
      { accountId: 'alice', displayName: 'Alice Martin' }
    ]);
    fixture.detectChanges();
    expect(equipe().members.map(m => m.accountId)).toEqual(['alice']);
  });

  // Un nom vide laisserait une ligne muette : le compte sert alors de pis-aller.
  it('se rabat sur le compte quand le nom manque', () => {
    hote.control.setValue([{ accountId: 'alice', displayName: '' }]);
    fixture.detectChanges();
    expect(equipe().members[0].displayName).toBe('alice');
  });

  it('n’ajoute ni ne retire rien quand le champ est désactivé', () => {
    hote.control.setValue([{ accountId: 'alice', displayName: 'Alice Martin' }]);
    hote.control.disable();
    fixture.detectChanges();
    expect(un('team-picker-input')).toBeNull();
    expect(un('team-remove')).toBeNull();
    equipe().onPicked(ANNUAIRE[1]);
    equipe().remove(0);
    expect(equipe().members.length).toBe(1);
  });

  // ---------- l'accessibilité des boutons ----------

  it('nomme la personne dans l’étiquette de chaque bouton', () => {
    choisir('alice');
    choisir('bruno');
    expect(tous('team-remove')[0].getAttribute('aria-label')).toContain('Alice Martin');
    expect(tous('team-promote')[0].getAttribute('aria-label')).toContain('Bruno Lefèvre');
  });
});
