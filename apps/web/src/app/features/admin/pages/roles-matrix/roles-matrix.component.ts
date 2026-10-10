import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { moduleLabel, permissionLabel, roleLabel } from '../../../../core/authz/authz.labels';
import { AuthzService } from '../../../../core/authz/authz.service';
import { CatalogEntry, RoleView } from '../../../../core/authz/authz.types';
import {
  ConfirmDialogComponent, ConfirmDialogData
} from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { TenantUser } from '../../admin.types';
import { TenantTeamService } from '../../tenant-team.service';

/** Les actions d'un module, dans l'ordre du catalogue. */
export interface ModuleGroup {
  module: string;
  permissions: string[];
}

/** Le seul couple qu'on ne peut pas décocher : sans lui, plus personne n'administrerait les droits. */
const LOCKED = { role: 'ADMIN_TENANT', permission: 'authz.manage' };

/**
 * Rôles et droits : la matrice des actions par rôle, et l'équipe qu'on y glisse.
 *
 * <p>Chaque colonne est un rôle — ceux de la plateforme, puis ceux du client —,
 * chaque ligne une action. Cocher ou décocher prépare un changement ; rien ne
 * part tant qu'on n'a pas enregistré, et la barre du bas dit combien de rôles
 * changent. Glisser un membre de l'équipe sur une colonne lui donne ce rôle,
 * tout de suite ; le menu de sa carte fait la même chose au clavier.
 *
 * <p>Les rôles attribués ici s'ajoutent à ceux du compte du membre : un rôle
 * porté par son compte ne se retire pas depuis cet écran.
 */
@Component({
  selector: 'qos-roles-matrix',
  templateUrl: './roles-matrix.component.html',
  styleUrls: ['./roles-matrix.component.scss'],
  standalone: false
})
export class RolesMatrixComponent implements OnInit {

  groups: ModuleGroup[] = [];
  roles: RoleView[] = [];
  team: TenantUser[] = [];
  /** Rôles attribués dans l'application, par identifiant de compte (sujet du jeton). */
  assigned = new Map<string, string[]>();
  /** Les droits en cours d'édition, par rôle modifié. */
  private drafts = new Map<string, Set<string>>();

  loading = false;
  failed = false;
  teamFailed = false;
  saving = false;
  creating = false;

  readonly createForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    code: ['', [Validators.required, Validators.pattern(/^[A-Za-z][A-Za-z0-9_]{1,63}$/)]],
    description: ['', Validators.maxLength(500)]
  });

  constructor(
    private readonly authz: AuthzService,
    private readonly teamService: TenantTeamService,
    private readonly fb: FormBuilder,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    forkJoin({
      catalog: this.authz.catalog(),
      roles: this.authz.roles(),
      members: this.authz.members(),
      // L'équipe vit dans un autre service : si elle ne répond pas, la matrice
      // reste utilisable, seul le vivier de membres manque.
      team: this.teamService.list(0, 200).pipe(catchError(() => {
        this.teamFailed = true;
        return of(null);
      }))
    }).pipe(finalize(() => this.loading = false)).subscribe({
      next: r => {
        this.groups = grouper(r.catalog);
        this.roles = r.roles;
        this.assigned = new Map(r.members.map(m => [m.userId, m.roles]));
        if (r.team) {
          this.teamFailed = false;
          this.team = r.team.content.filter(u => u.active);
        }
        this.drafts.clear();
      },
      error: err => {
        this.failed = true;
        this.echouer(err);
      }
    });
  }

  // ---------- matrice ----------

  isGranted(role: RoleView, permission: string): boolean {
    const brouillon = this.drafts.get(role.code);
    return brouillon ? brouillon.has(permission) : role.permissions.includes(permission);
  }

  isLocked(role: RoleView, permission: string): boolean {
    return role.code === LOCKED.role && permission === LOCKED.permission;
  }

  toggle(role: RoleView, permission: string): void {
    if (this.isLocked(role, permission) || this.saving) return;
    const droits = new Set(this.drafts.get(role.code) ?? role.permissions);
    if (droits.has(permission)) {
      droits.delete(permission);
    } else {
      droits.add(permission);
    }
    if (sameSet(droits, role.permissions)) {
      this.drafts.delete(role.code);
    } else {
      this.drafts.set(role.code, droits);
    }
  }

  isChanged(role: RoleView): boolean {
    return this.drafts.has(role.code);
  }

  get changedCount(): number {
    return this.drafts.size;
  }

  discard(): void {
    this.drafts.clear();
  }

  save(): void {
    if (!this.drafts.size || this.saving) return;
    this.saving = true;
    const envois = [...this.drafts.entries()].map(([code, droits]) => {
      const role = this.roles.find(r => r.code === code)!;
      return this.authz.updateRole(code, {
        name: role.name, description: role.description, permissions: [...droits].sort()
      });
    });
    forkJoin(envois).pipe(finalize(() => this.saving = false)).subscribe({
      next: maj => {
        this.roles = this.roles.map(r => maj.find(m => m.code === r.code) ?? r);
        this.drafts.clear();
        this.authz.refresh();
        this.snack.open($localize`:@@authz.matrix.saved:Droits enregistrés.`, undefined, { duration: 2500 });
      },
      error: err => this.echouer(err)
    });
  }

  /** Rend à un rôle de la plateforme ses droits livrés, ou supprime un rôle du client. */
  removeOrReset(role: RoleView): void {
    const data: ConfirmDialogData = role.system
      ? {
        title: $localize`:@@authz.matrix.reset-title:Rendre au rôle ${roleLabel(role.code, role.name)}:role: ses droits d'origine ?`,
        message: $localize`:@@authz.matrix.reset-message:Les droits livrés par la plateforme remplacent vos réglages. Ses membres le gardent.`,
        confirmLabel: $localize`:@@authz.matrix.reset:Rétablir`
      }
      : {
        title: $localize`:@@authz.matrix.delete-title:Supprimer le rôle ${roleLabel(role.code, role.name)}:role: ?`,
        message: $localize`:@@authz.matrix.delete-message:Ses membres perdent les droits qu'il leur donnait. La suppression est tracée dans le journal d'audit.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`
      };
    this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      data, panelClass: 'qos-dialog-panel'
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.authz.deleteRole(role.code).subscribe({
        next: () => {
          this.authz.refresh();
          this.charger();
        },
        error: err => this.echouer(err)
      });
    });
  }

  // ---------- nouveau rôle ----------

  openCreate(): void {
    this.creating = true;
    this.createForm.reset();
  }

  /** Le code se déduit du nom tant qu'on ne l'a pas écrit soi-même. */
  suggestCode(): void {
    const code = this.createForm.controls.code;
    if (code.dirty) return;
    code.setValue(slug(this.createForm.controls.name.value));
  }

  createRole(): void {
    if (this.createForm.invalid || this.saving) {
      this.createForm.markAllAsTouched();
      return;
    }
    const v = this.createForm.getRawValue();
    this.saving = true;
    this.authz.createRole({
      code: v.code.trim().toUpperCase(), name: v.name.trim(), description: v.description.trim() || null, permissions: []
    }).pipe(finalize(() => this.saving = false)).subscribe({
      next: role => {
        this.roles = [...this.roles, role];
        this.creating = false;
        this.snack.open($localize`:@@authz.matrix.created:Rôle créé : cochez maintenant ses droits.`, undefined, { duration: 3000 });
      },
      error: err => this.echouer(err)
    });
  }

  // ---------- équipe ----------

  rolesOf(user: TenantUser): string[] {
    return this.assigned.get(user.keycloakId) ?? [];
  }

  membersOf(role: RoleView): TenantUser[] {
    return this.team.filter(u => this.rolesOf(u).includes(role.code));
  }

  /** Dépôt d'une carte de membre sur une colonne de rôle. */
  dropOnRole(event: CdkDragDrop<string, unknown, TenantUser>): void {
    const user = event.item.data;
    const code = event.container.data;
    if (user && code) this.assign(user, code);
  }

  assign(user: TenantUser, code: string): void {
    const actuels = this.rolesOf(user);
    if (actuels.includes(code)) return;
    this.setRoles(user, [...actuels, code],
      $localize`:@@authz.matrix.assigned:${user.email}:member: a désormais le rôle ${this.labelOf(code)}:role:.`);
  }

  unassign(user: TenantUser, code: string): void {
    this.setRoles(user, this.rolesOf(user).filter(c => c !== code),
      $localize`:@@authz.matrix.unassigned:${user.email}:member: n'a plus le rôle ${this.labelOf(code)}:role:.`);
  }

  private setRoles(user: TenantUser, codes: string[], message: string): void {
    this.authz.setMemberRoles(user.keycloakId, codes).subscribe({
      next: m => {
        this.assigned = new Map(this.assigned).set(m.userId, m.roles);
        this.snack.open(message, undefined, { duration: 2500 });
      },
      error: err => this.echouer(err)
    });
  }

  // ---------- gabarit ----------

  labelOf(code: string): string {
    const role = this.roles.find(r => r.code === code);
    return roleLabel(code, role?.name);
  }

  roleText(role: RoleView): string {
    return roleLabel(role.code, role.name);
  }

  /** Une teinte par rôle, stable dans la page : elle relie la colonne aux pastilles de ses membres. */
  hue(role: RoleView): number {
    return this.roles.indexOf(role) % 6;
  }

  initials(user: TenantUser): string {
    const local = user.email.split('@')[0] ?? '';
    const parts = local.split(/[._-]+/).filter(Boolean);
    return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? parts[0]?.[1] ?? '')).toUpperCase() || '?';
  }

  grantedCount(role: RoleView): number {
    return this.groups.reduce((n, g) => n + g.permissions.filter(p => this.isGranted(role, p)).length, 0);
  }

  readonly lockedText =
    $localize`:@@authz.matrix.locked:L'administrateur garde toujours ce droit : sans lui, plus personne ne pourrait rétablir les autres.`;

  /** Le vivier est une source : on n'y dépose rien. */
  readonly neverEnter = (): boolean => false;

  labelRetrait(user: TenantUser, role: RoleView): string {
    return $localize`:@@authz.matrix.unassign-aria:Retirer le rôle ${this.roleText(role)}:role: à ${user.email}:member:`;
  }

  permissionText = permissionLabel;
  moduleText = moduleLabel;

  trackByCode(_i: number, r: { code: string }): string {
    return r.code;
  }

  trackByModule(_i: number, g: ModuleGroup): string {
    return g.module;
  }

  trackByPermission(_i: number, p: string): string {
    return p;
  }

  trackByUser(_i: number, u: TenantUser): string {
    return u.id;
  }

  private echouer(err: unknown): void {
    this.snack.open(safeErrorMessage(err, $localize`:@@authz.matrix.failed:L'opération sur les droits a échoué.`),
      undefined, { duration: 5000 });
  }
}

export function grouper(catalog: CatalogEntry[]): ModuleGroup[] {
  const groupes: ModuleGroup[] = [];
  for (const e of catalog) {
    let g = groupes.find(x => x.module === e.module);
    if (!g) {
      g = { module: e.module, permissions: [] };
      groupes.push(g);
    }
    g.permissions.push(e.code);
  }
  return groupes;
}

function sameSet(a: Set<string>, b: string[]): boolean {
  return a.size === b.length && b.every(x => a.has(x));
}

/** « Pilote de site » → « PILOTE_DE_SITE » : un code valable, sans accent. */
export function slug(name: string): string {
  const s = name.normalize('NFD').replace(/[̀-ͯ]/g, '')
    .toUpperCase().replace(/[^A-Z0-9]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 64);
  return /^[A-Z]/.test(s) ? s : (s ? 'R_' + s : '');
}
