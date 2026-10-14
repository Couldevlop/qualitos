import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { DirectoryService, normalizeSearch } from './directory.service';
import { DirectoryUser } from './directory.types';

/**
 * Le nom à recopier dans un dossier pour une personne de l'annuaire : son nom
 * affiché, et l'adresse seulement quand aucun nom n'est enregistré.
 */
export function personName(user: Pick<DirectoryUser, 'displayName' | 'email'>): string {
  return user.displayName?.trim() || user.email;
}

/**
 * Vrai quand `hint` désigne EXACTEMENT cette personne : son nom affiché ou son
 * adresse, accents, casse et espaces ignorés. Rien d'approchant : désigner la
 * mauvaise personne serait pire que ne rien proposer.
 */
export function isExactPerson(user: Pick<DirectoryUser, 'displayName' | 'email'>, hint: string | null | undefined): boolean {
  const cle = normalizeSearch(hint ?? '');
  if (!cle) return false;
  return normalizeSearch(user.displayName ?? '') === cle || normalizeSearch(user.email ?? '') === cle;
}

/**
 * La personne de l'annuaire que désigne un nom écrit en clair ailleurs — le
 * propriétaire d'un risque —, ou `null`.
 *
 * <p>Une recherche, puis une correspondance EXACTE (nom ou adresse) parmi les
 * réponses ; plusieurs personnes du même nom : aucune n'est proposée. Un
 * annuaire indisponible ne propose personne, sans erreur.
 */
export function findExactPerson(directory: DirectoryService, hint: string | null | undefined): Observable<DirectoryUser | null> {
  const term = (hint ?? '').trim();
  if (!term) return of(null);
  return directory.search(term).pipe(
    map(page => {
      const exact = page.content.filter(u => u.active && isExactPerson(u, term));
      return exact.length === 1 ? exact[0] : null;
    }),
    catchError(() => of(null))
  );
}

/**
 * Les noms des comptes cités par des dossiers anciens qui n'ont pas recopié le
 * nom de leur responsable, lus dans l'annuaire en lecture restreinte (ADR 0091).
 *
 * <p>`resolve` se lance quand les dossiers arrivent, `get` se lit dans le
 * gabarit : le gabarit ne déclenche jamais d'appel. Chaque compte n'est
 * demandé qu'une fois par écran ; un compte inconnu reste sans nom (l'écran
 * affiche alors son libellé neutre, jamais l'identifiant).
 */
export class AccountNames {

  private readonly names = new Map<string, string>();
  private readonly requested = new Set<string>();

  constructor(
    private readonly directory: DirectoryService,
    private readonly onResolved: () => void = () => undefined
  ) {}

  resolve(accountIds: Iterable<string | null | undefined>): void {
    for (const id of accountIds) {
      if (!id || this.requested.has(id)) continue;
      this.requested.add(id);
      this.directory.getByAccount(id).subscribe({
        next: user => {
          this.names.set(id, personName(user));
          this.onResolved();
        },
        error: () => undefined
      });
    }
  }

  get(accountId: string | null | undefined): string | undefined {
    return accountId ? this.names.get(accountId) : undefined;
  }
}
