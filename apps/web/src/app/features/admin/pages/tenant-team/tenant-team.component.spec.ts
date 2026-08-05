import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { TenantUser } from '../../admin.types';
import { TenantTeamService } from '../../tenant-team.service';
import { TenantTeamComponent } from './tenant-team.component';

describe('TenantTeamComponent', () => {
  let component: TenantTeamComponent;
  let fixture: ComponentFixture<TenantTeamComponent>;
  let service: jasmine.SpyObj<TenantTeamService>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const user = (over: Partial<TenantUser> = {}): TenantUser => ({
    id: 'u1', tenantId: 't1', keycloakId: 'kc-1', email: 'alice@acme.com',
    roles: ['quality_manager'], active: true,
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', ...over
  });

  const page = (members: TenantUser[]) => ({
    content: members, totalElements: members.length, totalPages: 1, number: 0, size: 50
  });

  beforeEach(async () => {
    service = jasmine.createSpyObj<TenantTeamService>(
      'TenantTeamService', ['list', 'setRoles', 'deactivate']);
    service.list.and.returnValue(of(page([user(), user({
      id: 'u2', email: 'bob@acme.com', roles: ['auditor'], active: false
    })])));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [TenantTeamComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: TenantTeamService, useValue: service },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TenantTeamComponent);
    component = fixture.componentInstance;
  });

  it('charge l’équipe du tenant au démarrage', () => {
    fixture.detectChanges();
    expect(component.members.length).toBe(2);
  });

  it('n’offre jamais le rôle super_admin, qui appartient à l’éditeur', () => {
    // Un administrateur de tenant qui pourrait se l'attribuer sortirait de son
    // périmètre : c'est une élévation de privilège offerte par l'interface.
    expect(component.assignableRoles as readonly string[]).not.toContain('super_admin');
  });

  it('accorde un rôle en envoyant l’ensemble complet', () => {
    service.setRoles.and.returnValue(of(user({ roles: ['quality_manager', 'auditor'] })));
    fixture.detectChanges();

    component.toggleRole(component.members[0], 'auditor');

    expect(service.setRoles).toHaveBeenCalledWith('u1', ['quality_manager', 'auditor'], true);
    expect(component.members[0].roles).toEqual(['quality_manager', 'auditor']);
  });

  it('retire un rôle déjà accordé', () => {
    service.setRoles.and.returnValue(of(user({ roles: ['auditor'] })));
    fixture.detectChanges();
    const member = user({ roles: ['quality_manager', 'auditor'] });
    component.members = [member];

    component.toggleRole(member, 'quality_manager');

    expect(service.setRoles).toHaveBeenCalledWith('u1', ['auditor'], true);
  });

  it('refuse de laisser un membre sans aucun rôle', () => {
    // Un compte sans rôle ouvre une session pour se heurter à un refus partout :
    // mieux vaut retirer l'accès explicitement.
    fixture.detectChanges();

    component.toggleRole(component.members[0], 'quality_manager');

    expect(service.setRoles).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
  });

  it('retire l’accès sans toucher aux rôles', () => {
    service.setRoles.and.returnValue(of(user({ active: false })));
    fixture.detectChanges();

    component.setActive(component.members[0], false);

    expect(service.setRoles).toHaveBeenCalledWith('u1', ['quality_manager'], false);
  });

  it('filtre sur l’adresse comme sur le rôle', () => {
    fixture.detectChanges();

    component.search = 'bob';
    expect(component.visibleMembers.map(m => m.id)).toEqual(['u2']);

    component.search = 'auditor';
    expect(component.visibleMembers.map(m => m.id)).toEqual(['u2']);
  });

  it('signale un refus du serveur sans modifier l’affichage', () => {
    service.setRoles.and.returnValue(throwError(() => new Error('403')));
    fixture.detectChanges();

    component.toggleRole(component.members[0], 'auditor');

    expect(component.members[0].roles).toEqual(['quality_manager']);
    expect(snack.open).toHaveBeenCalled();
    expect(component.pendingUserId).toBeNull();
  });

  it('signale l’échec de chargement', () => {
    service.list.and.returnValue(throwError(() => new Error('500')));

    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(snack.open).toHaveBeenCalled();
  });
});
