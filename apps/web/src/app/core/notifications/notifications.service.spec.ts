import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { NotificationView } from './notification.types';
import { NotificationsService } from './notifications.service';

/**
 * L'API `/api/v1/notifications` (5 routes) n'avait aucun consommateur : la cloche de la
 * barre supérieure était décorative. Ces tests couvrent le service qui la branche, y
 * compris sa dégradation — un service annexe ne doit jamais casser la barre supérieure.
 */
describe('NotificationsService', () => {
  let service: NotificationsService;
  let http: HttpTestingController;

  const endpoint = `${environment.apiBaseUrl}/api/v1/notifications`;

  function notification(overrides: Partial<NotificationView> = {}): NotificationView {
    return {
      id: 'n1', type: 'INFO', title: 'Nouvelle CAPA', body: null, link: '/capa/1',
      read: false, createdAt: '2026-05-15T08:00:00Z', readAt: null, ...overrides
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(NotificationsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('charge les dernières notifications avec une limite', (done) => {
    service.recent(5).subscribe(list => {
      expect(list.length).toBe(1);
      done();
    });

    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('limit')).toBe('5');
    req.flush([notification()]);
  });

  it('déduit le compteur de non-lues de la liste chargée', (done) => {
    service.recent().subscribe(() => {
      service.unread$.subscribe(unread => {
        expect(unread).toBe(2);
        done();
      });
    });

    http.expectOne(r => r.url === endpoint).flush([
      notification({ id: 'a', read: false }),
      notification({ id: 'b', read: true }),
      notification({ id: 'c', read: false })
    ]);
  });

  it('renvoie une liste vide plutôt que d’échouer si le service est indisponible', (done) => {
    service.recent().subscribe(list => {
      expect(list).toEqual([]);
      done();
    });

    http.expectOne(r => r.url === endpoint)
      .flush('down', { status: 503, statusText: 'Service Unavailable' });
  });

  it('rafraîchit la pastille sans charger la liste', (done) => {
    service.refreshUnreadCount();
    http.expectOne(`${endpoint}/unread-count`).flush({ unread: 7 });

    service.unread$.subscribe(unread => {
      expect(unread).toBe(7);
      done();
    });
  });

  it('remet la pastille à zéro si le compteur est injoignable', (done) => {
    service.refreshUnreadCount();
    http.expectOne(`${endpoint}/unread-count`)
      .flush('down', { status: 500, statusText: 'Server Error' });

    service.unread$.subscribe(unread => {
      expect(unread).toBe(0);
      done();
    });
  });

  it('décrémente la pastille au marquage d’une notification', (done) => {
    service.refreshUnreadCount();
    http.expectOne(`${endpoint}/unread-count`).flush({ unread: 3 });

    service.markRead('n1').subscribe(() => {
      service.unread$.subscribe(unread => {
        expect(unread).toBe(2);
        done();
      });
    });
    http.expectOne(`${endpoint}/n1/read`).flush(notification({ read: true }));
  });

  it('ne descend jamais sous zéro', (done) => {
    service.markRead('n1').subscribe(() => {
      service.unread$.subscribe(unread => {
        expect(unread).toBe(0);
        done();
      });
    });
    http.expectOne(`${endpoint}/n1/read`).flush(notification({ read: true }));
  });

  it('remet la pastille au compte renvoyé par « tout marquer comme lu »', (done) => {
    service.markAllRead().subscribe(unread => {
      expect(unread).toBe(0);
      done();
    });
    http.expectOne(`${endpoint}/read-all`).flush({ unread: 0 });
  });

  it('absorbe un échec de « tout marquer comme lu »', (done) => {
    service.markAllRead().subscribe(unread => {
      expect(unread).toBe(0);
      done();
    });
    http.expectOne(`${endpoint}/read-all`)
      .flush('down', { status: 500, statusText: 'Server Error' });
  });
});
