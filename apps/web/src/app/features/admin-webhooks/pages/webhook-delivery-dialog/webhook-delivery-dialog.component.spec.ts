import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { WebhookDelivery } from '../../webhooks.types';
import {
  WebhookDeliveryDialogComponent, WebhookDeliveryDialogData
} from './webhook-delivery-dialog.component';

describe('WebhookDeliveryDialogComponent', () => {
  let dialogRef: jasmine.SpyObj<MatDialogRef<WebhookDeliveryDialogComponent>>;

  const delivery = (over: Partial<WebhookDelivery> = {}): WebhookDelivery => ({
    id: 'd-1', tenantId: 't-1', subscriptionId: 's-1', eventId: 'e-1',
    eventType: 'capa.case.opened', payload: '{"id":"e-1","type":"capa.case.opened"}',
    status: 'FAILED', attemptCount: 3, lastAttemptAt: '2026-07-01T10:00:00Z',
    nextRetryAt: null, responseStatusCode: 503, responseBody: 'upstream down',
    errorMessage: 'HTTP 503', createdAt: null, updatedAt: null, ...over
  });

  async function setup(data: WebhookDeliveryDialogData): Promise<ComponentFixture<WebhookDeliveryDialogComponent>> {
    await TestBed.configureTestingModule({
      declarations: [WebhookDeliveryDialogComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    return TestBed.createComponent(WebhookDeliveryDialogComponent);
  }

  beforeEach(() => {
    dialogRef = jasmine.createSpyObj<MatDialogRef<WebhookDeliveryDialogComponent>>('MatDialogRef', ['close']);
  });

  it('met en forme la charge utile JSON pour la rendre lisible', async () => {
    const fixture = await setup({ delivery: delivery(), subscriptionName: 'ERP' });
    expect(fixture.componentInstance.prettyPayload).toContain('\n');
    expect(fixture.componentInstance.prettyPayload).toContain('"type": "capa.case.opened"');
  });

  it('affiche la charge utile telle quelle si elle n\'est pas du JSON valide', async () => {
    const fixture = await setup({
      delivery: delivery({ payload: '{tronqué' }), subscriptionName: 'ERP'
    });
    expect(fixture.componentInstance.prettyPayload).toBe('{tronqué');
  });

  it('tolère une charge utile vide', async () => {
    const fixture = await setup({
      delivery: delivery({ payload: '' }), subscriptionName: 'ERP'
    });
    expect(fixture.componentInstance.prettyPayload).toBe('');
  });

  it('montre erreur, réponse et code HTTP — les trois faits du diagnostic', async () => {
    const fixture = await setup({ delivery: delivery(), subscriptionName: 'ERP' });
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('HTTP 503');
    expect(text).toContain('upstream down');
    expect(text).toContain('503');
    expect(text).toContain('ERP');
  });

  it('retombe sur l\'identifiant quand le nom d\'abonnement est inconnu', async () => {
    const fixture = await setup({ delivery: delivery(), subscriptionName: null });
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('s-1');
  });

  it('indique explicitement l\'absence de corps de réponse', async () => {
    const fixture = await setup({
      delivery: delivery({ responseBody: null }), subscriptionName: 'ERP'
    });
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Aucun corps de réponse');
  });

  it('ferme le dialogue', async () => {
    const fixture = await setup({ delivery: delivery(), subscriptionName: 'ERP' });
    fixture.componentInstance.close();
    expect(dialogRef.close).toHaveBeenCalled();
  });
});
