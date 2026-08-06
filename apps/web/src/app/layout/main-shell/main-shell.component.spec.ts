import { Router } from '@angular/router';
import { NEVER, Observable, of } from 'rxjs';

import { TenantModulesService } from '../../features/admin/tenant-modules.service';

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
  modules: jasmine.SpyObj<TenantModulesService>;
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

  const modules = jasmine.createSpyObj<TenantModulesService>(
    'TenantModulesService', ['enabledCodes']);
  // Par défaut, tous les modules du catalogue : les tests de navigation qui ne
  // parlent pas de modules ne doivent pas voir leurs entrées disparaître.
  modules.enabledCodes.and.returnValue(of([
    'pdca', 'ishikawa', 'fives', 'circle', 'dmaic', 'capa', 'docs', 'audit',
    'risk', 'supplier', 'training', 'change', 'complaints', 'calibration',
    'ehs', 'standards', 'kpi', 'industry', 'iot', 'auditlog', 'blockchain',
    'webhooks', 'itsm'
  ]));

  return {
    component: new MainShellComponent(
      auth, connectivity, offline, notifications, router, modules),
    notifications, router, auth, modules
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

  it('expose huit groupes de navigation', () => {
    // Le septième est l'Administration ; le huitième, « Non-conformité », sorti
    // des Opérations pour porter ses deux origines (interne / externe).
    expect(component.sections.length).toBe(8);
  });

  it('orders the six groups as designed', () => {
    const labels = component.sections.map(s => s.items.length);
    // Pilotage(5), Méthodes(6), Analyses IA(8), Opérations(10), Référentiels(10),
    // GRC(1), Administration(6).
    // Opérations = 10 : + Réclamations (§4.9), Calibration (§4.10), Parc IoT (§9).
    // Référentiels = 10 : + Co-couverture IMS (§8.9).
    // GRC reste à 1 : le registre des systèmes d'IA rejoint la page hub /compliance,
    // pas la barre latérale — c'est tout l'objet de ce regroupement.
    // Administration = 7 : + Équipe & habilitations (§16) — le tenant habilite
    // lui-même son équipe qualité, sans passer par l'éditeur de la plateforme.
    // Non-conformité(2) s'intercale, et Opérations retombe à 9 : son entrée
    // « Non-conformités » unique est remplacée par les deux entrées du groupe.
    expect(labels).toEqual([5, 6, 8, 2, 9, 10, 1, 7]);
  });

  it('collapses the entire GRC mass into a single /compliance entry', () => {
    const grc = component.sections[6];
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
     '/nc/interne', '/nc/externe', '/capa', '/audits', '/standards', '/itsm', '/compliance']
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
 * Navigation filtrée par les modules du tenant.
 *
 * La barre latérale n'était filtrée que par RÔLE : un tenant réduit au socle
 * voyait donc toutes les entrées — IoT, Standards Hub, marketplace… — et
 * découvrait le refus en ouvrant l'écran. Montrer une porte fermée n'est pas
 * une information, c'est une déception.
 */
describe('MainShellComponent (navigation filtrée par module)', () => {

  /**
   * Dernière valeur émise, et non la première : la navigation s'affiche complète
   * le temps que les modules soient connus, puis se resserre. C'est l'état
   * stabilisé qui nous intéresse — tout est synchrone ici.
   */
  function routesFor(enabled: string[] | null, roles: string[] = []): string[] {
    const h = harness();
    h.auth.user.and.returnValue(of({
      userId: 'u', tenantId: 't', displayName: 'Demo', roles
    }));
    h.modules.enabledCodes.and.returnValue(of(enabled ?? []));
    h.component.ngOnInit();

    let routes: string[] = [];
    h.component.visibleSections$.subscribe(sections => {
      routes = sections.flatMap(s => s.items.map(i => i.route));
    });
    return routes;
  }

  it('masque les modules que le tenant n’a pas', () => {
    const routes = routesFor(['pdca', 'ishikawa', 'fives', 'capa', 'docs', 'audit']);

    expect(routes).toContain('/pdca');
    expect(routes).not.toContain('/iot');
    expect(routes).not.toContain('/standards');
  });

  it('montre un module dès qu’il est ouvert au tenant', () => {
    const routes = routesFor(['pdca', 'iot']);

    expect(routes).toContain('/iot');
  });

  it('garde les entrées qui ne dépendent d’aucun module', () => {
    // L'accueil, le tableau de bord ou la file de synchronisation ne sont pas
    // des modules facturés : les masquer laisserait une coquille vide.
    const routes = routesFor([]);

    expect(routes).toContain('/home');
    expect(routes).toContain('/dashboard');
  });

  it('ne masque rien tant que la liste des modules n’est pas connue', () => {
    // Pendant le chargement, mieux vaut une barre complète qu'une barre qui se
    // vide puis se remplit sous les yeux de l'utilisateur.
    const h = harness();
    h.auth.user.and.returnValue(of({
      userId: 'u', tenantId: 't', displayName: 'Demo', roles: []
    }));
    h.modules.enabledCodes.and.returnValue(NEVER);
    h.component.ngOnInit();

    let routes: string[] = [];
    h.component.visibleSections$.subscribe(s => {
      routes = s.flatMap(x => x.items.map(i => i.route));
    });

    expect(routes).toContain('/iot');
  });

  it('chaque entrée déclarant un module cite un code du catalogue', () => {
    // Une faute de frappe dans un code ferait disparaître l'entrée en silence.
    const known = [
      'pdca', 'ishikawa', 'fives', 'circle', 'dmaic', 'capa', 'docs', 'audit',
      'risk', 'supplier', 'training', 'change', 'complaints', 'calibration',
      'ehs', 'standards', 'kpi', 'industry', 'iot', 'auditlog', 'blockchain',
      'webhooks', 'itsm'
    ];
    const component = make();

    component.sections.flatMap(s => s.items).forEach(item => {
      if (item.module) {
        expect(known).withContext(`${item.route} → ${item.module}`).toContain(item.module);
      }
    });
  });
});

/**
 * Navigation sur écran étroit. Le shell était une grille à deux colonnes à TOUTES
 * les largeurs : sur un téléphone, la barre latérale mangeait les deux tiers de
 * l'écran, et la repliér laissait encore un rail de 64 px. Sous 1024 px elle
 * devient un tiroir superposé, fermé par défaut.
 */
describe('MainShellComponent (navigation en écran étroit)', () => {

  function compactHarness(compact: boolean): ShellHarness {
    const h = harness();
    h.component.onViewportChange(compact);
    return h;
  }

  it('garde le tiroir fermé au démarrage', () => {
    const { component } = compactHarness(true);
    expect(component.navOpen).toBeFalse();
  });

  it('ouvre le tiroir au lieu de replier le rail, en écran étroit', () => {
    const { component } = compactHarness(true);

    component.toggle();

    expect(component.navOpen).toBeTrue();
    // Le repli du rail n'a aucun sens ici : il ne doit pas être touché.
    expect(component.collapsed).toBeFalse();
  });

  it('replie le rail et ne touche pas au tiroir, en écran large', () => {
    const { component } = compactHarness(false);

    component.toggle();

    expect(component.collapsed).toBeTrue();
    expect(component.navOpen).toBeFalse();
  });

  it('referme le tiroir quand on suit un lien de navigation', () => {
    const { component } = compactHarness(true);
    component.toggle();

    component.onNavigate();

    expect(component.navOpen).toBeFalse();
  });

  it('referme le tiroir sur Échap', () => {
    const { component } = compactHarness(true);
    component.toggle();

    component.closeNav();

    expect(component.navOpen).toBeFalse();
  });

  it('referme le tiroir dès que la fenêtre redevient large', () => {
    const { component } = compactHarness(true);
    component.toggle();

    component.onViewportChange(false);

    expect(component.navOpen).toBeFalse();
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
    // Toutes les entrées d'administration sont gardées : la section disparaît, et
    // il reste les sept autres — Non-conformité comprise depuis qu'elle est
    // sortie des Opérations.
    const sections = make().filterSections(['USER']);
    expect(sections.length).toBe(7);
    expect(sections.some(s => s.items.some(i => i.route.startsWith('/admin')))).toBeFalse();
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
