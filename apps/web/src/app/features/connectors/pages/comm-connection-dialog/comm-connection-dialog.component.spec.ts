import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ConnectorsService } from '../../connectors.service';
import { CommConnection } from '../../connectors.types';
import { CommConnectionDialogComponent, CommConnectionDialogData } from './comm-connection-dialog.component';

describe('CommConnectionDialogComponent', () => {
  let svc: jasmine.SpyObj<ConnectorsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CommConnectionDialogComponent>>;
  let user: AuthUser | null;

  const WEBHOOK = 'https://hooks.slack.com/services/T000/B000/xxxx';

  const connection = (over: Partial<CommConnection> = {}): CommConnection => ({
    id: 'c-1', tenantId: 't-1', name: 'Alertes HSE', provider: 'TEAMS', channel: '#hse',
    status: 'ACTIVE', consecutiveFailures: 0, lastNotifiedAt: null, lastSuccessAt: null,
    createdBy: 'u-1', createdAt: null, updatedAt: null, ...over
  });

  async function setup(data: CommConnectionDialogData): Promise<ComponentFixture<CommConnectionDialogComponent>> {
    await TestBed.configureTestingModule({
      declarations: [CommConnectionDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: ConnectorsService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AuthService, useValue: { snapshot: () => user } },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    return TestBed.createComponent(CommConnectionDialogComponent);
  }

  beforeEach(() => {
    user = { userId: 'u-42', tenantId: 't-1', displayName: 'Admin', roles: ['admin_tenant'] };
    svc = jasmine.createSpyObj<ConnectorsService>('ConnectorsService', ['createComm', 'updateComm']);
    svc.createComm.and.returnValue(of(connection()));
    svc.updateComm.and.returnValue(of(connection()));
    dialogRef = jasmine.createSpyObj<MatDialogRef<CommConnectionDialogComponent>>('MatDialogRef', ['close']);
  });

  // ---- Création --------------------------------------------------------------

  it('exige l\'URL du webhook à la création', async () => {
    const fixture = await setup({ connection: null });
    const url = fixture.componentInstance.form.controls.webhookUrl;

    expect(fixture.componentInstance.editing).toBeFalse();
    expect(url.hasError('required')).toBeTrue();
  });

  it('rejette une URL non https et une URL trop courte', async () => {
    const fixture = await setup({ connection: null });
    const url = fixture.componentInstance.form.controls.webhookUrl;

    url.setValue('http://hooks.example/x');
    expect(url.hasError('pattern')).toBeTrue();

    url.setValue('https:/');
    expect(url.hasError('minlength')).toBeTrue();

    url.setValue(WEBHOOK);
    expect(url.valid).toBeTrue();
  });

  it('crée la destination avec l\'auteur issu de la session', async () => {
    const fixture = await setup({ connection: null });
    // Espaces en fin de saisie : le motif serveur (`.+`) les accepte, c'est au client
    // de normaliser avant l'envoi.
    fixture.componentInstance.form.patchValue({
      name: 'Alertes HSE ', provider: 'SLACK', webhookUrl: `${WEBHOOK} `, channel: '#hse '
    });

    fixture.componentInstance.submit();

    expect(svc.createComm).toHaveBeenCalledWith({
      name: 'Alertes HSE', provider: 'SLACK', webhookUrl: WEBHOOK,
      channel: '#hse', createdBy: 'u-42'
    });
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('omet le salon quand il n\'est pas renseigné', async () => {
    const fixture = await setup({ connection: null });
    fixture.componentInstance.form.patchValue({ name: 'Alertes', webhookUrl: WEBHOOK });

    fixture.componentInstance.submit();

    expect(svc.createComm.calls.mostRecent().args[0].channel).toBeUndefined();
  });

  it('refuse de créer sans session plutôt que d\'inventer un auteur', async () => {
    user = null;
    const fixture = await setup({ connection: null });
    fixture.componentInstance.form.patchValue({ name: 'Alertes', webhookUrl: WEBHOOK });

    fixture.componentInstance.submit();

    expect(svc.createComm).not.toHaveBeenCalled();
    expect(fixture.componentInstance.errorMessage).toContain('Session expirée');
  });

  it('montre l\'exemple d\'URL du fournisseur choisi', async () => {
    const fixture = await setup({ connection: null });
    const c = fixture.componentInstance;

    expect(c.selectedUrlExample).toContain('webhook.office.com');
    c.form.controls.provider.setValue('MATTERMOST');
    expect(c.selectedUrlExample).toContain('/hooks/');
  });

  // ---- Modification ----------------------------------------------------------

  it('ne pré-remplit jamais l\'URL du webhook : c\'est un secret que le serveur ne renvoie pas', async () => {
    const fixture = await setup({ connection: connection() });
    const c = fixture.componentInstance;

    expect(c.editing).toBeTrue();
    expect(c.form.controls.webhookUrl.value).toBe('');
    expect(c.form.controls.provider.disabled).toBeTrue();
    expect(c.form.valid).toBeTrue();
  });

  it('conserve l\'URL existante quand le champ reste vide', async () => {
    const fixture = await setup({ connection: connection() });

    fixture.componentInstance.submit();

    const payload = svc.updateComm.calls.mostRecent().args[1];
    expect(payload.webhookUrl).toBeUndefined();
    expect(payload.name).toBe('Alertes HSE');
    expect(payload.channel).toBe('#hse');
    expect(payload.status).toBe('ACTIVE');
  });

  it('remplace l\'URL quand on en saisit une nouvelle', async () => {
    const fixture = await setup({ connection: connection() });
    fixture.componentInstance.form.controls.webhookUrl.setValue(WEBHOOK);

    fixture.componentInstance.submit();

    expect(svc.updateComm.calls.mostRecent().args[1].webhookUrl).toBe(WEBHOOK);
  });

  it('ramène un statut « désactivé sur erreurs » vers un choix que l\'administrateur peut poser', async () => {
    const fixture = await setup({ connection: connection({ status: 'DISABLED_ON_ERRORS' }) });
    expect(fixture.componentInstance.form.controls.status.value).toBe('ACTIVE');
  });

  // ---- Robustesse ------------------------------------------------------------

  it('affiche un message sûr et ne ferme pas quand l\'enregistrement échoue', async () => {
    svc.updateComm.and.returnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    const fixture = await setup({ connection: connection() });

    fixture.componentInstance.submit();

    expect(fixture.componentInstance.errorMessage).toBe('Service inaccessible — vérifiez votre connexion.');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('masque l\'URL par défaut et adapte son libellé accessible', async () => {
    const fixture = await setup({ connection: null });
    const c = fixture.componentInstance;

    expect(c.showWebhook).toBeFalse();
    const hidden = c.webhookToggleLabel;
    c.toggleWebhook();
    expect(c.showWebhook).toBeTrue();
    expect(c.webhookToggleLabel).not.toBe(hidden);
  });

  it('adapte titre et libellé d\'URL au mode', async () => {
    const fixture = await setup({ connection: connection() });
    expect(fixture.componentInstance.title).toContain('Modifier');
    expect(fixture.componentInstance.webhookLabel).toContain('inchangée');
    expect(fixture.componentInstance.statusLabel('DISABLED')).toBe('Désactivée');
  });

  it('annule sans rien envoyer', async () => {
    const fixture = await setup({ connection: connection() });
    fixture.componentInstance.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.updateComm).not.toHaveBeenCalled();
  });
});
