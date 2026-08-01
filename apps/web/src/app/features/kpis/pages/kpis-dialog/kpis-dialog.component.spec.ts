import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { KpisService } from '../../kpis.service';
import { KpiResponse } from '../../kpis.types';
import { KpisDialogComponent, KpisDialogData } from './kpis-dialog.component';

describe('KpisDialogComponent', () => {
  let fixture: ComponentFixture<KpisDialogComponent>;
  let component: KpisDialogComponent;
  let svc: jasmine.SpyObj<KpisService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<KpisDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let session: AuthUser | null;

  const existing: KpiResponse = {
    id: 'kpi-1', tenantId: 't1', code: 'capa-closure-time', name: 'Délai de clôture CAPA',
    description: 'Ouverture → clôture', category: 'CAPA', unit: 'jours',
    direction: 'LOWER_IS_BETTER', frequency: 'MONTHLY',
    targetValue: 30, thresholdWarning: 45, thresholdCritical: 60,
    status: 'ACTIVE', applicableIndustriesCsv: 'all', ownerUserId: 'u1', createdBy: 'u1',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z'
  };

  async function setup(data: KpisDialogData | null): Promise<void> {
    TestBed.resetTestingModule();
    svc = jasmine.createSpyObj<KpisService>('KpisService', ['create', 'update']);
    svc.create.and.returnValue(of(existing));
    svc.update.and.returnValue(of(existing));
    dialogRef = jasmine.createSpyObj<MatDialogRef<KpisDialogComponent>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [KpisDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: KpisService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => session } },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(KpisDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    session = { userId: 'u1', tenantId: 't1', displayName: 'Demo', roles: ['quality_manager'] };
  });

  // ---- Création ---------------------------------------------------------------

  it('ouvre un formulaire de création orienté « plus haut = mieux » en mensuel', async () => {
    await setup(null);
    expect(component.isEdit).toBeFalse();
    expect(component.dialogTitle).toBe('Nouveau KPI');
    expect(component.submitLabel).toBe('Créer');
    const v = component.form.getRawValue();
    expect(v.direction).toBe('HIGHER_IS_BETTER');
    expect(v.frequency).toBe('MONTHLY');
    expect(component.frequencies).toContain('REALTIME');
    expect(component.directions.length).toBe(2);
  });

  it('crée le KPI en nettoyant les champs et en omettant les vides', async () => {
    await setup(null);
    component.form.patchValue({
      code: ' dpmo ', name: ' DPMO ', description: '  ', category: ' DMAIC ',
      unit: ' DPMO ', direction: 'LOWER_IS_BETTER', frequency: 'QUARTERLY',
      targetValue: 3.4, thresholdWarning: 100, thresholdCritical: 1000,
      applicableIndustriesCsv: ' Manufacturing '
    });
    component.submit();

    expect(svc.create).toHaveBeenCalledWith({
      code: 'dpmo', name: 'DPMO', description: undefined, category: 'DMAIC',
      unit: 'DPMO', direction: 'LOWER_IS_BETTER', frequency: 'QUARTERLY',
      targetValue: 3.4, thresholdWarning: 100, thresholdCritical: 1000,
      applicableIndustriesCsv: 'Manufacturing', createdBy: 'u1'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(existing);
  });

  it('refuse un code qui ne respecte pas le motif serveur', async () => {
    await setup(null);
    component.form.patchValue({ code: 'First Pass Yield', name: 'FPY' });
    component.submit();
    expect(svc.create).not.toHaveBeenCalled();
    expect(component.form.controls.code.touched).toBeTrue();
  });

  it('n’envoie rien à la création si la session a expiré', async () => {
    session = null;
    await setup(null);
    component.form.patchValue({ code: 'fpy', name: 'FPY' });
    expect(() => component.submit()).toThrowError('No session');
    expect(svc.create).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  // ---- Cohérence des seuils (A04) -----------------------------------------------

  it('refuse des seuils désordonnés pour un KPI « plus haut = mieux »', async () => {
    await setup(null);
    component.form.patchValue({
      code: 'fpy', name: 'FPY', direction: 'HIGHER_IS_BETTER',
      targetValue: 98, thresholdWarning: 99, thresholdCritical: 90   // warning au-dessus de la cible
    });
    component.submit();
    expect(svc.create).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('accepte des seuils ordonnés pour un KPI « plus haut = mieux »', async () => {
    await setup(null);
    component.form.patchValue({
      code: 'fpy', name: 'FPY', direction: 'HIGHER_IS_BETTER',
      targetValue: 98, thresholdWarning: 95, thresholdCritical: 90
    });
    component.submit();
    expect(svc.create).toHaveBeenCalled();
  });

  it('refuse des seuils désordonnés pour un KPI « plus bas = mieux »', async () => {
    await setup(null);
    component.form.patchValue({
      code: 'dpmo', name: 'DPMO', direction: 'LOWER_IS_BETTER',
      targetValue: 30, thresholdWarning: 60, thresholdCritical: 45   // critical sous le warning
    });
    component.submit();
    expect(svc.create).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
  });

  it('n’impose l’ordre que si cible et seuils sont tous renseignés', async () => {
    await setup(null);
    component.form.patchValue({
      code: 'fpy', name: 'FPY', direction: 'HIGHER_IS_BETTER',
      targetValue: null, thresholdWarning: 99, thresholdCritical: 100
    });
    component.submit();
    expect(svc.create).toHaveBeenCalled();
  });

  // ---- Édition -------------------------------------------------------------------

  it('verrouille le code et le sens en édition — ils changeraient la signification des mesures', async () => {
    await setup({ kpi: existing });
    expect(component.isEdit).toBeTrue();
    expect(component.dialogTitle).toBe('Modifier le KPI');
    expect(component.submitLabel).toBe('Enregistrer');
    expect(component.form.controls.code.disabled).toBeTrue();
    expect(component.form.controls.direction.disabled).toBeTrue();
    expect(component.form.getRawValue().targetValue).toBe(30);
  });

  it('met à jour le KPI existant sans renvoyer son code ni son sens', async () => {
    await setup({ kpi: existing });
    component.form.patchValue({ name: ' Délai CAPA ', unit: '  ', targetValue: 25 });
    component.submit();

    expect(svc.update).toHaveBeenCalledWith('kpi-1', {
      name: 'Délai CAPA', description: 'Ouverture → clôture', category: 'CAPA',
      unit: undefined, frequency: 'MONTHLY',
      targetValue: 25, thresholdWarning: 45, thresholdCritical: 60,
      applicableIndustriesCsv: 'all'
    });
    expect(svc.create).not.toHaveBeenCalled();
  });

  it('affiche l’erreur serveur sans fermer le dialogue', async () => {
    await setup({ kpi: existing });
    svc.update.and.returnValue(throwError(() => ({ status: 400 })));
    component.submit();
    expect(snack.open)
      .toHaveBeenCalledWith('Champs invalides — vérifiez le formulaire.', 'OK', { duration: 4000 });
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien envoyer à l’annulation', async () => {
    await setup(null);
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.create).not.toHaveBeenCalled();
  });
});
