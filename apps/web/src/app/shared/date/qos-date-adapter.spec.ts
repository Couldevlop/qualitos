import { TestBed } from '@angular/core/testing';
import { MAT_DATE_LOCALE } from '@angular/material/core';

import { QosDateAdapter } from './qos-date-adapter';

/**
 * L'adaptateur qui relit ce qu'il écrit.
 *
 * <p>Le banc qui porte tout est le premier : pour CHAQUE langue du produit,
 * `parse(format(d))` doit rendre `d`. C'est la seule formulation qui ne se
 * trompe pas de problème — elle ne suppose aucun ordre, aucune écriture de
 * chiffres, et elle tomberait au premier build dans une langue nouvelle.
 */
describe('QosDateAdapter', () => {

  /** Les langues construites, telles que `angular.json` les déclare. */
  const LANGUES = ['fr', 'en', 'es', 'ar', 'ja', 'zh'];

  /**
   * L'adaptateur tel qu'Angular le fabrique.
   *
   * <p>`NativeDateAdapter` lit MAT_DATE_LOCALE par initialiseur de champ : il
   * ne peut naitre qu'en contexte d'injection. Le construire a la main
   * passerait a cote du cablage reel, qui est precisement ce qu'on verifie.
   */
  function adaptateur(langue: string): QosDateAdapter {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [QosDateAdapter, { provide: MAT_DATE_LOCALE, useValue: langue }]
    });
    return TestBed.inject(QosDateAdapter);
  }

  describe('relire ce qui vient d être écrit', () => {
    for (const langue of LANGUES) {
      it(`rend la date de départ en ${langue}`, () => {
        const a = adaptateur(langue);
        for (const [an, mois, jour] of [[2026, 10, 24], [2026, 0, 1], [2026, 11, 31],
          [2024, 1, 29], [1999, 5, 5]] as const) {
          const depart = a.createDate(an, mois, jour);
          const relu = a.parse(a.format(depart, { year: 'numeric', month: 'numeric', day: 'numeric' }));

          expect(relu).withContext(`${langue} : ${a.format(depart, {
            year: 'numeric', month: 'numeric', day: 'numeric'
          })}`).not.toBeNull();
          expect(a.isValid(relu!)).withContext(`${langue} : date invalide`).toBeTrue();
          expect(a.getYear(relu!)).toBe(an);
          expect(a.getMonth(relu!)).toBe(mois);
          expect(a.getDate(relu!)).toBe(jour);
        }
      });
    }
  });

  describe('le défaut d origine', () => {
    it('lit une date française tapée au clavier, que Date.parse rendait NaN', () => {
      expect(Date.parse('24/11/2026')).toBeNaN();

      const relu = adaptateur('fr').parse('24/11/2026')!;

      expect(relu.getFullYear()).toBe(2026);
      expect(relu.getMonth()).toBe(10);
      expect(relu.getDate()).toBe(24);
    });

    it('ne lit JAMAIS une date française dans l ordre américain', () => {
      // Le cas qui fait le plus de dégâts : Date.parse acceptait « 05/06/2026 »
      // et rendait le 6 MAI. Une date acceptée et fausse sur une fiche qui part
      // au client est pire qu une date refusée.
      const relu = adaptateur('fr').parse('05/06/2026')!;

      expect(relu.getMonth()).toBe(5);
      expect(relu.getDate()).toBe(5);
    });

    it('lit la même suite de chiffres autrement selon la langue', () => {
      const enFrancais = adaptateur('fr').parse('05/06/2026')!;
      const enAnglais = adaptateur('en').parse('05/06/2026')!;

      expect(enFrancais.getMonth()).toBe(5);
      expect(enAnglais.getMonth()).toBe(4);
    });
  });

  describe('ce qui ne doit pas passer', () => {
    it('refuse un 31 février au lieu de le décaler au 3 mars', () => {
      const a = adaptateur('fr');
      const relu = a.parse('31/02/2026')!;

      expect(a.isValid(relu)).toBeFalse();
    });

    it('refuse un mois au-delà de douze', () => {
      const a = adaptateur('fr');

      expect(a.isValid(a.parse('01/13/2026')!)).toBeFalse();
    });

    it('refuse une année écrite sur deux chiffres', () => {
      // « 24/11/26 » ne dit pas si 26 est l année ou le jour d un autre ordre.
      const a = adaptateur('fr');

      expect(a.isValid(a.parse('24/11/26')!)).toBeFalse();
    });

    it('rend null sur une saisie vide, et non une date invalide', () => {
      const a = adaptateur('fr');

      expect(a.parse('')).toBeNull();
      expect(a.parse('   ')).toBeNull();
    });
  });

  describe('ce qui doit continuer de passer', () => {
    it('lit une date ISO, quelle que soit la langue', () => {
      for (const langue of LANGUES) {
        const a = adaptateur(langue);
        const relu = a.parse('2026-11-24')!;

        expect(a.isValid(relu)).withContext(langue).toBeTrue();
        expect(a.getYear(relu)).toBe(2026);
        expect(a.getMonth(relu)).toBe(10);
        expect(a.getDate(relu)).toBe(24);
      }
    });

    it('accepte encore un horodatage en nombre', () => {
      const a = adaptateur('fr');
      const relu = a.parse(Date.UTC(2026, 10, 24))!;

      expect(a.isValid(relu)).toBeTrue();
    });

    it('laisse la lecture d origine traiter un mois en toutes lettres', () => {
      const a = adaptateur('en');
      const relu = a.parse('November 24, 2026')!;

      expect(a.isValid(relu)).toBeTrue();
      expect(a.getMonth(relu)).toBe(10);
    });
  });

  describe('les chiffres qui ne sont pas les nôtres', () => {
    it('lit une date en chiffres arabes-indiens', () => {
      const a = adaptateur('ar');
      const relu = a.parse('٢٤/١١/٢٠٢٦')!;

      expect(a.isValid(relu)).toBeTrue();
      expect(a.getYear(relu)).toBe(2026);
      expect(a.getMonth(relu)).toBe(10);
      expect(a.getDate(relu)).toBe(24);
    });

    it('ignore les marques de sens d écriture que l arabe insère', () => {
      const a = adaptateur('ar');
      const relu = a.parse('٢٤‏/١١‏/٢٠٢٦')!;

      expect(a.isValid(relu)).toBeTrue();
      expect(a.getDate(relu)).toBe(24);
    });
  });

  describe('l ordre des champs', () => {
    it('suit la langue et non une constante', () => {
      // Si cette attente tombe parce qu un navigateur a changé d avis sur une
      // langue, c est l adaptateur qui a raison : il lit la même source que
      // celle qui a servi à écrire. Aucun ordre n est codé en dur.
      expect(adaptateur('ja').parse('2026/11/24')!.getDate()).toBe(24);
      expect(adaptateur('zh').parse('2026/11/24')!.getDate()).toBe(24);
    });
  });
});
