import { DragDropModule } from '@angular/cdk/drag-drop';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthzService } from '../../../../core/authz/authz.service';
import { RoleView } from '../../../../core/authz/authz.types';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { TenantUser } from '../../admin.types';
import { TenantTeamService } from '../../tenant-team.service';
import { RolesMatrixComponent, grouper, slug } from './roles-matrix.component';

describe('RolesMatrixComponent', () => {

  let fixture: ComponentFixture<RolesMatrixComponent>;
  let component: RolesMatrixComponent;
  let authz: jasmine.SpyObj<AuthzService>;
  let team: jasmine.SpyObj<TenantTeamService>;
  let dialog: jasmine.SpyObj<MatDialog>;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const role = (o: Partial<RoleView>): RoleView => ({
    code: 'USER', name: null, description: null, system: true, customized: false, permissions: [], ...o
  });

  const ROLES: RoleView[] = [
    role({ code: 'ADMIN_TENANT', permissions: ['authz.manage', 'nc.close', 'nc.create'] }),
    role({ code: 'USER', permissions: ['nc.create'] }),
    role({ code: 'PILOTE', name: 'Pilote de site', system: false, customized: true, permissions: ['nc.close'] })
  ];

  const MARIE: TenantUser = {
    id: 'a1', tenantId: 't', keycloakId: 'kc-marie', email: 'marie.dupont@acme.fr', roles: [], active: true,
    createdAt: '', updatedAt: ''
  };
  const PAUL: TenantUser = { ...MARIE, id: 'a2', keycloakId: 'kc-paul', email: 'paul@acme.fr' };
  const INACTIF: TenantUser = { ...MARIE, id: 'a3', keycloakId: 'kc-x', email: 'x@acme.fr', active: false };

  async function setup(teamFails = false): Promise<void> {
    authz = jasmine.createSpyObj<AuthzService>('AuthzService',
      ['catalog', 'roles', 'members', 'updateRole', 'createRole', 'deleteRole', 'setMemberRoles', 'refresh', 'can']);
    authz.can.and.returnValue(of(true));
    authz.catalog.and.returnValue(of([
      { code: 'authz.manage', module: 'admin' }, { code: 'nc.create', module: 'nc' }, { code: 'nc.close', module: 'nc' }
    ]));
    authz.roles.and.returnValue(of(ROLES));
    authz.members.and.returnValue(of([{ userId: 'kc-marie', roles: ['PILOTE'] }]));
    team = jasmine.createSpyObj<TenantTeamService>('TenantTeamService', ['list']);
    team.list.and.returnValue(teamFails ? throwError(() => new Error('503'))
      : of({ content: [MARIE, PAUL, INACTIF], totalElements: 3, totalPages: 1, number: 0, size: 200 }));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);

    await TestBed.configureTestingModule({
      declarations: [RolesMatrixComponent],
      imports: [SharedModule, UiModule, DragDropModule, MatMenuModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: AuthzService, useValue: authz },
        { provide: TenantTeamService, useValue: team },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(RolesMatrixComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  const cellule = (role: string, perm: string): HTMLButtonElement =>
    hote().querySelector(`[data-test="cellule-${role}-${perm}"]`) as HTMLButtonElement;

  it('croise actions et rôles, regroupées par module, avec l’équipe active dans le vivier', async () => {
    await setup();

    expect(component.groups.map(g => g.module)).toEqual(['admin', 'nc']);
    expect(hote().querySelector('[data-test="role-PILOTE"]')!.textContent).toContain('Pilote de site');
    expect(hote().querySelector('[data-test="role-USER"]')!.textContent).toContain('Utilisateur');
    expect(cellule('USER', 'nc.create').getAttribute('aria-checked')).toBe('true');
    expect(cellule('USER', 'nc.close').getAttribute('aria-checked')).toBe('false');
    expect(hote().querySelectorAll('[data-test="membre"]').length).toBe(2);
    // Marie porte « Pilote » : sa pastille est dans la colonne.
    expect(hote().querySelector('[data-test="depot-PILOTE"]')!.textContent).toContain('MD');
    expect(component.grantedCount(ROLES[0])).toBe(3);
  });

  it('cocher prépare un changement, et l’enregistrer envoie le rôle modifié seulement', async () => {
    await setup();
    authz.updateRole.and.returnValue(of({ ...ROLES[1], customized: true, permissions: ['nc.close', 'nc.create'] }));

    cellule('USER', 'nc.close').click();
    fixture.detectChanges();
    expect(component.changedCount).toBe(1);
    expect(hote().querySelector('[data-test="barre"]')!.textContent).toContain('Rôles modifiés : 1');

    (hote().querySelector('[data-test="enregistrer"]') as HTMLButtonElement).click();
    expect(authz.updateRole).toHaveBeenCalledOnceWith('USER',
      { name: null, description: null, permissions: ['nc.close', 'nc.create'] });
    expect(authz.refresh).toHaveBeenCalled();
    fixture.detectChanges();
    expect(component.changedCount).toBe(0);
    expect(hote().querySelector('[data-test="barre"]')).toBeNull();
  });

  it('décocher puis recocher ne laisse aucun changement ; annuler efface tout', async () => {
    await setup();
    component.toggle(ROLES[1], 'nc.create');
    component.toggle(ROLES[1], 'nc.create');
    expect(component.changedCount).toBe(0);

    component.toggle(ROLES[2], 'nc.create');
    expect(component.isChanged(ROLES[2])).toBeTrue();
    component.discard();
    expect(component.changedCount).toBe(0);
    component.save();
    expect(authz.updateRole).not.toHaveBeenCalled();
  });

  it('l’administrateur ne perd jamais le droit d’administrer', async () => {
    await setup();
    expect(cellule('ADMIN_TENANT', 'authz.manage').disabled).toBeTrue();
    component.toggle(ROLES[0], 'authz.manage');
    expect(component.changedCount).toBe(0);
    expect(component.isLocked(ROLES[0], 'nc.create')).toBeFalse();
  });

  it('un échec d’enregistrement garde les changements', async () => {
    await setup();
    authz.updateRole.and.returnValue(throwError(() => new Error('422')));
    component.toggle(ROLES[1], 'nc.close');
    component.save();
    expect(component.changedCount).toBe(1);
    expect(component.saving).toBeFalse();
  });

  it('glisser un membre sur un rôle le lui donne ; la croix le retire', async () => {
    await setup();
    authz.setMemberRoles.and.callFake((id: string, roles: string[]) => of({ userId: id, roles }));

    component.dropOnRole({ item: { data: PAUL }, container: { data: 'USER' } } as never);
    expect(authz.setMemberRoles).toHaveBeenCalledWith('kc-paul', ['USER']);
    expect(component.rolesOf(PAUL)).toEqual(['USER']);

    // Déjà porté : rien ne part.
    authz.setMemberRoles.calls.reset();
    component.assign(MARIE, 'PILOTE');
    expect(authz.setMemberRoles).not.toHaveBeenCalled();

    component.unassign(MARIE, 'PILOTE');
    expect(authz.setMemberRoles).toHaveBeenCalledWith('kc-marie', []);
    expect(component.membersOf(ROLES[2])).toEqual([]);

    // Un dépôt sans données ne fait rien.
    authz.setMemberRoles.calls.reset();
    component.dropOnRole({ item: { data: null }, container: { data: 'USER' } } as never);
    expect(authz.setMemberRoles).not.toHaveBeenCalled();
    expect(component.neverEnter()).toBeFalse();
  });

  it('créer un rôle : le code se déduit du nom tant qu’on ne l’a pas écrit', async () => {
    await setup();
    authz.createRole.and.returnValue(of(role({ code: 'PILOTE_DE_SITE', name: 'Pilote de site', system: false, customized: true })));

    component.openCreate();
    component.createForm.controls.name.setValue('Pilote de site');
    component.suggestCode();
    expect(component.createForm.controls.code.value).toBe('PILOTE_DE_SITE');

    component.createRole();
    expect(authz.createRole).toHaveBeenCalledWith({
      code: 'PILOTE_DE_SITE', name: 'Pilote de site', description: null, permissions: []
    });
    expect(component.roles.map(r => r.code)).toContain('PILOTE_DE_SITE');
    expect(component.creating).toBeFalse();

    component.openCreate();
    component.createRole();
    expect(authz.createRole).toHaveBeenCalledTimes(1);
  });

  it('rétablir un rôle réglé ou supprimer un rôle sur mesure passe par une confirmation', async () => {
    await setup();
    authz.deleteRole.and.returnValue(of(undefined));
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as MatDialogRef<unknown>);

    component.removeOrReset(ROLES[2]);
    expect(authz.deleteRole).toHaveBeenCalledWith('PILOTE');
    expect(authz.roles).toHaveBeenCalledTimes(2);

    dialog.open.and.returnValue({ afterClosed: () => of(false) } as MatDialogRef<unknown>);
    component.removeOrReset(ROLES[1]);
    expect(authz.deleteRole).toHaveBeenCalledTimes(1);
  });

  it('sans annuaire de l’équipe, la matrice reste utilisable', async () => {
    await setup(true);
    expect(component.teamFailed).toBeTrue();
    expect(hote().querySelector('[data-test="equipe-indisponible"]')).not.toBeNull();
    expect(cellule('USER', 'nc.create')).not.toBeNull();
  });

  it('outils : regroupement, code dérivé, initiales', async () => {
    await setup();
    expect(grouper([{ code: 'a.x', module: 'a' }, { code: 'b.x', module: 'b' }, { code: 'a.y', module: 'a' }]))
      .toEqual([{ module: 'a', permissions: ['a.x', 'a.y'] }, { module: 'b', permissions: ['b.x'] }]);
    expect(slug('Pilote été — Site 2')).toBe('PILOTE_ETE_SITE_2');
    expect(slug('2e équipe')).toBe('R_2E_EQUIPE');
    expect(slug('!!')).toBe('');
    expect(component.initials(MARIE)).toBe('MD');
    expect(component.initials(PAUL)).toBe('PA');
    expect(component.labelOf('INCONNU')).toBe('INCONNU');
    expect(component.labelRetrait(MARIE, ROLES[2])).toContain('marie.dupont@acme.fr');
  });
});
