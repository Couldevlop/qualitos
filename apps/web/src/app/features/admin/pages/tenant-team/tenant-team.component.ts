import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ASSIGNABLE_ROLES, TenantUser } from '../../admin.types';
import { TenantTeamService } from '../../tenant-team.service';

/**
 * Équipe & habilitations du tenant (§16).
 *
 * L'administrateur du tenant compose ici son équipe : qui a accès, avec quels
 * rôles. Jusqu'à cet écran, la table des utilisateurs n'était pilotable que par
 * appel HTTP — et l'API elle-même était fermée à l'administrateur de tenant.
 *
 * L'autorisation réelle reste au serveur : cet écran ne fait que rendre visible
 * ce que l'API accepte déjà, et n'affiche jamais `super_admin`, qui appartient à
 * l'éditeur de la plateforme.
 */
@Component({
  selector: 'qos-tenant-team',
  templateUrl: './tenant-team.component.html',
  styleUrls: ['./tenant-team.component.scss'],
  standalone: false
})
export class TenantTeamComponent implements OnInit {

  readonly assignableRoles = ASSIGNABLE_ROLES;
  readonly columns = ['email', 'roles', 'status', 'actions'];

  members: TenantUser[] = [];
  loading = false;
  /** Identifiant du membre dont une modification est en vol (désactive sa ligne). */
  pendingUserId: string | null = null;
  search = '';

  constructor(
    private readonly service: TenantTeamService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading = true;
    this.service.list().subscribe({
      next: page => {
        this.members = page.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snack.open(
          $localize`:@@admin.team.load-failed:Impossible de charger l'équipe.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  onSearch(event: Event): void {
    this.search = (event.target as HTMLInputElement).value;
  }

  /** Filtre local : la liste d'une organisation tient dans une page. */
  get visibleMembers(): TenantUser[] {
    const needle = this.search.trim().toLowerCase();
    if (!needle) {
      return this.members;
    }
    return this.members.filter(m =>
      m.email.toLowerCase().includes(needle)
      || m.roles.some(r => r.toLowerCase().includes(needle)));
  }

  hasRole(member: TenantUser, role: string): boolean {
    return member.roles.includes(role);
  }

  /**
   * Ajoute ou retire un rôle. L'API attend l'ensemble complet : on recompose la
   * liste plutôt que d'envoyer un écart, et l'on refuse de laisser un membre sans
   * aucun rôle — un compte sans rôle est un compte qui ouvre une session pour
   * rencontrer un refus à chaque écran.
   */
  toggleRole(member: TenantUser, role: string): void {
    const roles = this.hasRole(member, role)
      ? member.roles.filter(r => r !== role)
      : [...member.roles, role];
    if (roles.length === 0) {
      this.snack.open(
        $localize`:@@admin.team.needs-a-role:Un membre doit conserver au moins un rôle.`,
        $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.apply(member, roles, member.active);
  }

  setActive(member: TenantUser, active: boolean): void {
    this.apply(member, member.roles, active);
  }

  private apply(member: TenantUser, roles: string[], active: boolean): void {
    this.pendingUserId = member.id;
    this.service.setRoles(member.id, roles, active).subscribe({
      next: updated => {
        this.members = this.members.map(m => (m.id === updated.id ? updated : m));
        this.pendingUserId = null;
        this.snack.open(
          $localize`:@@admin.team.saved:Habilitations mises à jour.`,
          $localize`:@@common.ok:OK`, { duration: 2500 });
      },
      error: () => {
        this.pendingUserId = null;
        this.snack.open(
          $localize`:@@admin.team.save-failed:Modification refusée.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  /** Libellé lisible d'un rôle technique. */
  roleLabel(role: string): string {
    return this.roleLabels[role] ?? role;
  }

  private readonly roleLabels: Record<string, string> = {
    admin_tenant: $localize`:@@role.admin-tenant:Administrateur du tenant`,
    quality_director: $localize`:@@role.quality-director:Directeur qualité`,
    quality_manager: $localize`:@@role.quality-manager:Manager qualité`,
    auditor: $localize`:@@role.auditor:Auditeur`,
    user: $localize`:@@role.user:Utilisateur`,
    external_auditor: $localize`:@@role.external-auditor:Auditeur externe`,
    super_admin: $localize`:@@role.super-admin:Super administrateur`
  };
}
