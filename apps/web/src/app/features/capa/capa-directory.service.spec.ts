import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth/auth.service';
import { TenantUserPage } from '../admin/admin.types';
import { TenantTeamService } from '../admin/tenant-team.service';
import { CapaDirectoryService, DirectoryChoices, DirectoryMember, matchMember } from './capa-directory.service';

describe('CapaDirectoryService', () => {
  let service: CapaDirectoryService;
  let prevMock: boolean;
  let snapshot: jasmine.Spy;
  let list: jasmine.Spy;

  const page = (content: Partial<TenantUserPage['content'][number]>[]): TenantUserPage =>
    ({ content: content as TenantUserPage['content'], totalElements: content.length, totalPages: 1, number: 0, size: 200 });

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    snapshot = jasmine.createSpy('snapshot').and.returnValue({
      userId: 'kc-moi', tenantId: 't', displayName: 'Moi Même', roles: []
    });
    list = jasmine.createSpy('list');
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { snapshot } },
        { provide: TenantTeamService, useValue: { list } }
      ]
    });
    service = TestBed.inject(CapaDirectoryService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('propose moi d abord, puis les membres actifs dotes d un compte, par adresse', () => {
    list.and.returnValue(of(page([
      { keycloakId: 'kc-2', email: 'zoe@client.test', active: true },
      { keycloakId: 'kc-1', email: 'amina@client.test', active: true },
      { keycloakId: 'kc-moi', email: 'moi@client.test', active: true },
      { keycloakId: 'kc-3', email: 'parti@client.test', active: false },
      { keycloakId: '', email: 'sanscompte@client.test', active: true }
    ])));
    let recu: DirectoryChoices | undefined;
    service.choices().subscribe(c => (recu = c));

    expect(recu?.available).toBeTrue();
    expect(recu?.members).toEqual([
      { userId: 'kc-moi', label: 'Moi Même' },
      { userId: 'kc-1', label: 'amina@client.test' },
      { userId: 'kc-2', label: 'zoe@client.test' }
    ]);
  });

  it('lit l annuaire une seule fois par session', () => {
    list.and.returnValue(of(page([])));
    service.choices().subscribe();
    service.choices().subscribe();
    expect(list).toHaveBeenCalledTimes(1);
  });

  it('sans droit de lire l annuaire, se propose soi-meme et le dit ; le refus n est pas conserve', () => {
    list.and.returnValue(throwError(() => ({ status: 403 })));
    let recu: DirectoryChoices | undefined;
    service.choices().subscribe(c => (recu = c));
    expect(recu).toEqual({ members: [{ userId: 'kc-moi', label: 'Moi Même' }], available: false });

    list.and.returnValue(of(page([])));
    service.choices().subscribe(c => (recu = c));
    expect(list).toHaveBeenCalledTimes(2);
    expect(recu?.available).toBeTrue();
  });

  it('sans session ni annuaire, ne propose personne', () => {
    snapshot.and.returnValue(null);
    list.and.returnValue(throwError(() => ({ status: 403 })));
    let recu: DirectoryChoices | undefined;
    service.choices().subscribe(c => (recu = c));
    expect(recu).toEqual({ members: [], available: false });
    expect(service.currentMember()).toBeNull();
  });

  it('le nom de session vide retombe sur l identifiant de connexion', () => {
    snapshot.and.returnValue({ userId: 'kc-moi', tenantId: 't', displayName: '', roles: [] });
    expect(service.currentMember()).toEqual({ userId: 'kc-moi', label: 'kc-moi' });
  });

  it('en demonstration, propose une equipe fictive sans appel reseau', (done) => {
    environment.useMockApi = true;
    service.choices().subscribe(c => {
      expect(list).not.toHaveBeenCalled();
      expect(c.members[0].userId).toBe('kc-moi');
      expect(c.members.length).toBe(4);
      done();
    });
  });
});

describe('matchMember', () => {
  const membres: DirectoryMember[] = [
    { userId: '1', label: 'a.diallo@client.test' },
    { userId: '2', label: 'amina.dridi@client.test' },
    { userId: '3', label: 'Marc Lefèvre' }
  ];

  it('rapproche un nom ecrit en clair, sans casse, accents, espaces ni points', () => {
    expect(matchMember(membres, 'A. Diallo')?.userId).toBe('1');
    expect(matchMember(membres, 'Amina Dridi')?.userId).toBe('2');
    expect(matchMember(membres, 'marc lefevre')?.userId).toBe('3');
    expect(matchMember(membres, 'AMINA.DRIDI@client.test')?.userId).toBe('2');
  });

  it('ne devine rien d approchant', () => {
    expect(matchMember(membres, 'Diallo')).toBeUndefined();
    expect(matchMember(membres, 'Service Qualité')).toBeUndefined();
    expect(matchMember(membres, '  ')).toBeUndefined();
    expect(matchMember(membres, null)).toBeUndefined();
    expect(matchMember(membres, undefined)).toBeUndefined();
  });
});
