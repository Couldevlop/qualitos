import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ConnectorsService } from '../../connectors.service';
import { ErpConnection } from '../../connectors.types';
import { ErpConnectionDialogComponent, ErpConnectionDialogData } from './erp-connection-dialog.component';

describe('ErpConnectionDialogComponent', () => {
  let svc: jasmine.SpyObj<ConnectorsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ErpConnectionDialogComponent>>;
  let user: AuthUser | null;

  const connection = (over: Partial<ErpConnection> = {}): ErpConnection => ({
    id: 'e-1', tenantId: 't-1', name: 'SAP prod', provider: 'SAP',
    baseUrl: 'https://erp.example/odata', username: 'svc', externalScope: 'Usine A',
    status: 'ACTIVE', consecutiveFailures: 0, lastSyncAt: null, lastSuccessAt: null,
    createdBy: 'u-1', createdAt: null, updatedAt: null, ...over
  });

  async function setup(data: ErpConnectionDialogData): Promise<ComponentFixture<ErpConnectionDialogComponent>> {
    await TestBed.configureTestingModule({
      declarations: [ErpConnectionDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: ConnectorsService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AuthService, useValue: { snapshot: () => user } },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    return TestBed.createComponent(ErpConnectionDialogComponent);
  }

  beforeEach(() => {
    user = { userId: 'u-42', tenantId: 't-1', displayName: 'Admin', roles: ['admin_tenant'] };
    svc = jasmine.createSpyObj<ConnectorsService>('ConnectorsService', ['createErp', 'updateErp']);
    svc.createErp.and.returnValue(of(connection()));
    svc.updateErp.and.returnValue(of(connection({ name: 'SAP recette' })));
    dialogRef = jasmine.createSpyObj<MatDialogRef<ErpConnectionDialogComponent>>('MatDialogRef', ['close']);
  });

  // ---- Création --------------------------------------------------------------

  it('démarre vide et invalide en création', async () => {
    const fixture = await setup({ connection: null });
    const c = fixture.componentInstance;

    expect(c.editing).toBeFalse();
    expect(c.form.invalid).toBeTrue();
    expect(c.form.controls.provider.disabled).toBeFalse();
  });

  it('refuse d\'envoyer un formulaire invalide et marque les champs', async () => {
    const fixture = await setup({ connection: null });
    fixture.componentInstance.submit();

    expect(svc.createErp).not.toHaveBeenCalled();
    expect(fixture.componentInstance.form.controls.name.touched).toBeTrue();
  });

  it('rejette une URL non https, comme le motif du serveur', async () => {
    const fixture = await setup({ connection: null });
    const url = fixture.componentInstance.form.controls.baseUrl;

    url.setValue('http://erp.example/odata');
    expect(url.hasError('pattern')).toBeTrue();

    // Une URL qui ne commence pas par https:// est refusée y compris précédée d'espaces :
    // le motif serveur est ancré, mieux vaut le signaler ici qu'essuyer un 400.
    url.setValue(' https://erp.example/odata');
    expect(url.hasError('pattern')).toBeTrue();

    url.setValue('https://erp.example/odata');
    expect(url.valid).toBeTrue();
  });

  it('exige un secret d\'au moins 4 caractères à la création', async () => {
    const fixture = await setup({ connection: null });
    const secret = fixture.componentInstance.form.controls.secret;

    expect(secret.hasError('required')).toBeTrue();
    secret.setValue('abc');
    expect(secret.hasError('minlength')).toBeTrue();
  });

  it('crée la connexion avec l\'auteur issu de la session et normalise les espaces', async () => {
    const fixture = await setup({ connection: null });
    const c = fixture.componentInstance;
    // Espaces en fin de saisie : fréquents au copier-coller et acceptés par le motif
    // serveur (`.+`) — c'est donc au client de normaliser avant l'envoi.
    c.form.patchValue({
      name: 'SAP prod  ', provider: 'DYNAMICS', baseUrl: 'https://erp.example/odata ',
      username: 'svc ', secret: 'topsecret ', externalScope: 'Usine A '
    });

    c.submit();

    expect(svc.createErp).toHaveBeenCalledWith({
      name: 'SAP prod', provider: 'DYNAMICS', baseUrl: 'https://erp.example/odata',
      username: 'svc', secret: 'topsecret', externalScope: 'Usine A', createdBy: 'u-42'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(connection());
  });

  it('omet les champs facultatifs laissés vides plutôt que d\'envoyer des chaînes vides', async () => {
    const fixture = await setup({ connection: null });
    fixture.componentInstance.form.patchValue({
      name: 'SAP', baseUrl: 'https://erp.example', secret: 'topsecret'
    });

    fixture.componentInstance.submit();

    const payload = svc.createErp.calls.mostRecent().args[0];
    expect(payload.username).toBeUndefined();
    expect(payload.externalScope).toBeUndefined();
  });

  it('refuse de créer sans session plutôt que d\'inventer un auteur', async () => {
    user = null;
    const fixture = await setup({ connection: null });
    fixture.componentInstance.form.patchValue({
      name: 'SAP', baseUrl: 'https://erp.example', secret: 'topsecret'
    });

    fixture.componentInstance.submit();

    expect(svc.createErp).not.toHaveBeenCalled();
    expect(fixture.componentInstance.errorMessage).toContain('Session expirée');
  });

  // ---- Modification ----------------------------------------------------------

  it('pré-remplit le formulaire et fige le fournisseur en modification', async () => {
    const fixture = await setup({ connection: connection() });
    const c = fixture.componentInstance;

    expect(c.editing).toBeTrue();
    expect(c.form.controls.name.value).toBe('SAP prod');
    expect(c.form.controls.provider.disabled).toBeTrue();
    // Le serveur ne renvoie jamais le secret : le champ reste vide et facultatif.
    expect(c.form.controls.secret.value).toBe('');
    expect(c.form.valid).toBeTrue();
  });

  it('n\'envoie pas le secret quand il n\'a pas été saisi : la valeur en base est conservée', async () => {
    const fixture = await setup({ connection: connection() });

    fixture.componentInstance.submit();

    const payload = svc.updateErp.calls.mostRecent().args[1];
    expect(payload.secret).toBeUndefined();
    expect(payload.name).toBe('SAP prod');
    expect(payload.status).toBe('ACTIVE');
  });

  it('envoie le secret quand on le remplace', async () => {
    const fixture = await setup({ connection: connection() });
    fixture.componentInstance.form.controls.secret.setValue('nouveau-secret');

    fixture.componentInstance.submit();

    expect(svc.updateErp.calls.mostRecent().args[1].secret).toBe('nouveau-secret');
  });

  it('ramène un statut « désactivé sur erreurs » à un choix que l\'administrateur peut poser', async () => {
    const fixture = await setup({ connection: connection({ status: 'DISABLED_ON_ERRORS' }) });
    // Le serveur pose lui-même DISABLED_ON_ERRORS : le formulaire ne propose que
    // ACTIVE ou DISABLED, et repart d'ACTIVE pour que la réactivation soit le geste simple.
    expect(fixture.componentInstance.form.controls.status.value).toBe('ACTIVE');
  });

  // ---- Robustesse ------------------------------------------------------------

  it('affiche un message sûr et ne ferme pas quand l\'enregistrement échoue', async () => {
    svc.updateErp.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    const fixture = await setup({ connection: connection() });

    fixture.componentInstance.submit();

    expect(fixture.componentInstance.errorMessage).toBe('Erreur serveur — réessayez dans un instant.');
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(fixture.componentInstance.submitting).toBeFalse();
  });

  it('ignore un second envoi tant que le premier est en vol', async () => {
    const fixture = await setup({ connection: connection() });
    fixture.componentInstance.submitting = true;

    fixture.componentInstance.submit();

    expect(svc.updateErp).not.toHaveBeenCalled();
  });

  it('bascule l\'affichage du secret et adapte son libellé accessible', async () => {
    const fixture = await setup({ connection: null });
    const c = fixture.componentInstance;

    expect(c.showSecret).toBeFalse();
    const hidden = c.secretToggleLabel;
    c.toggleSecret();
    expect(c.showSecret).toBeTrue();
    expect(c.secretToggleLabel).not.toBe(hidden);
  });

  it('annule sans rien envoyer', async () => {
    const fixture = await setup({ connection: connection() });
    fixture.componentInstance.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.updateErp).not.toHaveBeenCalled();
  });

  it('annonce la création dans son titre et son libellé de secret', async () => {
    const fixture = await setup({ connection: null });
    expect(fixture.componentInstance.title).toContain('Nouvelle');
    expect(fixture.componentInstance.secretLabel).not.toContain('inchangé');
  });

  it('annonce la modification et nomme les statuts comme le tableau', async () => {
    const fixture = await setup({ connection: connection() });
    expect(fixture.componentInstance.title).toContain('Modifier');
    expect(fixture.componentInstance.secretLabel).toContain('inchangé');
    expect(fixture.componentInstance.statusLabel('DISABLED')).toBe('Désactivée');
    expect(fixture.componentInstance.statusLabel('ACTIVE')).toBe('Active');
  });
});
