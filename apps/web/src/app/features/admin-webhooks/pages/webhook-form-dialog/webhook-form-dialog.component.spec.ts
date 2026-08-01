import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { WebhooksService } from '../../webhooks.service';
import { WebhookSubscription } from '../../webhooks.types';
import { WebhookFormDialogComponent, WebhookFormDialogData } from './webhook-form-dialog.component';

describe('WebhookFormDialogComponent', () => {
  let svc: jasmine.SpyObj<WebhooksService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<WebhookFormDialogComponent>>;
  let user: AuthUser | null;

  const SECRET = 'f'.repeat(64);

  const subscription = (over: Partial<WebhookSubscription> = {}): WebhookSubscription => ({
    id: 's-1', tenantId: 't-1', name: 'ERP', endpointUrl: 'https://erp.example/hook',
    eventTypes: ['capa.case.opened', 'pdca.cycle.created'], status: 'ACTIVE', maxRetries: 3,
    consecutiveFailures: 0, lastTriggeredAt: null, lastSuccessAt: null, createdBy: 'u-1',
    createdAt: null, updatedAt: null, ...over
  });

  async function setup(data: WebhookFormDialogData): Promise<ComponentFixture<WebhookFormDialogComponent>> {
    await TestBed.configureTestingModule({
      declarations: [WebhookFormDialogComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        { provide: WebhooksService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AuthService, useValue: { snapshot: () => user } },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    return TestBed.createComponent(WebhookFormDialogComponent);
  }

  beforeEach(() => {
    user = { userId: 'u-42', tenantId: 't-1', displayName: 'Admin', roles: ['admin_tenant'] };
    svc = jasmine.createSpyObj<WebhooksService>('WebhooksService',
      ['createSubscription', 'updateSubscription', 'generateSecret']);
    svc.generateSecret.and.returnValue(SECRET);
    dialogRef = jasmine.createSpyObj<MatDialogRef<WebhookFormDialogComponent>>('MatDialogRef', ['close']);
  });

  // ---- Création --------------------------------------------------------------

  it('pré-génère un secret et laisse le formulaire invalide tant que rien n\'est saisi', async () => {
    const fixture = await setup({ subscription: null });
    const c = fixture.componentInstance;

    expect(c.editing).toBeFalse();
    expect(c.form.controls.secret.value).toBe(SECRET);
    expect(c.form.invalid).toBeTrue();
  });

  it('refuse d\'envoyer un formulaire invalide', async () => {
    const fixture = await setup({ subscription: null });
    fixture.componentInstance.submit();
    expect(svc.createSubscription).not.toHaveBeenCalled();
    expect(fixture.componentInstance.form.controls.name.touched).toBeTrue();
  });

  it('rejette une URL non http(s)', async () => {
    const fixture = await setup({ subscription: null });
    fixture.componentInstance.form.controls.endpointUrl.setValue('ftp://erp.example/hook');
    expect(fixture.componentInstance.form.controls.endpointUrl.hasError('pattern')).toBeTrue();
  });

  it('crée l\'abonnement avec l\'auteur issu de la session et ferme en exposant le secret', async () => {
    svc.createSubscription.and.returnValue(of({ subscription: subscription(), secret: SECRET }));
    const fixture = await setup({ subscription: null });
    const c = fixture.componentInstance;

    // Espaces en fin de saisie : fréquents au copier-coller, et acceptés par le motif
    // serveur (`.+`) — c'est donc au client de normaliser avant l'envoi.
    c.form.patchValue({
      name: '  ERP  ', endpointUrl: 'https://erp.example/hook  ',
      eventTypes: ['capa.case.opened'], maxRetries: 2
    });
    c.submit();

    expect(svc.createSubscription).toHaveBeenCalledWith({
      name: 'ERP', endpointUrl: 'https://erp.example/hook',
      eventTypes: ['capa.case.opened'], secret: SECRET, maxRetries: 2, createdBy: 'u-42'
    });
    expect(dialogRef.close).toHaveBeenCalledWith({ subscription: subscription(), secret: SECRET });
  });

  it('n\'appelle pas le serveur sans session : `createdBy` est obligatoire côté API', async () => {
    user = null;
    const fixture = await setup({ subscription: null });
    const c = fixture.componentInstance;
    c.form.patchValue({
      name: 'ERP', endpointUrl: 'https://erp.example/hook', eventTypes: ['capa.case.opened']
    });

    c.submit();

    expect(svc.createSubscription).not.toHaveBeenCalled();
    expect(c.error).toContain('Session');
    expect(c.submitting).toBeFalse();
  });

  it('affiche une erreur sûre quand la création échoue et rouvre la saisie', async () => {
    svc.createSubscription.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 400 })));
    const fixture = await setup({ subscription: null });
    const c = fixture.componentInstance;
    c.form.patchValue({
      name: 'ERP', endpointUrl: 'https://erp.example/hook', eventTypes: ['capa.case.opened']
    });

    c.submit();

    expect(c.error).toBe('Champs invalides — vérifiez le formulaire.');
    expect(c.submitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('regénère un secret à la demande', async () => {
    const fixture = await setup({ subscription: null });
    svc.generateSecret.and.returnValue('b'.repeat(32));

    fixture.componentInstance.regenerateSecret();

    expect(fixture.componentInstance.form.controls.secret.value).toBe('b'.repeat(32));
    expect(fixture.componentInstance.form.controls.secret.dirty).toBeTrue();
  });

  // ---- Édition ---------------------------------------------------------------

  it('pré-remplit le formulaire et verrouille le secret en édition', async () => {
    const fixture = await setup({ subscription: subscription() });
    const c = fixture.componentInstance;

    expect(c.editing).toBeTrue();
    expect(c.form.controls.name.value).toBe('ERP');
    expect(c.form.controls.maxRetries.value).toBe(3);
    expect(c.form.controls.eventTypes.value).toEqual(['capa.case.opened', 'pdca.cycle.created']);
    expect(c.form.controls.secret.disabled).toBeTrue();
    expect(svc.generateSecret).not.toHaveBeenCalled();
  });

  it('signale et écarte les évènements inconnus, que le serveur refuserait', async () => {
    const fixture = await setup({
      subscription: subscription({ eventTypes: ['capa.case.opened', 'legacy.event.gone', ''] })
    });
    const c = fixture.componentInstance;

    expect(c.form.controls.eventTypes.value).toEqual(['capa.case.opened']);
    expect(c.droppedEventTypes).toEqual(['legacy.event.gone']);
  });

  it('met à jour l\'abonnement et ferme sans secret', async () => {
    const updated = subscription({ name: 'ERP v2' });
    svc.updateSubscription.and.returnValue(of(updated));
    const fixture = await setup({ subscription: subscription() });
    const c = fixture.componentInstance;

    c.form.patchValue({ name: 'ERP v2' });
    c.submit();

    expect(svc.updateSubscription).toHaveBeenCalledWith('s-1', {
      name: 'ERP v2', endpointUrl: 'https://erp.example/hook',
      eventTypes: ['capa.case.opened', 'pdca.cycle.created'], maxRetries: 3
    });
    expect(dialogRef.close).toHaveBeenCalledWith({ subscription: updated, secret: null });
  });

  it('affiche une erreur sûre quand la mise à jour échoue', async () => {
    svc.updateSubscription.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })));
    const fixture = await setup({ subscription: subscription() });

    fixture.componentInstance.submit();

    expect(fixture.componentInstance.error).toBe('Erreur serveur — réessayez dans un instant.');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('ignore un second envoi tant que le premier est en vol', async () => {
    const fixture = await setup({ subscription: subscription() });
    const c = fixture.componentInstance;
    c.submitting = true;

    c.submit();

    expect(svc.updateSubscription).not.toHaveBeenCalled();
  });

  it('libelle les groupes avec le préfixe wire des évènements listés', async () => {
    const fixture = await setup({ subscription: null });
    expect(fixture.componentInstance.groupLabel('pdca')).toBe('pdca.*');
  });

  it('ferme sans rien enregistrer à l\'annulation', async () => {
    const fixture = await setup({ subscription: null });
    fixture.componentInstance.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.createSubscription).not.toHaveBeenCalled();
  });
});
