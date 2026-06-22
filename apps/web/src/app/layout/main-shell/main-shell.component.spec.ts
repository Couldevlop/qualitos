import { of } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { OfflineQueueService } from '../../core/offline/offline-queue.service';
import { MainShellComponent } from './main-shell.component';

/** Construit le composant avec des dépendances stub (test de logique pure). */
function make(): MainShellComponent {
  const auth = { user: () => of(null) } as unknown as AuthService;
  const connectivity = { online$: of(true) } as unknown as ConnectivityService;
  const offline = { pendingCount$: of(0) } as unknown as OfflineQueueService;
  return new MainShellComponent(auth, connectivity, offline);
}

describe('MainShellComponent (navigation model)', () => {
  let component: MainShellComponent;

  beforeEach(() => {
    localStorage.clear();
    component = make();
  });

  it('exposes exactly six navigation groups', () => {
    expect(component.sections.length).toBe(6);
  });

  it('orders the six groups as designed', () => {
    const labels = component.sections.map(s => s.items.length);
    // Pilotage(5), Méthodes(6), Analyses IA(8), Opérations(7), Référentiels(8), GRC(1)
    // Référentiels = 8 : + Génération doc IA (standards-doc-gen) + Academy (LMS).
    expect(labels).toEqual([5, 6, 8, 7, 8, 1]);
  });

  it('collapses the entire GRC mass into a single /compliance entry', () => {
    const grc = component.sections[component.sections.length - 1];
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
