import type { NavSection } from './main-shell.component';
import { NAV_SEARCH_LIMIT, normalize, searchNav } from './nav-search';

const SECTIONS: NavSection[] = [
  {
    label: 'Pilotage',
    items: [
      { label: 'Accueil', route: '/home', icon: 'home' },
      { label: 'Tableau de bord', route: '/dashboard', icon: 'dashboard' }
    ]
  },
  {
    label: 'Sécurité',
    items: [
      { label: 'Incidents cyber', route: '/cyber-incidents', icon: 'shield' },
      { label: 'Registre', route: '/registre', icon: 'list' }
    ]
  },
  {
    label: 'Opérations',
    items: [
      { label: 'Registre des risques', route: '/risques', icon: 'warning' },
      { label: 'Tableau de bord', route: '/dashboard', icon: 'dashboard' }
    ]
  }
];

describe('searchNav', () => {
  it('ignore les accents et la casse', () => {
    expect(normalize('  Sécurité ')).toBe('securite');
    expect(searchNav(SECTIONS, 'SECURITE').map(h => h.item.route))
      .toEqual(['/cyber-incidents', '/registre']);
  });

  it('ne propose rien pour une saisie vide', () => {
    expect(searchNav(SECTIONS, '   ')).toEqual([]);
  });

  it('classe le début de libellé, puis le libellé, puis le groupe', () => {
    const routes = searchNav(SECTIONS, 'registre').map(h => h.item.route);
    expect(routes).toEqual(['/registre', '/risques']);
    expect(searchNav(SECTIONS, 'risques').map(h => h.item.route)).toEqual(['/risques']);
    expect(searchNav(SECTIONS, 'cyber').map(h => h.item.route)).toEqual(['/cyber-incidents']);
  });

  it('exige chaque mot, dans le libellé ou le groupe', () => {
    const hits = searchNav(SECTIONS, 'securite registre');
    expect(hits.map(h => h.item.route)).toEqual(['/registre']);
    expect(hits[0].section).toBe('Sécurité');
    expect(searchNav(SECTIONS, 'registre inexistant')).toEqual([]);
  });

  it('ne répète pas une route présente dans deux groupes', () => {
    expect(searchNav(SECTIONS, 'tableau').length).toBe(1);
  });

  it('borne le nombre de propositions', () => {
    const many: NavSection[] = [{
      label: 'Groupe',
      items: Array.from({ length: 20 }, (_, i) => ({ label: `Écran ${i}`, route: `/e${i}`, icon: 'x' }))
    }];
    expect(searchNav(many, 'ecran').length).toBe(NAV_SEARCH_LIMIT);
    expect(searchNav(many, 'ecran', 3).length).toBe(3);
  });
});
