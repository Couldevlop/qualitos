import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { AuthService, AuthUser } from '../../core/auth/auth.service';
import { TenantTeamService } from '../admin/tenant-team.service';
import { AuditPlanResponse } from './audits.types';

/**
 * Un auditeur proposable comme responsable d'un audit.
 *
 * `id` est l'identifiant du COMPTE DE CONNEXION (le `sub` du jeton, `keycloakId`
 * dans l'annuaire) : c'est lui que l'audit a toujours porté, puisque l'écran y
 * recopiait l'utilisateur connecté. Prendre l'identifiant interne de l'annuaire
 * aurait fait de chaque audit existant l'audit de « quelqu'un d'autre ».
 */
export interface LeadAuditorOption {
  id: string;
  label: string;
}

export interface LeadAuditorChoices {
  options: LeadAuditorOption[];
  /** false : l'annuaire n'a pas répondu (droits ou réseau) — seul « moi » est proposé. */
  directoryAvailable: boolean;
}

/**
 * Le nom à montrer pour l'auditeur responsable d'un audit — jamais un identifiant.
 *
 * Ordre de lecture : le nom recopié à la désignation ; à défaut, si l'auditeur
 * est l'utilisateur connecté, son nom de session ; à défaut, un libellé neutre.
 * Les audits créés avant la recopie du nom n'en portent pas, et afficher leur
 * identifiant brut était précisément ce qui rendait le champ incompréhensible.
 */
export function leadAuditorDisplay(
  plan: Pick<AuditPlanResponse, 'leadAuditorId' | 'leadAuditorName'>,
  me: AuthUser | null
): string {
  if (plan.leadAuditorName) return plan.leadAuditorName;
  if (me && plan.leadAuditorId && plan.leadAuditorId === me.userId) return me.displayName;
  return $localize`:@@audits.lead-auditor.unknown:Membre de l'organisation (nom non enregistré)`;
}

/**
 * L'annuaire de l'organisation, réduit à ce qu'il faut pour désigner l'auditeur
 * responsable. Même source que le vérificateur d'une CAPA (`/api/v1/users`).
 *
 * L'utilisateur connecté est toujours proposé, en tête : c'est la valeur par
 * défaut, et l'annuaire n'est pas lisible par tous les rôles. Sans lui, un
 * manager qualité sans droit d'administration ne pourrait plus créer d'audit.
 */
@Injectable({ providedIn: 'root' })
export class LeadAuditorDirectory {

  constructor(
    private readonly team: TenantTeamService,
    private readonly auth: AuthService
  ) {}

  choices(): Observable<LeadAuditorChoices> {
    const me = this.auth.snapshot();
    const self: LeadAuditorOption[] = me?.userId
      ? [{ id: me.userId, label: $localize`:@@audits.lead-auditor.me:${me.displayName}:name: (vous)` }]
      : [];
    return this.team.list(0, 200).pipe(
      map(page => ({
        options: [
          ...self,
          ...page.content
            .filter(m => m.active && m.keycloakId && m.keycloakId !== me?.userId)
            .map(m => ({ id: m.keycloakId, label: m.email }))
            .sort((a, b) => a.label.localeCompare(b.label))
        ],
        directoryAvailable: true
      })),
      catchError(() => of({ options: self, directoryAvailable: false }))
    );
  }

  /** Le nom à recopier pour l'option choisie (sans la mention « (vous) »). */
  nameFor(option: LeadAuditorOption | undefined): string | undefined {
    if (!option) return undefined;
    const me = this.auth.snapshot();
    return me && option.id === me.userId ? me.displayName : option.label;
  }
}
