import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ConnectorsService } from '../../connectors.service';
import { EhrConnection } from '../../connectors.types';
import { EhrConnectionDialogComponent, EhrConnectionDialogData } from './ehr-connection-dialog.component';

describe('EhrConnectionDialogComponent', () => {
  let svc: jasmine.SpyObj<ConnectorsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<EhrConnectionDialogComponent>>;
  let user: AuthUser | null;

  const connection = (over: Partial<EhrConnection> = {}): EhrConnection => ({
    id: 'h-1', tenantId: 't-1', name: 'CHU Sud', provider: 'FHIR_R5',
    fhirBaseUrl: 'https://fhir.example/R5', authMode: 'BASIC', username: 'svc',
    resourceCategory: 'AdverseEvent', status: 'ACTIVE', consecutiveFailures: 0,
    lastSyncAt: null, lastSuccessAt: null, createdBy: 'u-1', createdAt: null, updatedAt: null, ...over
  });

  async function setup(data: EhrConnectionDialogData): Promise<ComponentFixture<EhrConnectionDialogComponent>> {
    await TestBed.configureTestingModule({
      declarations: [EhrConnectionDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: ConnectorsService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AuthService, useValue: { snapshot: () => user } },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    return TestBed.createComponent(EhrConnectionDialogComponent);
  }

  beforeEach(() => {
    user = { userId: 'u-42', tenantId: 't-1', displayName: 'Admin', roles: ['admin_tenant'] };
    svc = jasmine.createSpyObj<ConnectorsService>('ConnectorsService', ['createEhr', 'updateEhr']);
    svc.createEhr.and.returnValue(of(connection()));
    svc.updateEhr.and.returnValue(of(connection()));
    dialogRef = jasmine.createSpyObj<MatDialogRef<EhrConnectionDialogComponent>>('MatDialogRef', ['close']);
  });

  // ---- Création --------------------------------------------------------------

  it('propose FHIR R5 et l\'authentification Basic par défaut', async () => {
    const fixture = await setup({ connection: null });
    const c = fixture.componentInstance;

    expect(c.editing).toBeFalse();
    expect(c.form.controls.provider.value).toBe('FHIR_R5');
    expect(c.form.controls.authMode.value).toBe('BASIC');
    expect(c.usernameRelevant).toBeTrue();
  });

  it('rejette une URL FHIR non https', async () => {
    const fixture = await setup({ connection: null });
    fixture.componentInstance.form.controls.fhirBaseUrl.setValue('http://fhir.example/R5');
    expect(fixture.componentInstance.form.controls.fhirBaseUrl.hasError('pattern')).toBeTrue();
  });

  it('crée la connexion avec l\'auteur issu de la session', async () => {
    const fixture = await setup({ connection: null });
    // Espaces en fin de saisie : le motif serveur (`.+`) les accepte, c'est au client
    // de normaliser avant l'envoi.
    fixture.componentInstance.form.patchValue({
      name: 'CHU Sud ', fhirBaseUrl: 'https://fhir.example/R5 ', authMode: 'BASIC',
      username: 'svc ', secret: 'motdepasse ', resourceCategory: 'AdverseEvent '
    });

    fixture.componentInstance.submit();

    expect(svc.createEhr).toHaveBeenCalledWith({
      name: 'CHU Sud', provider: 'FHIR_R5', fhirBaseUrl: 'https://fhir.example/R5',
      authMode: 'BASIC', username: 'svc', secret: 'motdepasse',
      resourceCategory: 'AdverseEvent', createdBy: 'u-42'
    });
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('n\'envoie pas de compte de service en mode Bearer : le serveur ne s\'en sert pas', async () => {
    const fixture = await setup({ connection: null });
    const c = fixture.componentInstance;
    c.form.patchValue({
      name: 'CHU', fhirBaseUrl: 'https://fhir.example/R5', authMode: 'BEARER',
      username: 'svc', secret: 'jeton-porteur'
    });

    expect(c.usernameRelevant).toBeFalse();
    c.submit();

    expect(svc.createEhr.calls.mostRecent().args[0].username).toBeUndefined();
  });

  it('nomme le secret « jeton » en mode Bearer', async () => {
    const fixture = await setup({ connection: null });
    const c = fixture.componentInstance;

    expect(c.secretLabel).toContain('Secret');
    c.form.controls.authMode.setValue('BEARER');
    expect(c.secretLabel).toContain('Jeton');
  });

  it('refuse de créer sans session plutôt que d\'inventer un auteur', async () => {
    user = null;
    const fixture = await setup({ connection: null });
    fixture.componentInstance.form.patchValue({
      name: 'CHU', fhirBaseUrl: 'https://fhir.example/R5', secret: 'motdepasse'
    });

    fixture.componentInstance.submit();

    expect(svc.createEhr).not.toHaveBeenCalled();
    expect(fixture.componentInstance.errorMessage).toContain('Session expirée');
  });

  // ---- Modification ----------------------------------------------------------

  it('pré-remplit et fige la version FHIR en modification', async () => {
    const fixture = await setup({ connection: connection() });
    const c = fixture.componentInstance;

    expect(c.editing).toBeTrue();
    expect(c.form.controls.provider.disabled).toBeTrue();
    // Le mode d'authentification, lui, reste modifiable : le serveur l'accepte.
    expect(c.form.controls.authMode.disabled).toBeFalse();
    expect(c.form.controls.secret.value).toBe('');
  });

  it('conserve le secret existant quand le champ reste vide', async () => {
    const fixture = await setup({ connection: connection() });

    fixture.componentInstance.submit();

    const payload = svc.updateEhr.calls.mostRecent().args[1];
    expect(payload.secret).toBeUndefined();
    expect(payload.authMode).toBe('BASIC');
    expect(payload.resourceCategory).toBe('AdverseEvent');
  });

  it('remplace le jeton quand on en saisit un nouveau', async () => {
    const fixture = await setup({ connection: connection({ authMode: 'BEARER' }) });
    fixture.componentInstance.form.controls.secret.setValue('nouveau-jeton');

    fixture.componentInstance.submit();

    expect(svc.updateEhr.calls.mostRecent().args[1].secret).toBe('nouveau-jeton');
    expect(fixture.componentInstance.secretLabel).toContain('inchangé');
  });

  // ---- Robustesse ------------------------------------------------------------

  it('affiche un message sûr et ne ferme pas quand l\'enregistrement échoue', async () => {
    svc.updateEhr.and.returnValue(throwError(() => new HttpErrorResponse({ status: 400 })));
    const fixture = await setup({ connection: connection() });

    fixture.componentInstance.submit();

    expect(fixture.componentInstance.errorMessage).toBe('Champs invalides — vérifiez le formulaire.');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('refuse d\'envoyer un formulaire invalide', async () => {
    const fixture = await setup({ connection: null });
    fixture.componentInstance.submit();

    expect(svc.createEhr).not.toHaveBeenCalled();
    expect(fixture.componentInstance.form.controls.name.touched).toBeTrue();
  });

  it('traduit les modes d\'authentification et les statuts pour l\'affichage', async () => {
    const fixture = await setup({ connection: connection() });
    const c = fixture.componentInstance;

    expect(c.authModeLabel('BASIC')).toContain('Basic');
    expect(c.authModeLabel('BEARER')).toContain('Bearer');
    expect(c.statusLabel('ACTIVE')).toBe('Active');
  });

  it('annule sans rien envoyer', async () => {
    const fixture = await setup({ connection: connection() });
    fixture.componentInstance.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.updateEhr).not.toHaveBeenCalled();
  });
});
