import {
  catalogCodes,
  catalogLabel,
  catalogOptions,
  catalogOverride,
  clearCatalogOverrides,
  setCatalogOverrides
} from './catalog-overrides';
import { CatalogEntry } from './catalog.types';

/** Raccourci : une entrée en service. */
const e = (code: string, label: string, active = true): CatalogEntry => ({ code, label, active });

describe('catalog-overrides', () => {

  afterEach(() => clearCatalogOverrides());

  describe('quand le client n\'a rien réglé', () => {

    it('ne connaît aucune liste', () => {
      expect(catalogOverride('audit-type')).toBeNull();
    });

    it('ne propose aucun libellé, pour que l\'écran garde le sien traduit', () => {
      expect(catalogLabel('audit-type', 'INTERNAL')).toBeNull();
    });

    it('rend les options de l\'écran, inchangées et dans leur ordre', () => {
      const repli = [{ value: 'A', label: 'Un' }, { value: 'B', label: 'Deux' }];
      expect(catalogOptions('audit-type', repli)).toEqual(repli);
    });

    it('rend les codes de l\'écran, inchangés', () => {
      expect(catalogCodes('nc-category', ['PRODUCT', 'PROCESS'])).toEqual(['PRODUCT', 'PROCESS']);
    });
  });

  describe('quand le client a réglé une liste adossée aux codes', () => {

    beforeEach(() => setCatalogOverrides({
      'audit-type': [e('SUPPLIER', 'Audit fournisseur'), e('INTERNAL', 'Revue interne')]
    }));

    it('rend le mot du client pour un code connu', () => {
      expect(catalogLabel('audit-type', 'INTERNAL')).toBe('Revue interne');
    });

    it('accepte un code en minuscules : le code vit en capitales', () => {
      expect(catalogLabel('audit-type', 'internal')).toBe('Revue interne');
    });

    it('ne prétend rien pour un code que le client ne porte pas', () => {
      expect(catalogLabel('audit-type', 'CERTIFICATION')).toBeNull();
    });

    it('ne prétend rien pour une autre liste', () => {
      expect(catalogLabel('capa-nature', 'INTERNAL')).toBeNull();
    });

    it('ne prétend rien sans code', () => {
      expect(catalogLabel('audit-type', null)).toBeNull();
      expect(catalogLabel('audit-type', undefined)).toBeNull();
      expect(catalogLabel('audit-type', '')).toBeNull();
    });

    it('reprend l\'ordre du client et ses mots', () => {
      const repli = [
        { value: 'INTERNAL', label: 'Interne' },
        { value: 'SUPPLIER', label: 'Fournisseur' }
      ];
      expect(catalogOptions('audit-type', repli)).toEqual([
        { value: 'SUPPLIER', label: 'Audit fournisseur' },
        { value: 'INTERNAL', label: 'Revue interne' }
      ]);
    });

    it('garde en fin de liste un code que l\'écran propose et que le client n\'a pas réglé', () => {
      const repli = [
        { value: 'INTERNAL', label: 'Interne' },
        { value: 'SUPPLIER', label: 'Fournisseur' },
        { value: 'CERTIFICATION', label: 'Certification' }
      ];
      expect(catalogOptions('audit-type', repli).map(o => o.value))
        .toEqual(['SUPPLIER', 'INTERNAL', 'CERTIFICATION']);
    });

    it('écarte de la saisie une entrée hors service', () => {
      setCatalogOverrides({
        'audit-type': [e('SUPPLIER', 'Audit fournisseur'), e('INTERNAL', 'Revue interne', false)]
      });
      expect(catalogOptions<string>('audit-type', [{ value: 'INTERNAL', label: 'Interne' }])
        .map(o => o.value)).toEqual(['SUPPLIER']);
    });

    it('garde le libellé d\'une entrée hors service : c\'est lui qui explique l\'historique', () => {
      setCatalogOverrides({ 'audit-type': [e('INTERNAL', 'Revue interne', false)] });
      expect(catalogLabel('audit-type', 'INTERNAL')).toBe('Revue interne');
    });

    it('rend les codes dans l\'ordre du client', () => {
      expect(catalogCodes('audit-type', ['INTERNAL', 'SUPPLIER']))
        .toEqual(['SUPPLIER', 'INTERNAL']);
    });
  });

  describe('quand le client a réglé une liste libre', () => {

    beforeEach(() => setCatalogOverrides({
      'fives-zone': [e('ATELIER-A', 'Atelier A'), e('QUAI', 'Quai de chargement')]
    }));

    it('rend ses valeurs à lui, même absentes de l\'écran', () => {
      expect(catalogOptions('fives-zone', [])).toEqual([
        { value: 'ATELIER-A', label: 'Atelier A' },
        { value: 'QUAI', label: 'Quai de chargement' }
      ]);
    });

    it('rend ses codes à lui', () => {
      expect(catalogCodes<string>('fives-zone', [])).toEqual(['ATELIER-A', 'QUAI']);
    });
  });

  describe('la mémoire du registre', () => {

    it('oublie tout à la déconnexion : un client ne lit jamais les mots d\'un autre', () => {
      setCatalogOverrides({ 'audit-type': [e('INTERNAL', 'Revue interne')] });
      clearCatalogOverrides();
      expect(catalogOverride('audit-type')).toBeNull();
      expect(catalogLabel('audit-type', 'INTERNAL')).toBeNull();
    });

    it('remplace le réglage au lieu de le compléter', () => {
      setCatalogOverrides({ 'audit-type': [e('INTERNAL', 'Revue interne')] });
      setCatalogOverrides({ 'capa-nature': [e('CORRECTIVE', 'Correctif')] });
      expect(catalogOverride('audit-type')).toBeNull();
      expect(catalogLabel('capa-nature', 'CORRECTIVE')).toBe('Correctif');
    });

    it('traite une liste vide comme « rien réglé »', () => {
      setCatalogOverrides({ 'audit-type': [] });
      expect(catalogOverride('audit-type')).toBeNull();
    });

    it('ne laisse pas la liste rendue modifier le repli de l\'écran', () => {
      const repli = [{ value: 'A', label: 'Un' }];
      const rendu = catalogOptions('audit-type', repli);
      rendu.push({ value: 'B', label: 'Deux' });
      expect(repli.length).toBe(1);
    });
  });
});
