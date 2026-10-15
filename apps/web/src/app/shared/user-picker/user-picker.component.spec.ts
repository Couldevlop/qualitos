import { OverlayContainer } from '@angular/cdk/overlay';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, flush, tick } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { DirectoryService } from '../../core/directory/directory.service';
import { DirectoryPage, DirectoryUser } from '../../core/directory/directory.types';
import { USER_PICKER_DEBOUNCE_MS, UserPickerComponent } from './user-picker.component';
import { UserPickerModule } from './user-picker.module';

@Component({
  standalone: false,
  template: `
    <qos-user-picker [formControl]="ctrl" label="Responsable" hint="Celui qui porte l'action"
                     [excludeIds]="exclude" (userSelected)="picked.push($event)"></qos-user-picker>`
})
class HostComponent {
  ctrl = new FormControl<string | null>(null);
  exclude: string[] = [];
  picked: (DirectoryUser | null)[] = [];
}

describe('UserPickerComponent', () => {

  const ALICE: DirectoryUser = { id: '11111111-1111-4111-8111-111111111111', accountId: 'aaaaaaaa-1111-4111-8111-111111111111', displayName: 'Alice Durand', email: 'alice@acme.com', active: true };
  const BOB: DirectoryUser = { id: '22222222-2222-4222-8222-222222222222', accountId: 'bbbbbbbb-2222-4222-8222-222222222222', displayName: 'bob@acme.com', email: 'bob@acme.com', active: true };
  const ANCIEN: DirectoryUser = { id: '33333333-3333-4333-8333-333333333333', accountId: 'cccccccc-3333-4333-8333-333333333333', displayName: 'Paul Ancien', email: 'paul@acme.com', active: false };

  const page = (content: DirectoryUser[], total = content.length): DirectoryPage =>
    ({ content, page: 0, size: 20, totalElements: total, totalPages: 1 });

  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;
  let directory: jasmine.SpyObj<DirectoryService>;
  let overlay: OverlayContainer;

  beforeEach(async () => {
    directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    directory.search.and.returnValue(of(page([ALICE, BOB])));
    directory.getByAccount.and.callFake((accountId: string) =>
      [ALICE, BOB, ANCIEN].some(u => u.accountId === accountId)
        ? of([ALICE, BOB, ANCIEN].find(u => u.accountId === accountId)!)
        : throwError(() => ({ status: 404 })));

    await TestBed.configureTestingModule({
      declarations: [HostComponent],
      imports: [UserPickerModule, ReactiveFormsModule, NoopAnimationsModule],
      providers: [{ provide: DirectoryService, useValue: directory }]
    }).compileComponents();

    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
    overlay = TestBed.inject(OverlayContainer);
    fixture.detectChanges();
  });

  const root = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const input = (): HTMLInputElement => root().querySelector('[data-test="user-picker-input"]') as HTMLInputElement;
  const picker = (): UserPickerComponent =>
    fixture.debugElement.children[0].componentInstance as UserPickerComponent;
  const panel = (): HTMLElement => overlay.getContainerElement();
  const options = (): HTMLElement[] =>
    Array.from(panel().querySelectorAll<HTMLElement>('[data-test="user-picker-option"]'));

  /** Le déclencheur d'autocomplétion écrit le texte affiché en microtâche. */
  async function settle(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function type(text: string): void {
    input().focus();
    input().value = text;
    input().dispatchEvent(new Event('input'));
    tick(USER_PICKER_DEBOUNCE_MS);
    fixture.detectChanges();
  }

  it('affiche le nom de la personne déjà choisie, jamais son identifiant', async () => {
    host.ctrl.setValue(ALICE.accountId);
    await settle();
    expect(directory.getByAccount).toHaveBeenCalledWith(ALICE.accountId);
    expect(input().value).toBe('Alice Durand');
    expect(root().textContent).not.toContain(ALICE.accountId);
    expect(root().textContent).not.toContain(ALICE.id);
  });

  it('une personne absente de l’annuaire : champ vide et message, pas d’identifiant', async () => {
    const inconnu = '99999999-9999-4999-8999-999999999999';
    host.ctrl.setValue(inconnu);
    await settle();
    expect(input().value).toBe('');
    expect(root().querySelector('[data-test="user-picker-unknown"]')).not.toBeNull();
    expect(root().textContent).not.toContain(inconnu);
    expect(host.ctrl.value).toBe(inconnu);
  });

  it('signale un ancien membre', async () => {
    host.ctrl.setValue(ANCIEN.accountId);
    await settle();
    expect(input().value).toBe('Paul Ancien');
    expect(root().querySelector('[data-test="user-picker-former"]')).not.toBeNull();
  });

  it('cherche après la frappe et propose noms et adresses', fakeAsync(() => {
    type('ali');
    expect(directory.search).toHaveBeenCalledWith('ali', 20);
    const opts = options();
    expect(opts.length).toBe(2);
    expect(opts[0].textContent).toContain('Alice Durand');
    expect(opts[0].textContent).toContain('alice@acme.com');
    // Sans nom enregistré, l'adresse n'est pas répétée deux fois.
    expect(opts[1].textContent!.match(/bob@acme\.com/g)?.length).toBe(1);
    flush();
  }));

  it('choisir une proposition donne son identifiant au formulaire et la signale', fakeAsync(() => {
    type('ali');
    options()[0].click();
    fixture.detectChanges();
    flush();
    expect(host.ctrl.value).toBe(ALICE.accountId);
    expect(host.picked).toEqual([ALICE]);
    expect(input().value).toBe('Alice Durand');
  }));

  it('n’interroge pas l’annuaire à chaque touche', fakeAsync(() => {
    input().dispatchEvent(new Event('focusin'));
    for (const t of ['a', 'al', 'ali']) {
      input().value = t;
      input().dispatchEvent(new Event('input'));
      tick(50);
    }
    tick(USER_PICKER_DEBOUNCE_MS);
    expect(directory.search).toHaveBeenCalledTimes(1);
    expect(directory.search).toHaveBeenCalledWith('ali', 20);
    flush();
  }));

  it('écarte les personnes exclues', fakeAsync(() => {
    host.exclude = [ALICE.accountId];
    fixture.detectChanges();
    type('a');
    expect(options().map(o => o.textContent)).not.toContain(jasmine.stringContaining('Alice'));
    expect(options().length).toBe(1);
    flush();
  }));

  it('dit quand rien ne correspond', fakeAsync(() => {
    directory.search.and.returnValue(of(page([])));
    type('zzz');
    expect(panel().querySelector('[data-test="user-picker-empty"]')).not.toBeNull();
    expect(picker().liveMessage).toContain('Aucune');
    flush();
  }));

  it('dit quand l’annuaire ne répond pas', fakeAsync(() => {
    directory.search.and.returnValue(throwError(() => new Error('503')));
    type('ali');
    expect(panel().querySelector('[data-test="user-picker-error"]')).not.toBeNull();
    expect(picker().state).toBe('error');
    flush();
  }));

  it('invite à préciser quand il y a plus de résultats que de propositions', fakeAsync(() => {
    directory.search.and.returnValue(of(page([ALICE, BOB], 42)));
    type('a');
    const more = panel().querySelector('[data-test="user-picker-more"]');
    expect(more?.textContent).toContain('40');
    flush();
  }));

  it('pas de saisie libre : un texte quelconque redevient la personne choisie', async () => {
    host.ctrl.setValue(ALICE.accountId);
    await settle();
    picker().text.setValue('n’importe quoi');
    picker().reconcile();
    await settle();
    expect(input().value).toBe('Alice Durand');
    expect(host.ctrl.value).toBe(ALICE.accountId);
  });

  it('pas de saisie libre : sans personne choisie, le texte est effacé sans rien signaler', async () => {
    picker().text.setValue('quelqu’un');
    picker().reconcile();
    await settle();
    expect(input().value).toBe('');
    expect(host.ctrl.value).toBeNull();
    expect(host.picked).toEqual([]);
  });

  it('un champ vide sans choix ne signale rien', () => {
    picker().text.setValue('');
    picker().reconcile();
    expect(host.picked).toEqual([]);
    expect(host.ctrl.touched).toBeTrue();
  });

  it('ne réconcilie pas tant que la liste est ouverte', fakeAsync(() => {
    host.ctrl.setValue(ALICE.accountId);
    fixture.detectChanges();
    flush();
    type('xyz');
    // Le déclencheur réévalue la visibilité de la liste au tour suivant.
    tick();
    fixture.detectChanges();
    picker().reconcile();
    expect(picker().text.value).toBe('xyz');
    flush();
  }));

  it('rappeler la même valeur ne relit pas l’annuaire', () => {
    host.ctrl.setValue(ALICE.accountId);
    fixture.detectChanges();
    host.ctrl.setValue(ALICE.accountId);
    expect(directory.getByAccount).toHaveBeenCalledTimes(1);
  });

  it('affiche un texte, une personne ou rien', () => {
    expect(picker().displayWith('ali')).toBe('ali');
    expect(picker().displayWith(ALICE)).toBe('Alice Durand');
    expect(picker().displayWith(null)).toBe('');
    expect(picker().trackById(0, ALICE)).toBe(ALICE.accountId);
  });

  it('un second focus ne relance pas la recherche', fakeAsync(() => {
    picker().onFocus();
    tick(USER_PICKER_DEBOUNCE_MS);
    picker().onFocus();
    tick(USER_PICKER_DEBOUNCE_MS);
    expect(directory.search).toHaveBeenCalledTimes(1);
    flush();
  }));

  it('au premier focus après un choix, cherche sans terme', fakeAsync(() => {
    host.ctrl.setValue(ALICE.accountId);
    fixture.detectChanges();
    flush();
    picker().onFocus();
    tick(USER_PICKER_DEBOUNCE_MS);
    expect(directory.search).toHaveBeenCalledWith('', 20);
    flush();
  }));

  it('vider le texte efface le choix', () => {
    host.ctrl.setValue(ALICE.accountId);
    fixture.detectChanges();
    picker().text.setValue('');
    picker().reconcile();
    expect(host.ctrl.value).toBeNull();
    expect(host.picked).toEqual([null]);
    expect(host.ctrl.touched).toBeTrue();
  });

  it('le bouton de retrait vide la valeur et porte un nom accessible', () => {
    host.ctrl.setValue(ALICE.accountId);
    fixture.detectChanges();
    const clear = root().querySelector('[data-test="user-picker-clear"]') as HTMLButtonElement;
    expect(clear.getAttribute('aria-label')).toBeTruthy();
    clear.click();
    fixture.detectChanges();
    expect(host.ctrl.value).toBeNull();
    expect(input().value).toBe('');
    expect(root().querySelector('[data-test="user-picker-clear"]')).toBeNull();
  });

  it('suit l’état désactivé du formulaire', () => {
    host.ctrl.disable();
    fixture.detectChanges();
    expect(input().disabled).toBeTrue();
    host.ctrl.enable();
    fixture.detectChanges();
    expect(input().disabled).toBeFalse();
  });

  it('remettre la valeur à null vide le champ', () => {
    host.ctrl.setValue(ALICE.accountId);
    fixture.detectChanges();
    host.ctrl.setValue(null);
    fixture.detectChanges();
    expect(input().value).toBe('');
  });

  it('montre le libellé et l’aide fournis', () => {
    expect(root().textContent).toContain('Responsable');
    expect(root().textContent).toContain('Celui qui porte l\'action');
  });

  it('au premier focus, propose les premiers noms sans attendre une frappe', fakeAsync(() => {
    picker().onFocus();
    tick(USER_PICKER_DEBOUNCE_MS);
    expect(directory.search).toHaveBeenCalledWith('', 20);
    flush();
  }));
});
