import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Observable, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { WebhooksService } from '../../webhooks.service';
import { WebhookDelivery, WebhookPage, WebhookSubscription } from '../../webhooks.types';
import { WebhookDeliveryDialogComponent } from '../webhook-delivery-dialog/webhook-delivery-dialog.component';
import { WebhookFormDialogComponent } from '../webhook-form-dialog/webhook-form-dialog.component';
import { WebhookSecretDialogComponent } from '../webhook-secret-dialog/webhook-secret-dialog.component';
import { WebhooksListComponent } from './webhooks-list.component';

describe('WebhooksListComponent', () => {
  let component: WebhooksListComponent;
  let fixture: ComponentFixture<WebhooksListComponent>;
  let svc: jasmine.SpyObj<WebhooksService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const SECRET = 'e'.repeat(64);

  const subscription = (over: Partial<WebhookSubscription> = {}): WebhookSubscription => ({
    id: 's-1', tenantId: 't-1', name: 'ERP', endpointUrl: 'https://erp.example/hook',
    eventTypes: ['capa.case.opened', 'pdca.cycle.created', 'audit.finding.raised', 'fives.audit.completed'],
    status: 'ACTIVE', maxRetries: 5, consecutiveFailures: 0, lastTriggeredAt: null,
    lastSuccessAt: '2026-07-01T09:00:00Z', createdBy: 'u-1', createdAt: null, updatedAt: null, ...over
  });

  const delivery = (over: Partial<WebhookDelivery> = {}): WebhookDelivery => ({
    id: 'd-1', tenantId: 't-1', subscriptionId: 's-1', eventId: 'e-1',
    eventType: 'capa.case.opened', payload: '{}', status: 'FAILED', attemptCount: 2,
    lastAttemptAt: '2026-07-01T10:00:00Z', nextRetryAt: null, responseStatusCode: 500,
    responseBody: null, errorMessage: 'boom', createdAt: null, updatedAt: null, ...over
  });

  const pageOf = <T>(content: T[], total = content.length): WebhookPage<T> => ({
    content, totalElements: total, totalPages: 1, number: 0, size: 20
  });

  /** Fait répondre le prochain `dialog.open(...)` avec la valeur fermée voulue. */
  function dialogCloses(value: unknown): void {
    dialog.open.and.returnValue({
      afterClosed: () => of(value) as Observable<unknown>
    } as unknown as MatDialogRef<unknown>);
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<WebhooksService>('WebhooksService', [
      'listSubscriptions', 'createSubscription', 'updateSubscription', 'deleteSubscription',
      'testPing', 'listDeliveries', 'retryDelivery', 'generateSecret'
    ]);
    svc.listSubscriptions.and.returnValue(of(pageOf([subscription()])));
    svc.listDeliveries.and.returnValue(of(pageOf([delivery()])));
    svc.updateSubscription.and.returnValue(of(subscription({ status: 'PAUSED' })));
    svc.deleteSubscription.and.returnValue(of(undefined));
    svc.testPing.and.returnValue(of({
      deliveryId: 'd-9', status: 'SUCCESS', responseStatusCode: 200, errorMessage: null
    }));
    svc.retryDelivery.and.returnValue(of(delivery({ status: 'SUCCESS', responseStatusCode: 200 })));
    svc.generateSecret.and.returnValue(SECRET);

    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialogCloses(undefined);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [WebhooksListComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        { provide: WebhooksService, useValue: svc },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WebhooksListComponent);
    component = fixture.componentInstance;
  });

  // ---- Chargement initial ----------------------------------------------------

  it('charge les abonnements et les livraisons dès l\'affichage', () => {
    fixture.detectChanges();
    expect(svc.listSubscriptions).toHaveBeenCalledWith(0, 20);
    expect(svc.listDeliveries).toHaveBeenCalledWith({
      subscriptionId: null, status: null, page: 0, size: 25
    });
  });

  it('rend une ligne par abonnement avec ses évènements tronqués au-delà de trois', () => {
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelectorAll('tbody tr').length).toBeGreaterThan(0);
    expect(host.textContent).toContain('ERP');
    expect(host.textContent).toContain('+1');
  });

  it('affiche les compteurs quand la page couvre tout le jeu', (done) => {
    svc.listSubscriptions.and.returnValue(of(pageOf([
      subscription({ id: 's-1', status: 'ACTIVE' }),
      subscription({ id: 's-2', status: 'PAUSED' }),
      subscription({ id: 's-3', status: 'DISABLED_ON_ERRORS', consecutiveFailures: 10 })
    ])));
    fixture.detectChanges();
    component.subscriptions$.subscribe(view => {
      expect(view.complete).toBeTrue();
      expect(view.total).toBe(3);
      expect(view.activeCount).toBe(1);
      expect(view.pausedCount).toBe(1);
      expect(view.disabledCount).toBe(1);
      expect(view.failingCount).toBe(1);
      done();
    });
  });

  it('masque les compteurs quand la page ne couvre pas tout le jeu : ils mentiraient', (done) => {
    svc.listSubscriptions.and.returnValue(of(pageOf([subscription()], 42)));
    fixture.detectChanges();
    component.subscriptions$.subscribe(view => {
      expect(view.complete).toBeFalse();
      expect(view.total).toBe(42);
      done();
    });
  });

  it('survit à une page serveur incomplète sans planter l\'écran', (done) => {
    svc.listSubscriptions.and.returnValue(of({} as WebhookPage<WebhookSubscription>));
    fixture.detectChanges();
    component.subscriptions$.subscribe(view => {
      expect(view.rows).toEqual([]);
      expect(view.total).toBe(0);
      expect(view.complete).toBeTrue();
      done();
    });
  });

  it('affiche un message sûr et vide la liste quand le chargement échoue', fakeAsync(() => {
    svc.listSubscriptions.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Vous n\'avez pas les droits pour cette action.');
  }));

  it('signale séparément l\'échec du chargement des livraisons', fakeAsync(() => {
    svc.listDeliveries.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Erreur serveur — réessayez dans un instant.');
  }));

  // ---- Cycle de vie des abonnements ------------------------------------------

  it('met en pause un abonnement actif et recharge la liste', () => {
    fixture.detectChanges();
    const before = svc.listSubscriptions.calls.count();

    component.pause(subscription());

    expect(svc.updateSubscription).toHaveBeenCalledWith('s-1', { status: 'PAUSED' });
    expect(svc.listSubscriptions.calls.count()).toBe(before + 1);
    expect(snack.open).toHaveBeenCalled();
  });

  it('réactive un abonnement en pause ou désactivé sur erreurs', () => {
    fixture.detectChanges();
    svc.updateSubscription.and.returnValue(of(subscription({ status: 'ACTIVE' })));

    component.resume(subscription({ status: 'DISABLED_ON_ERRORS' }));

    expect(svc.updateSubscription).toHaveBeenCalledWith('s-1', { status: 'ACTIVE' });
    expect(component.canResume(subscription({ status: 'PAUSED' }))).toBeTrue();
    expect(component.canResume(subscription({ status: 'DISABLED_ON_ERRORS' }))).toBeTrue();
    expect(component.canResume(subscription({ status: 'ACTIVE' }))).toBeFalse();
  });

  it('remonte un message sûr quand une transition échoue', () => {
    fixture.detectChanges();
    svc.updateSubscription.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    component.pause(subscription());

    expect(snack.open).toHaveBeenCalledWith('État incompatible — rechargez la page.',
      jasmine.any(String), jasmine.any(Object));
  });

  it('ignore une seconde action tant que la première est en vol', () => {
    fixture.detectChanges();
    component.pendingSubscriptionId = 's-1';

    component.pause(subscription());
    component.test(subscription());
    component.rotateSecret(subscription());
    component.remove(subscription());

    expect(svc.updateSubscription).not.toHaveBeenCalled();
    expect(svc.testPing).not.toHaveBeenCalled();
    expect(svc.deleteSubscription).not.toHaveBeenCalled();
    // Aucune confirmation ne doit non plus s'ouvrir : le clic est purement inopérant.
    expect(dialog.open).not.toHaveBeenCalled();
  });

  // ---- Ping de test ----------------------------------------------------------

  it('envoie un ping de test et rafraîchit les deux plans', () => {
    fixture.detectChanges();
    const subsBefore = svc.listSubscriptions.calls.count();
    const delBefore = svc.listDeliveries.calls.count();

    component.test(subscription());

    expect(svc.testPing).toHaveBeenCalledWith('s-1');
    expect(svc.listSubscriptions.calls.count()).toBe(subsBefore + 1);
    expect(svc.listDeliveries.calls.count()).toBe(delBefore + 1);
    expect(snack.open.calls.mostRecent().args[0] as string).toContain('accepté');
  });

  it('distingue un ping rejeté d\'un ping accepté', () => {
    svc.testPing.and.returnValue(of({
      deliveryId: 'd-9', status: 'DEAD_LETTER', responseStatusCode: 500, errorMessage: 'nope'
    }));
    fixture.detectChanges();

    component.test(subscription());

    expect(snack.open.calls.mostRecent().args[0] as string).toContain('rejeté');
  });

  it('remonte un message sûr quand le ping lui-même échoue', () => {
    svc.testPing.and.returnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    fixture.detectChanges();

    component.test(subscription());

    expect(snack.open.calls.mostRecent().args[0] as string)
      .toBe('Service inaccessible — vérifiez votre connexion.');
    expect(component.pendingSubscriptionId).toBeNull();
  });

  // ---- Rotation du secret ----------------------------------------------------

  it('ne touche à rien si la rotation du secret n\'est pas confirmée', () => {
    dialogCloses(false);
    fixture.detectChanges();

    component.rotateSecret(subscription());

    expect(svc.updateSubscription).not.toHaveBeenCalled();
  });

  it('pousse un secret neuf puis le montre une seule fois', () => {
    dialogCloses(true);
    svc.updateSubscription.and.returnValue(of(subscription()));
    fixture.detectChanges();

    component.rotateSecret(subscription());

    expect(svc.generateSecret).toHaveBeenCalled();
    expect(svc.updateSubscription).toHaveBeenCalledWith('s-1', { secret: SECRET });
    const lastOpen = dialog.open.calls.mostRecent().args;
    expect(lastOpen[0]).toBe(WebhookSecretDialogComponent);
    expect((lastOpen[1] as { data: { rotated: boolean; secret: string } }).data)
      .toEqual(jasmine.objectContaining({ secret: SECRET, rotated: true }));
  });

  it('remonte un message sûr quand la rotation échoue', () => {
    dialogCloses(true);
    svc.updateSubscription.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    fixture.detectChanges();

    component.rotateSecret(subscription());

    expect(snack.open.calls.mostRecent().args[0] as string)
      .toBe('Erreur serveur — réessayez dans un instant.');
  });

  // ---- Suppression -----------------------------------------------------------

  it('ne supprime rien sans confirmation', () => {
    dialogCloses(false);
    fixture.detectChanges();

    component.remove(subscription());

    expect(svc.deleteSubscription).not.toHaveBeenCalled();
  });

  it('supprime et relâche le filtre qui pointait sur l\'abonnement disparu', () => {
    dialogCloses(true);
    fixture.detectChanges();
    component.onSubscriptionFilterChange('s-1');

    component.remove(subscription());

    expect(svc.deleteSubscription).toHaveBeenCalledWith('s-1');
    expect(component.deliveryQuery.subscriptionId).toBeNull();
    expect(svc.listDeliveries.calls.mostRecent().args[0].subscriptionId).toBeNull();
  });

  it('remonte un message sûr quand la suppression échoue', () => {
    dialogCloses(true);
    svc.deleteSubscription.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    fixture.detectChanges();

    component.remove(subscription());

    expect(snack.open.calls.mostRecent().args[0] as string).toBe('Erreur lors de la suppression.');
  });

  // ---- Création / édition ----------------------------------------------------

  it('ouvre le formulaire de création et révèle le secret retourné', () => {
    dialogCloses({ subscription: subscription(), secret: SECRET });
    fixture.detectChanges();
    const before = svc.listSubscriptions.calls.count();

    component.create();

    expect(dialog.open.calls.first().args[0]).toBe(WebhookFormDialogComponent);
    expect(svc.listSubscriptions.calls.count()).toBe(before + 1);
    expect(dialog.open.calls.mostRecent().args[0]).toBe(WebhookSecretDialogComponent);
  });

  it('confirme une édition sans jamais afficher de secret', () => {
    dialogCloses({ subscription: subscription(), secret: null });
    fixture.detectChanges();

    component.edit(subscription());

    expect(dialog.open.calls.mostRecent().args[0]).toBe(WebhookFormDialogComponent);
    expect(snack.open.calls.mostRecent().args[0] as string).toContain('mis à jour');
  });

  it('ne recharge rien si le formulaire est annulé', () => {
    dialogCloses(undefined);
    fixture.detectChanges();
    const before = svc.listSubscriptions.calls.count();

    component.create();

    expect(svc.listSubscriptions.calls.count()).toBe(before);
    expect(snack.open).not.toHaveBeenCalled();
  });

  // ---- Livraisons ------------------------------------------------------------

  it('pré-filtre les livraisons sur un abonnement depuis sa ligne', () => {
    fixture.detectChanges();

    component.showDeliveriesFor(subscription({ id: 's-7' }));

    expect(component.deliveryQuery.subscriptionId).toBe('s-7');
    expect(component.deliveryQuery.page).toBe(0);
    expect(svc.listDeliveries.calls.mostRecent().args[0].subscriptionId).toBe('s-7');
  });

  it('neutralise le filtre de statut tant qu\'un abonnement est sélectionné', () => {
    fixture.detectChanges();
    expect(component.statusFilterDisabled).toBeFalse();

    component.onSubscriptionFilterChange('s-1');

    expect(component.statusFilterDisabled).toBeTrue();
  });

  it('filtre par statut et revient à la première page', () => {
    fixture.detectChanges();
    component.onDeliveryPage({ pageIndex: 3, pageSize: 25, length: 100 } as PageEvent);

    component.onStatusFilterChange('DEAD_LETTER');

    expect(component.deliveryQuery.status).toBe('DEAD_LETTER');
    expect(component.deliveryQuery.page).toBe(0);
    expect(svc.listDeliveries.calls.mostRecent().args[0].status).toBe('DEAD_LETTER');
  });

  it('propage la pagination des deux tableaux', () => {
    fixture.detectChanges();

    component.onDeliveryPage({ pageIndex: 2, pageSize: 50, length: 200 } as PageEvent);
    component.onSubscriptionsPage({ pageIndex: 1, pageSize: 10, length: 40 } as PageEvent);

    expect(svc.listDeliveries.calls.mostRecent().args[0]).toEqual(
      jasmine.objectContaining({ page: 2, size: 50 }));
    expect(svc.listSubscriptions).toHaveBeenCalledWith(1, 10);
    expect(component.subsPageIndex).toBe(1);
    expect(component.subsPageSize).toBe(10);
  });

  it('ne propose pas de rejouer une livraison déjà réussie', () => {
    expect(component.canRetry(delivery({ status: 'SUCCESS' }))).toBeFalse();
    expect(component.canRetry(delivery({ status: 'DEAD_LETTER' }))).toBeTrue();
  });

  it('rejoue une livraison et rafraîchit les deux plans', () => {
    fixture.detectChanges();
    const subsBefore = svc.listSubscriptions.calls.count();
    const delBefore = svc.listDeliveries.calls.count();

    component.retry(delivery());

    expect(svc.retryDelivery).toHaveBeenCalledWith('d-1');
    expect(svc.listDeliveries.calls.count()).toBe(delBefore + 1);
    expect(svc.listSubscriptions.calls.count()).toBe(subsBefore + 1);
    expect(snack.open.calls.mostRecent().args[0] as string).toContain('succès');
  });

  it('annonce un rejeu qui échoue à nouveau', () => {
    svc.retryDelivery.and.returnValue(of(delivery({ status: 'DEAD_LETTER' })));
    fixture.detectChanges();

    component.retry(delivery());

    expect(snack.open.calls.mostRecent().args[0] as string).toContain('échouée');
  });

  it('remonte un message sûr quand le rejeu est refusé', () => {
    svc.retryDelivery.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    fixture.detectChanges();

    component.retry(delivery());

    expect(snack.open.calls.mostRecent().args[0] as string).toBe('État incompatible — rechargez la page.');
    expect(component.pendingDeliveryId).toBeNull();
  });

  it('ignore un second rejeu tant que le premier est en vol', () => {
    fixture.detectChanges();
    component.pendingDeliveryId = 'd-1';

    component.retry(delivery());

    expect(svc.retryDelivery).not.toHaveBeenCalled();
  });

  it('ouvre le détail d\'une livraison avec le nom d\'abonnement résolu', () => {
    fixture.detectChanges();

    component.openDelivery(delivery());

    const args = dialog.open.calls.mostRecent().args;
    expect(args[0]).toBe(WebhookDeliveryDialogComponent);
    expect((args[1] as { data: { subscriptionName: string | null } }).data.subscriptionName).toBe('ERP');
  });

  it('retombe sur null quand l\'abonnement n\'est pas dans la page chargée', () => {
    fixture.detectChanges();
    expect(component.subscriptionName('inconnu')).toBeNull();
    expect(component.subscriptionOptions.length).toBe(1);
  });

  // ---- Présentation ----------------------------------------------------------

  it('traduit et colore les statuts d\'abonnement', () => {
    expect(component.statusLabel('ACTIVE')).toBe('Actif');
    expect(component.statusLabel('PAUSED')).toBe('En pause');
    expect(component.statusLabel('DISABLED_ON_ERRORS')).toBe('Désactivé sur erreurs');
    expect(component.statusTone('ACTIVE')).toBe('success');
    expect(component.statusTone('PAUSED')).toBe('warn');
    expect(component.statusTone('DISABLED_ON_ERRORS')).toBe('danger');
  });

  it('traduit et colore les statuts de livraison', () => {
    expect(component.deliveryStatusLabel('SUCCESS')).toBe('Succès');
    expect(component.deliveryStatusLabel('DEAD_LETTER')).toBe('Abandonné');
    // Statut inconnu (référentiel serveur enrichi) : on affiche le code brut plutôt que rien.
    expect(component.deliveryStatusLabel('FUTUR' as never)).toBe('FUTUR');
    expect(component.deliveryStatusTone('SUCCESS')).toBe('success');
    expect(component.deliveryStatusTone('RETRYING')).toBe('warn');
    expect(component.deliveryStatusTone('PENDING')).toBe('neutral');
    expect(component.deliveryStatusTone('FAILED')).toBe('danger');
  });

  it('marque les abonnements en échec consécutif', () => {
    expect(component.isFailing(subscription({ consecutiveFailures: 0 }))).toBeFalse();
    expect(component.isFailing(subscription({ consecutiveFailures: 3 }))).toBeTrue();
  });

  it('identifie les lignes par leur identifiant pour éviter de recréer le DOM', () => {
    expect(component.trackBySubscription(0, subscription())).toBe('s-1');
    expect(component.trackByDelivery(0, delivery())).toBe('d-1');
  });

  it('affiche un état vide actionnable quand aucun abonnement n\'existe', fakeAsync(() => {
    svc.listSubscriptions.and.returnValue(of(pageOf([])));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Aucun abonnement');
  }));

  it('affiche un état vide pour des livraisons sans résultat', fakeAsync(() => {
    svc.listDeliveries.and.returnValue(of(pageOf([])));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Aucune livraison pour ces critères.');
  }));
});
