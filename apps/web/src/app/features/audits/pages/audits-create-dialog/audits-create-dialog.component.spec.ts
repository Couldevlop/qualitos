import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AuditsService } from '../../audits.service';
import { AuditPlanResponse } from '../../audits.types';
import { AuditsCreateDialogComponent } from './audits-create-dialog.component';

/**
 * Création d'un plan d'audit (§4.4).
 *
 * L'auditeur pilote est pris dans le JWT et jamais saisi (§18.2 #2) : un plan
 * d'audit dont le pilote serait déclaré par l'appelant n'aurait aucune valeur
 * probatoire.
 */
describe('AuditsCreateDialogComponent', () => {
  let fixture: ComponentFixture<AuditsCreateDialogComponent>;
  let component: AuditsCreateDialogComponent;
  let svc: jasmine.SpyObj<AuditsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<AuditsCreateDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let session: AuthUser | null;

  const created = { id: 'a-9', title: 'Audit' } as AuditPlanResponse;

  beforeEach(async () => {
    session = { userId: 'u1', tenantId: 't1', displayName: 'Demo', roles: ['auditor'] };
    svc = jasmine.createSpyObj<AuditsService>('AuditsService', ['createPlan']);
    svc.createPlan.and.returnValue(of(created));
    dialogRef = jasmine.createSpyObj<MatDialogRef<AuditsCreateDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [AuditsCreateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: AuditsService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => session } }
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
      leadAuditorId: 'u1'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('omet les champs facultatifs laissés vides', () => {
    component.form.patchValue({ title: 'Audit', scope: '   ', standard: '  ' });

    component.submit();

    expect(svc.createPlan).toHaveBeenCalledWith(jasmine.objectContaining({
      scope: undefined, standard: undefined, scheduledDate: undefined
    }));
  });

  // ---- Cas dégradés -----------------------------------------------------------------

  it('refuse de créer sans session et le dit, plutôt que d\'inventer un pilote', () => {
    session = null;
    component.form.patchValue({ title: 'Audit' });

    component.submit();

    expect(svc.createPlan).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
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
