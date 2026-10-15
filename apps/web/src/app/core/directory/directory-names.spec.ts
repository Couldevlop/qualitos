import { of, throwError } from 'rxjs';

import { AccountNames, findExactPerson, isExactPerson, personName } from './directory-names';
import { DirectoryService } from './directory.service';
import { DirectoryPage, DirectoryUser } from './directory.types';

/**
 * Les petites règles partagées par les écrans qui désignent une personne
 * (ADR 0091) : quel nom recopier, quand un nom écrit ailleurs désigne
 * exactement quelqu'un, et comment nommer le responsable d'un dossier ancien.
 */
describe('directory-names', () => {
  const person = (over: Partial<DirectoryUser>): DirectoryUser => ({
    id: 'i-' + (over.accountId ?? 'x'), accountId: 'x', displayName: 'X', email: 'x@client.test', active: true, ...over
  });
  const page = (content: DirectoryUser[]): DirectoryPage =>
    ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 });

  const AMINA = person({ accountId: 'kc-amina', displayName: 'Amina Dridi', email: 'amina.dridi@client.test' });
  const ELODIE = person({ accountId: 'kc-elodie', displayName: 'Élodie Martin', email: 'e.martin@client.test' });

  describe('personName', () => {
    it('recopie le nom, jamais l\'adresse quand un nom existe', () => {
      expect(personName(AMINA)).toBe('Amina Dridi');
    });

    it('se replie sur l\'adresse quand aucun nom n\'est enregistré', () => {
      expect(personName({ displayName: '  ', email: 'sans.nom@client.test' })).toBe('sans.nom@client.test');
    });
  });

  describe('isExactPerson', () => {
    it('reconnaît le nom ou l\'adresse, accents, casse et espaces ignorés', () => {
      expect(isExactPerson(ELODIE, 'elodie   MARTIN')).toBeTrue();
      expect(isExactPerson(ELODIE, 'E.Martin@Client.test')).toBeTrue();
    });

    it('refuse ce qui n\'est qu\'approchant ou vide', () => {
      expect(isExactPerson(ELODIE, 'Martin')).toBeFalse();
      expect(isExactPerson(ELODIE, 'Élodie Martine')).toBeFalse();
      expect(isExactPerson(ELODIE, '   ')).toBeFalse();
      expect(isExactPerson(ELODIE, null)).toBeFalse();
    });
  });

  describe('findExactPerson', () => {
    let directory: jasmine.SpyObj<DirectoryService>;

    beforeEach(() => {
      directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    });

    it('cherche le nom et ne retient que la correspondance exacte', () => {
      directory.search.and.returnValue(of(page([AMINA, ELODIE])));
      let found: DirectoryUser | null | undefined;
      findExactPerson(directory, ' Élodie Martin ').subscribe(u => (found = u));
      expect(directory.search).toHaveBeenCalledWith('Élodie Martin');
      expect(found).toEqual(ELODIE);
    });

    it('ne propose personne quand rien ne correspond exactement', () => {
      directory.search.and.returnValue(of(page([AMINA])));
      let found: DirectoryUser | null | undefined;
      findExactPerson(directory, 'Amina').subscribe(u => (found = u));
      expect(found).toBeNull();
    });

    it('ne choisit pas entre deux homonymes, ni un ancien membre', () => {
      const autre = person({ accountId: 'kc-amina-2', displayName: 'Amina Dridi', email: 'a.dridi@client.test' });
      directory.search.and.returnValue(of(page([AMINA, autre])));
      let found: DirectoryUser | null | undefined;
      findExactPerson(directory, 'Amina Dridi').subscribe(u => (found = u));
      expect(found).toBeNull();

      directory.search.and.returnValue(of(page([{ ...AMINA, active: false }])));
      findExactPerson(directory, 'Amina Dridi').subscribe(u => (found = u));
      expect(found).toBeNull();
    });

    it('sans nom ou sans annuaire, ne propose personne et n\'échoue pas', () => {
      let found: DirectoryUser | null | undefined;
      findExactPerson(directory, '  ').subscribe(u => (found = u));
      expect(found).toBeNull();
      expect(directory.search).not.toHaveBeenCalled();

      directory.search.and.returnValue(throwError(() => ({ status: 0 })));
      found = undefined;
      findExactPerson(directory, 'Amina Dridi').subscribe(u => (found = u));
      expect(found).toBeNull();
    });
  });

  describe('AccountNames', () => {
    it('lit chaque compte une fois et rend son nom', () => {
      const directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['getByAccount']);
      directory.getByAccount.and.callFake(id => id === 'kc-amina' ? of(AMINA) : throwError(() => ({ status: 404 })));
      const resolved = jasmine.createSpy('resolved');
      const names = new AccountNames(directory, resolved);

      names.resolve(['kc-amina', 'kc-amina', null, undefined, '', 'kc-parti']);
      names.resolve(['kc-amina', 'kc-parti']);

      expect(directory.getByAccount.calls.allArgs()).toEqual([['kc-amina'], ['kc-parti']]);
      expect(names.get('kc-amina')).toBe('Amina Dridi');
      expect(names.get('kc-parti')).toBeUndefined();
      expect(names.get(null)).toBeUndefined();
      expect(resolved).toHaveBeenCalledTimes(1);
    });
  });
});
