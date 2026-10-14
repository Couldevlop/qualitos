import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, delay, map, shareReplay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { TenantTeamService } from '../admin/tenant-team.service';
import { AuthService } from '../../core/auth/auth.service';

/**
 * Un membre de l'organisation que l'on peut désigner (ADR 0088).
 *
 * `userId` est l'identifiant du COMPTE DE CONNEXION (`sub` du jeton,
 * `keycloakId` dans l'annuaire) : c'est lui que les modules rapprochent de
 * l'utilisateur courant pour lui montrer « ses » dossiers (ADR 0081).
 * L'identifiant interne de l'annuaire l'aurait privé de les voir.
 */
export interface DirectoryMember {
  userId: string;
  /** Ce qui s'affiche, et ce qui est recopié dans le dossier. */
  label: string;
}

export interface DirectoryChoices {
  /** L'utilisateur courant d'abord, puis l'équipe par ordre alphabétique. */
  members: DirectoryMember[];
  /**
   * Faux quand l'annuaire n'a pas répondu — il n'est lisible que des
   * administrateurs, ou le réseau a manqué. Seul « moi » est alors proposé,
   * et l'écran le dit au lieu de montrer une liste trompeusement courte.
   */
  available: boolean;
}

/**
 * L'annuaire du client, réduit à ce qu'il faut pour désigner quelqu'un —
 * responsable d'une CAPA, vérificateur. Même source que l'équipe
 * (`/api/v1/users`), le client venant du jeton côté serveur (§18.2).
 *
 * Lu une fois par session puis partagé : la fenêtre de création CAPA s'ouvre
 * souvent et l'équipe ne change pas d'une fenêtre à l'autre. Un refus, lui,
 * n'est pas une réponse définitive : il n'est pas conservé.
 */
@Injectable({ providedIn: 'root' })
export class CapaDirectoryService {

  private choices$?: Observable<DirectoryChoices>;

  constructor(
    private readonly team: TenantTeamService,
    private readonly auth: AuthService
  ) {}

  choices(): Observable<DirectoryChoices> {
    if (!this.choices$) {
      const me = this.currentMember();
      const self = me ? [me] : [];
      const source = environment.useMockApi
        ? of(DEMO_TEAM).pipe(delay(80))
        : this.team.list(0, 200).pipe(map(page => page.content
            .filter(m => m.active && !!m.keycloakId)
            .map(m => ({ userId: m.keycloakId, label: m.email }))));
      this.choices$ = source.pipe(
        map(team => ({
          members: [
            ...self,
            ...team
              .filter(m => m.userId !== me?.userId)
              .sort((a, b) => a.label.localeCompare(b.label))
          ],
          available: true
        })),
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.choices$.pipe(
      catchError(() => {
        // Le prochain appel retente : un refus de droits peut être levé en cours de session.
        this.choices$ = undefined;
        const me = this.currentMember();
        return of({ members: me ? [me] : [], available: false });
      })
    );
  }

  /** L'utilisateur courant, désigné par son nom de session. */
  currentMember(): DirectoryMember | null {
    const me = this.auth.snapshot();
    if (!me?.userId) return null;
    return { userId: me.userId, label: me.displayName || me.userId };
  }
}

/** L'équipe fictive du mode démonstration. */
const DEMO_TEAM: DirectoryMember[] = [
  { userId: '00000000-0000-0000-0000-0000000000a1', label: 'amina.dridi@demo.qualitos.io' },
  { userId: '00000000-0000-0000-0000-0000000000a2', label: 'karim.benali@demo.qualitos.io' },
  { userId: '00000000-0000-0000-0000-0000000000a3', label: 'marc.lefevre@demo.qualitos.io' }
];

/**
 * Le membre que désigne un nom saisi ailleurs en clair — le propriétaire d'un
 * risque, par exemple —, ou `undefined`.
 *
 * Rapprochement STRICT : « A. Diallo » trouve « a.diallo@… », « Amina Diallo »
 * trouve « amina.diallo@… » ou le compte dont le nom de session est « Amina
 * Diallo », sans accents, casse, espaces ni points. Rien d'approchant :
 * désigner la mauvaise personne serait pire que ne rien proposer.
 */
export function matchMember(members: DirectoryMember[], hint: string | null | undefined): DirectoryMember | undefined {
  const cle = normaliser(hint ?? '');
  if (!cle) return undefined;
  return members.find(m => {
    const label = m.label.toLowerCase();
    const local = label.split('@')[0];
    return normaliser(label) === cle || normaliser(local) === cle;
  });
}

function normaliser(texte: string): string {
  return texte
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[\s._-]+/g, '');
}
