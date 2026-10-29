import { NavigationEnd, Router } from '@angular/router';
import { NEVER, Observable, Subject, of } from 'rxjs';

import { EditionService } from '../../core/edition/edition.service';
import { Edition } from '../../core/edition/edition.types';
import { TenantModulesService } from '../../features/admin/tenant-modules.service';

import { AuthService } from '../../core/auth/auth.service';
import { NotificationView } from '../../core/notifications/notification.types';
import { NotificationsService } from '../../core/notifications/notifications.service';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { OfflineQueueService } from '../../core/offline/offline-queue.service';
import { ThemePreference, ThemeService } from '../../core/theme/theme.service';
import { MainShellComponent } from './main-shell.component';

/** Dépendances observables du shell, exposées aux tests qui les inspectent. */
interface ShellHarness {
  component: MainShellComponent;
  notifications: jasmine.SpyObj<NotificationsService>;
  router: jasmine.SpyObj<Router>;
  /** Flux d'événements du routeur simulé : on y pousse les fins de navigation. */
  events: Subject<unknown>;
  auth: jasmine.SpyObj<AuthService>;
  modules: jasmine.SpyObj<TenantModulesService>;
  theme: jasmine.SpyObj<ThemeService>;
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
  // Propriétés simples (et non des espions d'accesseur) : les bancs posent
  // l'adresse courante par affectation.
  const events = new Subject<unknown>();
  Object.assign(router, { events, url: '/home' });

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

  const editions = jasmine.createSpyObj<EditionService>('EditionService', ['edition']);
  editions.edition.and.returnValue(of<Edition>('SAAS'));

  const theme = jasmine.createSpyObj<ThemeService>('ThemeService', ['preference', 'setPreference']);
  theme.preference.and.returnValue(of<ThemePreference>('system'));

  return {
    component: new MainShellComponent(
      auth, connectivity, offline, notifications, router, modules, editions, theme),
    notifications, router, auth, modules, events, theme
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

  it('expose onze groupes de navigation', () => {
    // Le septième est l'Administration ; le huitième, « Non-conformité », sorti
    // des Opérations pour porter ses deux origines (interne / externe). Le
    // dixième, « Risques & opportunités », suit la non-conformité. Le onzième,
    // « SPC », suit les Analyses IA (ADR 0096).
    expect(component.sections.length).toBe(11);
  });

  it('une seule entree s’allume, meme quand une route en prefixe une autre', () => {
    const h = harness();
    const shell = h.component;
    // Le montage commun fabrique un routeur simule : on lui pose l'adresse
    // courante, qui est la seule entree de la regle.
    const routeur = h.router as unknown as { url: string };

    // Sur l'efficacite des CAPA, « CAPA » (/capa) prefixe /capa/efficacite :
    // sans la regle de la correspondance la plus longue, les deux s'allumaient
    // et le menu disait qu'on etait a deux endroits a la fois.
    routeur.url = '/capa/efficacite';
    expect(shell.estActif('/capa/efficacite')).toBeTrue();
    expect(shell.estActif('/capa')).toBeFalse();

    // Mais un prefixe reste une correspondance valable : /apqp/pr1/2 designe un
    // projet et un rang de phase, pas une entree de menu, et « Projets » doit y
    // rester allumee.
    routeur.url = '/apqp/pr1/2';
    expect(shell.estActif('/apqp')).toBeTrue();

    // Les parametres de requete et l'ancre ne changent pas l'entree active.
    routeur.url = '/capa/efficacite?from=menu#haut';
    expect(shell.estActif('/capa/efficacite')).toBeTrue();
    expect(shell.estActif('/capa')).toBeFalse();
  });

  it('SPC a son groupe, en deux sous-menus : cartes de controle et Gage R&R', () => {
    const spc = component.sections.find(s => s.items.some(i => i.route === '/spc/cartes'))!;
    expect(spc.items.map(i => i.route)).toEqual(['/spc/cartes', '/spc/gage-rr']);
    expect(spc.items.every(i => i.module === 'dmaic')).toBeTrue();
    // L'analyse SPC n'a plus d'entree dans les Analyses IA : la SPC se rejoint par son groupe.
    const ia = component.sections.find(s => s.items.some(i => i.route === '/anomaly'))!;
    expect(ia.items.map(i => i.route)).not.toContain('/spc');
  });

  it('APQP n’a qu’une entree : les projets', () => {
    // Le dossier PPAP n'a plus d'entree globale : il appartient a un projet, et
    // /apqp/ppap n'existe plus -- une entree de menu y menerait a une page qui
    // chercherait un projet nomme « ppap ».
    const apqp = component.sections.find(s => s.items.some(i => i.route === '/apqp'))!;
    expect(apqp.items.map(i => i.route)).toEqual(['/apqp']);
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
    // Opérations passe à 10 : + Planning audits (§4.4) — les échéances à venir
    // ne se lisent pas dans la liste paginée de tous les plans, tous statuts mêlés.
    // Puis à 11 : + Produits — le référentiel qui donne son sujet au PFMEA et au
    // control plan, posé juste avant l'entrée FMEA qu'il alimente.
    // Methodes retombe a 5 et Non-conformite a 2 : Ishikawa et les 5 Pourquoi
    // quittent la barre laterale, on y accede depuis la fiche de NC.
    // APQP tient en 1 : « Projets ». Le schema en V d'un projet porte lui-meme
    // la navigation entre ses phases, et le dossier PPAP appartient desormais a
    // un projet -- il n'a plus d'adresse globale. Operations passe de 12
    // à 11 : l'écran Réclamations a été retiré.
    // Méthodes passe à 6 : + Boîte à idées, sans attribut `module` puisqu'elle
    // ne dépend pas de l'activation du module Cercle.
    // Non-conformite passe de 2 a 4 et Operations de 11 a 9 : CAPA et
    // Efficacite CAPA rejoignent le groupe de l'ecart qu'elles traitent.
    // Pilotage passe a 6 : + Couts de non-qualite, sans attribut `module`
    // (pas encore d'entree au catalogue des modules activables).
    // Risques & opportunites (1) s'insere apres la non-conformite : le registre
    // unique (ISO 9001 6.1), dont les onglets portent la navigation interne.
    // Pilotage passe a 7 : + Tableau de bord SMI, sans attribut `module` -- il
    // ne fait que relire les autres modules.
    // Administration passe a 8 : + Roles et droits (ADR 0078), puis a 9 : + Clients (ADR 0079),
    // puis a 10 : + Circuits de validation (ADR 0080), puis a 11 : + Licence
    // (ADR 0082) — qui n'apparait qu'en on-premise, comme Clients qu'en SaaS. Puis a 12 :
    // + Parametres MOC (ADR 0087). Puis a 13 : + Documentation (ADR 0094).
    // SPC (2) s'insere apres les Analyses IA : Cartes de controle et Gage R&R
    // (ADR 0096) ; Administration passe a 14 : + Parametres SPC.
    // Analyses IA retombe a 7 : l'entree SPC la quittait pour son propre groupe.
    // Administration retombe a 13 : les circuits par roles cedent la place aux
    // signataires choisis a la soumission (ADR 0099). Puis a 14 : + Parametres DMAIC (ADR 0101).
    // Non-conformite passe a 5 : + NC fournisseurs, a cote des NC interne et externe.
    // Administration passe a 15 : + Fournisseur d'IA (ADR 0102).
    // Methodes passe a 7 : + Ishikawa, de nouveau dans la barre laterale.
    // Non-conformite passe a 6 : + Fournisseurs, a cote de leurs NC. Formation
    // et Matrice de competences passent des Referentiels (11 -> 8) aux
    // Operations (9 -> 11).
    expect(labels).toEqual([7, 7, 7, 2, 1, 6, 1, 11, 8, 1, 15]);
  });

  it('le registre des risques a son groupe, juste apres la non-conformite', () => {
    const noms = component.sections.map(s => s.items.map(i => i.route));
    const nc = noms.findIndex(r => r.includes('/nc/interne'));
    expect(noms[nc + 1]).toEqual(['/risques']);
    // L'AMDEC reste ou elle est : le registre ne la remplace pas.
    expect(noms.flat()).toContain('/fmea');
  });

  it('collapses the entire GRC mass into a single /compliance entry', () => {
    // Repéré par sa ROUTE et non par son rang : ce banc dit « la masse GRC tient
    // en une entrée », pas « le septième groupe ». Un groupe inséré avant elle
    // le faisait tomber pour une raison étrangère à ce qu'il vérifie.
    const grc = component.sections.find(s => s.items.some(i => i.route === '/compliance'))!;
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
    ['/home', '/dashboard', '/smi', '/pdca', '/fives', '/dmaic', '/spc/cartes',
     '/nc/interne', '/nc/externe', '/capa', '/audits', '/standards', '/itsm', '/compliance']
      .forEach(r => expect(allRoutes).withContext(r).toContain(r));
  });

  it('propose Ishikawa dans les methodes, mais pas les 5 Pourquoi', () => {
    // Ishikawa a une liste propre, atteignable en un clic. Les 5 Pourquoi,
    // eux, partent toujours d'un ecart constate : on y entre par la fiche NC.
    const methodes = component.sections.find(s => s.items.some(i => i.route === '/pdca'))!;
    expect(methodes.items.map(i => i.route)).toContain('/ishikawa');
    const allRoutes = component.sections.flatMap(s => s.items.map(i => i.route));
    expect(allRoutes).not.toContain('/five-whys');
  });

  it('range les fournisseurs avec leurs NC, et la formation dans les operations', () => {
    const groupOf = (route: string) =>
      component.sections.find(s => s.items.some(i => i.route === route))!.label;
    expect(groupOf('/suppliers')).toBe(groupOf('/suppliers/non-conformites'));
    expect(groupOf('/training')).toBe(groupOf('/audits'));
    expect(groupOf('/training/competences')).toBe(groupOf('/audits'));
  });

  it('persists collapsed groups to localStorage and restores them', () => {
    component.ngOnInit();
    const label = component.sections[2].label;
    component.toggleSection(label);
    expect(component.isSectionCollapsed(label)).toBeFalse();

    const restored = make();
    restored.ngOnInit();
    expect(restored.isSectionCollapsed(label)).toBeFalse();
    // Les autres groupes gardent l'état enregistré, replié.
    expect(restored.isSectionCollapsed(component.sections[3].label)).toBeTrue();
  });

  it('rend tous les groupes repliables, sauf le Pilotage', () => {
    expect(component.sections[0].collapsible).toBeFalsy();
    component.sections.slice(1).forEach(s =>
      expect(s.collapsible).withContext(s.label).toBeTrue());
  });

  it('replie par défaut tous les groupes, sauf celui de la page courante', () => {
    const h = harness();
    (h.router as unknown as { url: string }).url = '/capa/efficacite';
    h.component.ngOnInit();
    const capa = h.component.sections.find(s => s.items.some(i => i.route === '/capa'))!;
    expect(h.component.isSectionCollapsed(capa.label)).toBeFalse();
    h.component.sections
      .filter(s => s.collapsible && s !== capa)
      .forEach(s => expect(h.component.isSectionCollapsed(s.label)).withContext(s.label).toBeTrue());
    h.component.ngOnDestroy();
  });

  it('ouvre le groupe de la page atteinte par une navigation', () => {
    const h = harness();
    h.component.ngOnInit();
    const audits = h.component.sections.find(s => s.items.some(i => i.route === '/audits'))!;
    expect(h.component.isSectionCollapsed(audits.label)).toBeTrue();

    (h.router as unknown as { url: string }).url = '/audits/a-1';
    h.events.next(new NavigationEnd(1, '/audits/a-1', '/audits/a-1'));
    expect(h.component.isSectionCollapsed(audits.label)).toBeFalse();
    expect(JSON.parse(localStorage.getItem('qos.nav.collapsed')!)).not.toContain(audits.label);

    // Une adresse hors menu ne change rien.
    (h.router as unknown as { url: string }).url = '/inconnue';
    h.events.next(new NavigationEnd(2, '/inconnue', '/inconnue'));
    expect(h.component.isSectionCollapsed(audits.label)).toBeFalse();
    h.component.ngOnDestroy();
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
      'webhooks', 'itsm', 'product', 'controlplan'
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
    // il reste les dix autres — Non-conformité, APQP, Risques & opportunités et
    // SPC comprises, chacune avec ses propres entrées.
    const sections = make().filterSections(['USER']);
    expect(sections.length).toBe(10);
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
  it('montre la console éditeur en SaaS et la licence en on-premise, jamais les deux (ADR 0082)', () => {
    const routes = (edition: Edition | null) => make().filterSections(['SUPER_ADMIN', 'ADMIN_TENANT'], null, edition)
      .flatMap(sec => sec.items.map(i => i.route));
    expect(routes('SAAS')).toContain('/admin/clients');
    expect(routes('SAAS')).not.toContain('/admin/licence');
    expect(routes('ONPREM')).toContain('/admin/licence');
    expect(routes('ONPREM')).not.toContain('/admin/clients');
    // Édition encore inconnue : rien n'est retiré.
    expect(routes(null)).toContain('/admin/clients');
    expect(routes(null)).toContain('/admin/licence');
  });
});

describe("MainShellComponent (recherche d'écran)", () => {
  let h: ShellHarness;

  beforeEach(() => {
    localStorage.clear();
    h = harness();
    h.component.ngOnInit();
    // Les sections visibles alimentent la recherche dès leur premier calcul.
    h.component.visibleSections$.subscribe();
  });

  afterEach(() => h.component.ngOnDestroy());

  function key(k: string, init: KeyboardEventInit = {}): KeyboardEvent {
    return new KeyboardEvent('keydown', { key: k, cancelable: true, ...init });
  }

  it('propose les entrées visibles qui correspondent', () => {
    h.component.onSearchInput('capa');
    expect(h.component.searchOpen).toBeTrue();
    expect(h.component.searchHits.map(x => x.item.route)).toContain('/capa');
    expect(h.component.activeHit).toBe(0);
  });

  it("referme la liste pour une saisie vide et signale l'absence de résultat", () => {
    h.component.onSearchInput('   ');
    expect(h.component.searchOpen).toBeFalse();
    h.component.onSearchInput('zzzz-introuvable');
    expect(h.component.searchOpen).toBeTrue();
    expect(h.component.searchHits).toEqual([]);
    expect(h.component.activeHit).toBe(-1);
  });

  it('parcourt les propositions au clavier, en boucle', () => {
    h.component.onSearchInput('a');
    const count = h.component.searchHits.length;
    expect(count).toBeGreaterThan(1);
    const down = key('ArrowDown');
    h.component.onSearchKeydown(down);
    expect(down.defaultPrevented).toBeTrue();
    expect(h.component.activeHit).toBe(1);
    h.component.onSearchKeydown(key('ArrowUp'));
    h.component.onSearchKeydown(key('ArrowUp'));
    expect(h.component.activeHit).toBe(count - 1);
  });

  it('ouvre la proposition désignée avec Entrée et vide le champ', () => {
    h.component.onSearchInput('capa');
    const target = h.component.searchHits[0].item.route;
    h.component.onSearchKeydown(key('Enter'));
    expect(h.router.navigateByUrl).toHaveBeenCalledWith(target);
    expect(h.component.searchQuery).toBe('');
    expect(h.component.searchOpen).toBeFalse();
  });

  it('ignore Entrée et les flèches sans proposition', () => {
    h.component.onSearchInput('zzzz-introuvable');
    const enter = key('Enter');
    h.component.onSearchKeydown(enter);
    h.component.onSearchKeydown(key('ArrowDown'));
    expect(enter.defaultPrevented).toBeFalse();
    expect(h.router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('abandonne la recherche avec Échap sans fermer le tiroir', () => {
    h.component.navOpen = true;
    h.component.onSearchInput('capa');
    const esc = key('Escape');
    spyOn(esc, 'stopPropagation');
    h.component.onSearchKeydown(esc);
    expect(esc.stopPropagation).toHaveBeenCalled();
    expect(h.component.searchQuery).toBe('');
    expect(h.component.navOpen).toBeTrue();
  });

  it('referme la liste à la perte du focus en gardant la saisie', () => {
    h.component.onSearchInput('capa');
    h.component.closeSearch();
    expect(h.component.searchOpen).toBeFalse();
    expect(h.component.searchQuery).toBe('capa');
  });

  it('ouvrir une proposition referme aussi le tiroir', () => {
    h.component.navOpen = true;
    h.component.onSearchInput('capa');
    h.component.openHit(h.component.searchHits[0]);
    expect(h.component.navOpen).toBeFalse();
    expect(h.router.navigateByUrl).toHaveBeenCalled();
  });

  it('Ctrl+K et ⌘K captent le raccourci, les autres touches non', () => {
    const ctrlK = key('k', { ctrlKey: true });
    h.component.onGlobalKeydown(ctrlK);
    expect(ctrlK.defaultPrevented).toBeTrue();
    const metaK = key('K', { metaKey: true });
    h.component.onGlobalKeydown(metaK);
    expect(metaK.defaultPrevented).toBeTrue();
    const plainK = key('k');
    h.component.onGlobalKeydown(plainK);
    expect(plainK.defaultPrevented).toBeFalse();
  });

  it('affiche un raccourci adapté à la plateforme', () => {
    expect(['⌘K', 'Ctrl K']).toContain(h.component.searchShortcut);
  });
});

describe('MainShellComponent (barre du haut sur téléphone)', () => {
  beforeEach(() => localStorage.clear());

  it('déploie la recherche repliée puis la replie quand le champ perd le focus', () => {
    const shell = harness().component;
    expect(shell.searchExpanded).toBeFalse();

    shell.expandSearch();
    expect(shell.searchExpanded).toBeTrue();

    shell.closeSearch();
    expect(shell.searchExpanded).toBeFalse();
    expect(shell.searchOpen).toBeFalse();
  });

  it('replie aussi la recherche quand on ouvre un écran proposé', () => {
    const h = harness();
    h.component.expandSearch();
    h.component.openHit({ item: { label: 'NC', route: '/nc/interne', icon: 'report' }, section: 'Non-conformité' });
    expect(h.component.searchExpanded).toBeFalse();
    expect(h.router.navigateByUrl).toHaveBeenCalledWith('/nc/interne');
  });

  it('reprend la préférence de thème et la modifie depuis le menu du profil', () => {
    const h = harness();
    h.component.ngOnInit();
    let pref: string | undefined;
    h.component.themePreference$.subscribe(p => (pref = p));
    expect(pref).toBe('system');

    h.component.setTheme('dark');
    expect(h.theme.setPreference).toHaveBeenCalledWith('dark');
    h.component.ngOnDestroy();
  });
});
