import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { TenantTeamService } from '../../../admin/tenant-team.service';
import { TenantUser } from '../../../admin/admin.types';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AuditsService } from '../../audits.service';
import { AuditPlanResponse } from '../../audits.types';
import { AuditsCreateDialogComponent } from './audits-create-dialog.component';

/**
 * Création d'un plan d'audit (§4.4).
 *
 * L'auditeur responsable se CHOISIT dans l'annuaire, l'utilisateur connecté
 * par défaut. Il était recopié en silence depuis la session, puis affiché comme
 * un identifiant que personne ne savait relire.
 */
describe('AuditsCreateDialogComponent', () => {
  let fixture: ComponentFixture<AuditsCreateDialogComponent>;
  let component: AuditsCreateDialogComponent;
  let svc: jasmine.SpyObj<AuditsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<AuditsCreateDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let session: AuthUser | null;
  let team: jasmine.SpyObj<TenantTeamService>;

  const membre = (over: Partial<TenantUser>): TenantUser => ({
    id: 'internal-' + (over.keycloakId ?? 'x'), tenantId: 't1', keycloakId: 'x', email: 'x@exemple.test',
    roles: ['auditor'], active: true, createdAt: '', updatedAt: '', ...over
  });

  const created = { id: 'a-9', title: 'Audit' } as AuditPlanResponse;

  beforeEach(async () => {
    session = { userId: 'u1', tenantId: 't1', displayName: 'Demo', roles: ['auditor'] };
    svc = jasmine.createSpyObj<AuditsService>('AuditsService', ['createPlan']);
    svc.createPlan.and.returnValue(of(created));
    dialogRef = jasmine.createSpyObj<MatDialogRef<AuditsCreateDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    team = jasmine.createSpyObj<TenantTeamService>('TenantTeamService', ['list']);
    team.list.and.returnValue(of({
      content: [
        membre({ keycloakId: 'u1', email: 'demo@exemple.test' }),
        membre({ keycloakId: 'u2', email: 'bruno@exemple.test' }),
        membre({ keycloakId: 'u3', email: 'parti@exemple.test', active: false })
      ],
      totalElements: 3, totalPages: 1, number: 0, size: 200
    }));

    await TestBed.configureTestingModule({
      declarations: [AuditsCreateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: AuditsService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => session } },
        { provide: TenantTeamService, useValue: team }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditsCreateDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ---- Ouverture ---------------------------------------------------------------

  it('propose les six natures d\'audit du référentiel, interne par défaut', () => {
    expect(component.types.map(t => t.value))
      .toEqual(['INTERNAL', 'EXTERNAL', 'SUPPLIER', 'LPA', 'CERTIFICATION', 'SURVEILLANCE']);
    expect(component.types.every(t => !!t.label)).toBeTrue();
    expect(component.form.getRawValue().type).toBe('INTERNAL');
  });

  it('s\'ouvre sur un formulaire vide et invalide', () => {
    expect(component.form.getRawValue().title).toBe('');
    expect(component.form.valid).toBeFalse();
    expect(component.submitting).toBeFalse();
  });

  // ---- Validation ---------------------------------------------------------------

  it('refuse un plan sans intitulé', () => {
    component.submit();

    expect(svc.createPlan).not.toHaveBeenCalled();
    expect(component.form.controls.title.touched).toBeTrue();
  });

  it('refuse un intitulé ou une norme au-delà des limites du serveur', () => {
    component.form.patchValue({ title: 'x'.repeat(256) });
    component.submit();
    expect(svc.createPlan).not.toHaveBeenCalled();

    component.form.patchValue({ title: 'Audit', standard: 'y'.repeat(101) });
    component.submit();
    expect(svc.createPlan).not.toHaveBeenCalled();
  });

  it('n\'envoie pas deux fois pendant un envoi en cours', () => {
    component.form.patchValue({ title: 'Audit interne' });
    component.submitting = true;

    component.submit();

    expect(svc.createPlan).not.toHaveBeenCalled();
  });

  // ---- Création -------------------------------------------------------------------

  it('crée le plan avec l\'auditeur du JWT, champs nettoyés', () => {
    component.form.patchValue({
      title: '  Audit interne ISO 9001  ',
      scope: '  Siège + 3 usines  ',
      type: 'CERTIFICATION',
      standard: '  ISO_9001  ',
      scheduledDate: '2026-11-15'
    });

    component.submit();

    expect(svc.createPlan).toHaveBeenCalledWith({
      title: 'Audit interne ISO 9001',
      scope: 'Siège + 3 usines',
      type: 'CERTIFICATION',
      standard: 'ISO_9001',
      scheduledDate: '2026-11-15',
      // Laissé vide : la clé part à `undefined` comme les autres facultatifs
      // (cf. « omet les champs facultatifs laissés vides ») et disparaît à la
      // sérialisation JSON. Le serveur ne voit donc aucun destinataire.
      reminderEmail: undefined,
      leadAuditorId: 'u1',
      leadAuditorName: 'Demo'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('propose soi-même en tête puis les membres actifs, soi-même par défaut', () => {
    expect(component.leadAuditors.map(o => o.id)).toEqual(['u1', 'u2']);
    expect(component.leadAuditors[0].label).toContain('Demo');
    expect(component.form.getRawValue().leadAuditorId).toBe('u1');
    expect(component.directoryUnavailable).toBeFalse();
  });

  it('confie l\'audit à un collègue choisi, avec son nom', () => {
    component.form.patchValue({ title: 'Audit', leadAuditorId: 'u2' });

    component.submit();

    expect(svc.createPlan).toHaveBeenCalledWith(jasmine.objectContaining({
      leadAuditorId: 'u2', leadAuditorName: 'bruno@exemple.test'
    }));
  });

  it('nettoie le destinataire du rappel comme les autres champs', () => {
    component.form.patchValue({
      title: 'Audit', reminderEmail: '  qualite@exemple.test  '
    });

    component.submit();

    expect(svc.createPlan).toHaveBeenCalledWith(jasmine.objectContaining({
      reminderEmail: 'qualite@exemple.test'
    }));
  });

  it('refuse d\'envoyer une adresse de rappel mal formée', () => {
    // Le formulaire barre la route AVANT l'appel : une adresse fautive rejetée
    // par le serveur ne se découvrirait qu'au 400, sans dire quel champ est en cause.
    component.form.patchValue({ title: 'Audit', reminderEmail: 'pas-une-adresse' });

    component.submit();

    expect(svc.createPlan).not.toHaveBeenCalled();
  });

  it('omet les champs facultatifs laissés vides', () => {
    component.form.patchValue({ title: 'Audit', scope: '   ', standard: '  ' });

    component.submit();

    expect(svc.createPlan).toHaveBeenCalledWith(jasmine.objectContaining({
      scope: undefined, standard: undefined, scheduledDate: undefined
    }));
  });

  // ---- Cas dégradés -----------------------------------------------------------------

  it('refuse de créer sans auditeur responsable, plutôt que d\'en inventer un', () => {
    component.form.patchValue({ title: 'Audit', leadAuditorId: '' });

    component.submit();

    expect(svc.createPlan).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('annuaire indisponible : seul soi-même est proposé, et l\'écran le dit', () => {
    team.list.and.returnValue(throwError(() => ({ status: 403 })));
    component.ngOnInit();

    expect(component.leadAuditors.map(o => o.id)).toEqual(['u1']);
    expect(component.directoryUnavailable).toBeTrue();
  });

  it('laisse le dialogue ouvert et explique quand le serveur refuse', () => {
    svc.createPlan.and.returnValue(throwError(() => ({ status: 500 })));
    component.form.patchValue({ title: 'Audit' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    // La saisie doit rester récupérable : refermer ferait perdre le travail.
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien créer à l\'annulation', () => {
    component.cancel();

    expect(svc.createPlan).not.toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
