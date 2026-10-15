import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';

import { DirectoryService } from '../../core/directory/directory.service';
import { DirectoryUser } from '../../core/directory/directory.types';
import { personUnknownLabel } from './person-name.component';
import { PersonNameModule } from './person-name.module';

@Component({
  standalone: false,
  template: `<qos-person-name [accountId]="id" [name]="name" [emptyLabel]="empty"></qos-person-name>`
})
class HostComponent {
  id: string | null = null;
  name: string | null = null;
  empty = '—';
}

describe('PersonNameComponent', () => {
  const AMINA: DirectoryUser = {
    id: '11111111-1111-4111-8111-111111111111', accountId: 'aaaaaaaa-1111-4111-8111-111111111111',
    displayName: 'Amina Dridi', email: 'amina@client.test', active: true
  };

  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;
  let directory: jasmine.SpyObj<DirectoryService>;

  beforeEach(async () => {
    directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['getByAccount']);
    directory.getByAccount.and.callFake((id: string) =>
      id === AMINA.accountId ? of(AMINA) : throwError(() => ({ status: 404 })));
    await TestBed.configureTestingModule({
      declarations: [HostComponent],
      imports: [PersonNameModule],
      providers: [{ provide: DirectoryService, useValue: directory }]
    }).compileComponents();
    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
  });

  const text = (): string =>
    (fixture.nativeElement as HTMLElement).querySelector('[data-test="person-name"]')!.textContent!.trim();
  const span = (): HTMLElement =>
    (fixture.nativeElement as HTMLElement).querySelector('[data-test="person-name"]') as HTMLElement;

  it('affiche le nom recopié par le dossier sans interroger l\'annuaire', () => {
    host.id = AMINA.accountId;
    host.name = '  Paul Martin ';
    fixture.detectChanges();
    expect(text()).toBe('Paul Martin');
    expect(directory.getByAccount).not.toHaveBeenCalled();
  });

  it('résout le nom par l\'annuaire et met l\'adresse en infobulle', () => {
    host.id = AMINA.accountId;
    fixture.detectChanges();
    expect(text()).toBe('Amina Dridi');
    expect(span().getAttribute('title')).toBe('amina@client.test');
    expect(span().classList).not.toContain('qos-person--muted');
  });

  it('affiche un libellé neutre, jamais l\'identifiant, pour un compte inconnu', () => {
    host.id = 'bbbbbbbb-2222-4222-8222-222222222222';
    fixture.detectChanges();
    expect(text()).toBe(personUnknownLabel());
    expect(text()).not.toContain('bbbbbbbb');
    expect(span().classList).toContain('qos-person--muted');
  });

  it('ne cherche pas une valeur qui n\'a pas la forme d\'un compte', () => {
    host.id = 'demo-user';
    fixture.detectChanges();
    expect(text()).toBe(personUnknownLabel());
    expect(directory.getByAccount).not.toHaveBeenCalled();
  });

  it('affiche le libellé vide quand le dossier ne désigne personne', () => {
    host.empty = 'Système';
    fixture.detectChanges();
    expect(text()).toBe('Système');
  });

  it('signale la lecture en cours puis abandonne l\'ancienne au changement de compte', () => {
    const pending = new Subject<DirectoryUser>();
    directory.getByAccount.and.returnValue(pending.asObservable());
    host.id = AMINA.accountId;
    fixture.detectChanges();
    expect(span().getAttribute('aria-busy')).toBe('true');
    expect(text()).not.toContain('aaaaaaaa');

    host.id = null;
    fixture.detectChanges();
    pending.next(AMINA);
    fixture.detectChanges();
    expect(text()).toBe('—');
    expect(span().getAttribute('aria-busy')).toBeNull();
  });
});
