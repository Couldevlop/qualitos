import { Router } from '@angular/router';
import { Observable, of } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { NotificationView } from '../../core/notifications/notification.types';
import { NotificationsService } from '../../core/notifications/notifications.service';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { OfflineQueueService } from '../../core/offline/offline-queue.service';
import { MainShellComponent } from './main-shell.component';

/** Dépendances observables du shell, exposées aux tests qui les inspectent. */
interface ShellHarness {
  component: MainShellComponent;
  notifications: jasmine.SpyObj<NotificationsService>;
  router: jasmine.SpyObj<Router>;
  auth: jasmine.SpyObj<AuthService>;
}

function notification(overrides: Partial<NotificationView> = {}): NotificationView {
  return {
    id: 'n1', type: 'INFO', title: 'Nouvelle CAPA', body: 'Assignée ce matin',
    link: '/capa/abc', read: false, createdAt: '2026-05-15T08:00:00Z', readAt: null,
    ...overrides
  };
}

/** Construit le composant avec des dépendances stub (test de logique pure). */
function harness(): ShellHarness {
  const auth = jasmine.createSpyObj<AuthService>('AuthService', ['user', 'logout']);
  auth.user.and.returnValue(of(null));

  const connectivity = { online$: of(true) } as unknown as ConnectivityService;
  const offline = { pendingCount$: of(0) } as unknown as OfflineQueueService;

  const notifications = jasmine.createSpyObj<NotificationsService>(
    'NotificationsService',
    ['recent', 'refreshUnreadCount', 'markRead', 'markAllRead'],
    { unread$: of(0) });
  notifications.recent.and.returnValue(of([]));
  notifications.markRead.and.returnValue(of(null));
  notifications.markAllRead.and.returnValue(of(0));

  const router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
  router.navigateByUrl.and.returnValue(Promise.resolve(true));

  return {
    component: new MainShellComponent(auth, connectivity, offline, notifications, router),
    notifications, router, auth
  };
}

function make(): MainShellComponent {
  return harness().component;
}

describe('MainShellComponent (navigation model)', () => {
  let component: MainShellComponent;

  beforeEach(() => {
    localStorage.clear();
    component = make();
  });

  it('exposes exactly seven navigation groups', () => {
    // Le septième groupe est l'Administration, réservée aux rôles d'administration.
    expect(component.sections.length).toBe(7);
  });

  it('orders the six groups as designed', () => {
    const labels = component.sections.map(s => s.items.length);
    // Pilotage(5), Méthodes(6), Analyses IA(8), Opérations(7), Référentiels(9),
    // GRC(1), Administration(1).
    expect(labels).toEqual([5, 6, 8, 7, 9, 1, 1]);
  });

  it('collapses the entire GRC mass into a single /compliance entry', () => {
    const grc = component.sections[5];
    expect(grc.items.length).toBe(1);
    expect(grc.items[0].route).toBe('/compliance');
  });

  it('keeps no raw GRC route in the global sidebar', () => {
    const grcRoutes = [
      '/ai-qms', '/ropa', '/consents', '/dpia', '/breaches',
      '/nis2-measures', '/cyber-incidents', '/ai-conformity'
    ];
    const allRoutes = component.sections.flatMap(s => s.items.map(i => i.route));
    grcRoutes.forEach(r => expect(allRoutes).withContext(r).not.toContain(r));
  });

  it('every nav item carries a route and a Material icon', () => {
    component.sections.forEach(s =>
      s.items.forEach(i => {
        expect(i.route).toBeTruthy();
        expect(i.icon).toBeTruthy();
      }));
  });

  it('keeps all core method/operation routes reachable from the sidebar', () => {
    const allRoutes = component.sections.flatMap(s => s.items.map(i => i.route));
    ['/home', '/dashboard', '/pdca', '/ishikawa', '/fives', '/dmaic', '/spc',
     '/nc', '/capa', '/audits', '/standards', '/itsm', '/compliance']
      .forEach(r => expect(allRoutes).withContext(r).toContain(r));
  });

  it('persists collapsed groups to localStorage and restores them', () => {
    component.ngOnInit();
    const label = component.sections[2].label;
    component.toggleSection(label);
    expect(component.isSectionCollapsed(label)).toBeTrue();

    const restored = make();
    restored.ngOnInit();
    expect(restored.isSectionCollapsed(label)).toBeTrue();
  });

  it('toggles a collapsed group back open', () => {
    const label = component.sections[0].label;
    component.toggleSection(label);
    component.toggleSection(label);
    expect(component.isSectionCollapsed(label)).toBeFalse();
  });

  it('computes initials from a display name', () => {
    expect(component.initials('Marie Curie')).toBe('MC');
    expect(component.initials('')).toBe('··');
    expect(component.initials(null)).toBe('··');
  });

  it('toggles the rail collapsed state', () => {
    expect(component.collapsed).toBeFalse();
    component.toggle();
    expect(component.collapsed).toBeTrue();
  });
});

/**
 * La cloche de notifications et le bouton de compte étaient deux boutons inertes :
 * aucun menu, aucune action. Ces tests verrouillent leur comportement.
 */
describe('MainShellComponent (notifications et compte)', () => {

  it('demande le compteur de non-lues dès le démarrage, sans charger la liste', () => {
    const h = harness();
    h.component.ngOnInit();

    expect(h.notifications.refreshUnreadCount).toHaveBeenCalledTimes(1);
    expect(h.notifications.recent).not.toHaveBeenCalled();
  });

  it('charge les notifications à l’ouverture du menu', () => {
    const h = harness();
    h.notifications.recent.and.returnValue(of([notification()]));

    h.component.openNotifications();

    expect(h.component.notifications.length).toBe(1);
    expect(h.component.notificationsLoading).toBeFalse();
  });

  it('sort de l’état de chargement même si le service échoue', () => {
    const h = harness();
    h.notifications.recent.and.returnValue(
      new Observable<NotificationView[]>(sub => sub.error(new Error('boom'))));

    h.component.openNotifications();

    expect(h.component.notificationsLoading).toBeFalse();
  });

  it('marque comme lue et ouvre la ressource liée', () => {
    const h = harness();
    const n = notification();

    h.component.onNotificationClick(n);

    expect(h.notifications.markRead).toHaveBeenCalledWith('n1');
    expect(n.read).toBeTrue();
    expect(h.router.navigateByUrl).toHaveBeenCalledWith('/capa/abc');
  });

  it('ne remarque pas comme lue une notification déjà lue', () => {
    const h = harness();

    h.component.onNotificationClick(notification({ read: true }));

    expect(h.notifications.markRead).not.toHaveBeenCalled();
    expect(h.router.navigateByUrl).toHaveBeenCalled();
  });

  it('reste cliquable sans lien : la notification est simplement marquée lue', () => {
    const h = harness();

    h.component.onNotificationClick(notification({ link: null }));

    expect(h.notifications.markRead).toHaveBeenCalled();
    expect(h.router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('marque tout comme lu et rafraîchit l’affichage', () => {
    const h = harness();
    h.notifications.recent.and.returnValue(of([notification(), notification({ id: 'n2' })]));
    h.component.openNotifications();

    h.component.markAllNotificationsRead();

    expect(h.component.notifications.every(n => n.read)).toBeTrue();
  });

  it('associe une icône à chaque nature de notification', () => {
    const c = make();
    expect(c.notificationIcon('SUCCESS')).toBe('check_circle');
    expect(c.notificationIcon('WARNING')).toBe('warning');
    expect(c.notificationIcon('ALERT')).toBe('error');
    expect(c.notificationIcon('INFO')).toBe('info');
  });

  it('déconnecte l’utilisateur depuis le menu de compte', () => {
    const h = harness();

    h.component.logout();

    expect(h.auth.logout).toHaveBeenCalledTimes(1);
  });
});

/**
 * Filtrage par rôle : afficher un lien d'administration à un utilisateur qui n'y a pas
 * droit lui promet un 403. Le serveur reste l'autorité, ceci n'est qu'un confort.
 */
describe('MainShellComponent (visibilité par rôle)', () => {

  it('masque l’administration pour un utilisateur sans rôle d’admin', () => {
    const sections = make().filterSections(['USER', 'AUDITOR']);
    const routes = sections.flatMap(s => s.items.map(i => i.route));
    expect(routes).not.toContain('/admin/modules');
  });

  it('supprime la section entière plutôt que d’afficher un titre orphelin', () => {
    const sections = make().filterSections(['USER']);
    expect(sections.length).toBe(6);
  });

  it('affiche l’administration pour un admin de tenant', () => {
    const sections = make().filterSections(['ADMIN_TENANT']);
    const routes = sections.flatMap(s => s.items.map(i => i.route));
    expect(routes).toContain('/admin/modules');
  });

  it('accepte le préfixe ROLE_ posé par Keycloak', () => {
    const sections = make().filterSections(['role_super_admin']);
    const routes = sections.flatMap(s => s.items.map(i => i.route));
    expect(routes).toContain('/admin/modules');
  });

  it('laisse passer toutes les entrées non gardées', () => {
    const sections = make().filterSections([]);
    const routes = sections.flatMap(s => s.items.map(i => i.route));
    expect(routes).toContain('/pdca');
    expect(routes).toContain('/compliance');
  });
});
