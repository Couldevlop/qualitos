import { Provider } from '@angular/core';
import { of, throwError } from 'rxjs';

import { DirectoryService } from '../directory.service';
import { DirectoryUser } from '../directory.types';

/** Une personne de l'annuaire de test, connue par son compte. */
export const STUB_PERSON: DirectoryUser = {
  id: '00000000-0000-4000-8000-0000000000a1',
  accountId: '11111111-1111-1111-1111-111111111111',
  displayName: 'Amina Dridi',
  email: 'amina.dridi@client.test',
  active: true
};

/**
 * Un annuaire sans réseau pour les tests des écrans qui montrent ou désignent
 * une personne (ADR 0092) : il connaît `people`, et répond « introuvable »
 * pour tout autre compte — comme le serveur pour un compte d'un autre client.
 */
export function directoryStub(people: readonly DirectoryUser[] = [STUB_PERSON]): Partial<DirectoryService> {
  return {
    search: () => of({ content: [...people], page: 0, size: 20, totalElements: people.length, totalPages: 1 }),
    getByAccount: (accountId: string) => {
      const person = people.find(p => p.accountId === accountId);
      return person ? of(person) : throwError(() => ({ status: 404 }));
    }
  };
}

/** Le fournisseur à poser dans `TestBed` à la place de l'annuaire réel. */
export function provideDirectoryStub(people?: readonly DirectoryUser[]): Provider {
  return { provide: DirectoryService, useValue: directoryStub(people) };
}
