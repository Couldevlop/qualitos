import {
  contrastRatio, hoverVariant, mix, parseHex, readableForeground, relativeLuminance,
  softForeground, toHex, withAlpha
} from './brand-color';

/**
 * L'arithmétique de couleur de la marque.
 *
 * <p>Elle sert à décider ce qu'un utilisateur pourra lire : les valeurs
 * attendues sont donc celles de WCAG, pas des approximations commodes.
 */
describe('brand-color', () => {

  const BLANC = { r: 255, g: 255, b: 255 };
  const NOIR = { r: 0, g: 0, b: 0 };

  describe('parseHex', () => {
    it('lit la forme longue, casse comprise', () => {
      expect(parseHex('#2563d6')).toEqual({ r: 0x25, g: 0x63, b: 0xd6 });
      expect(parseHex('#2563D6')).toEqual({ r: 0x25, g: 0x63, b: 0xd6 });
    });

    it('refuse ce qui n est pas une couleur, sans lever', () => {
      // Une marque mal formée ne doit pas empêcher l'application de s'afficher.
      expect(parseHex('#fff')).toBeNull();
      expect(parseHex('2563d6')).toBeNull();
      expect(parseHex('rouge')).toBeNull();
      expect(parseHex('')).toBeNull();
      expect(parseHex(null)).toBeNull();
      expect(parseHex(undefined)).toBeNull();
    });
  });

  describe('toHex', () => {
    it('réécrit une couleur en forme longue', () => {
      expect(toHex({ r: 37, g: 99, b: 214 })).toBe('#2563d6');
    });

    it('arrondit et borne les canaux hors échelle', () => {
      expect(toHex({ r: 254.6, g: -10, b: 300 })).toBe('#ff00ff');
    });
  });

  describe('relativeLuminance et contrastRatio', () => {
    it('donne les bornes de WCAG : 0 et 1', () => {
      expect(relativeLuminance(NOIR)).toBe(0);
      expect(relativeLuminance(BLANC)).toBe(1);
    });

    it('donne 21 entre noir et blanc, et 1 pour une couleur avec elle-même', () => {
      expect(contrastRatio(NOIR, BLANC)).toBeCloseTo(21, 5);
      expect(contrastRatio(BLANC, NOIR)).toBeCloseTo(21, 5);
      expect(contrastRatio(NOIR, NOIR)).toBeCloseTo(1, 5);
    });

    it('mesure la même chose que le serveur sur la couleur du socle', () => {
      // #2563d6 sur blanc : 5,4767 par la formule de WCAG 2.1, celle qu'applique
      // `ContrastGuard` au serveur. Deux formules différentes donneraient deux
      // verdicts sur la même couleur — et celle du serveur fait foi.
      expect(contrastRatio(parseHex('#2563d6')!, BLANC)).toBeCloseTo(5.4767, 3);
    });
  });

  describe('readableForeground', () => {
    it('pose du blanc sur un fond sombre et de l encre sur un fond clair', () => {
      expect(readableForeground(parseHex('#173a86')!)).toEqual(BLANC);
      expect(readableForeground(parseHex('#ffd400')!)).toEqual({ r: 17, g: 24, b: 27 });
    });

    it('rend toujours le MEILLEUR des deux, jamais un seuil arbitraire', () => {
      // Un seuil de luminance se trompe sur les couleurs saturées : on vérifie
      // sur plusieurs teintes que le choix rendu est bien le plus contrasté.
      for (const teinte of ['#2563d6', '#ffd400', '#00806a', '#808080', '#7f7f80']) {
        const fond = parseHex(teinte)!;
        const retenu = readableForeground(fond);
        const autre = retenu === BLANC ? { r: 17, g: 24, b: 27 } : BLANC;
        expect(contrastRatio(retenu, fond)).toBeGreaterThanOrEqual(contrastRatio(autre, fond));
      }
    });
  });

  describe('mix', () => {
    it('rend les extrémités telles quelles', () => {
      expect(mix(NOIR, BLANC, 0)).toEqual(NOIR);
      expect(mix(NOIR, BLANC, 1)).toEqual(BLANC);
    });

    it('rend le milieu au milieu', () => {
      expect(toHex(mix(NOIR, BLANC, 0.5))).toBe('#808080');
    });

    it('borne un ratio hors échelle au lieu d extrapoler', () => {
      expect(mix(NOIR, BLANC, 5)).toEqual(BLANC);
      expect(mix(NOIR, BLANC, -5)).toEqual(NOIR);
    });
  });

  describe('withAlpha', () => {
    it('écrit une couleur translucide', () => {
      expect(withAlpha({ r: 37, g: 99, b: 214 }, 0.12)).toBe('rgba(37, 99, 214, 0.12)');
    });

    it('borne l opacité', () => {
      expect(withAlpha(NOIR, 9)).toBe('rgba(0, 0, 0, 1)');
      expect(withAlpha(NOIR, -1)).toBe('rgba(0, 0, 0, 0)');
    });
  });

  describe('hoverVariant', () => {
    it('assombrit en clair et éclaircit en sombre', () => {
      const couleur = parseHex('#2563d6')!;

      const clair = hoverVariant(couleur, false);
      const sombre = hoverVariant(couleur, true);

      // Le survol doit S'ÉCARTER du fond : plus sombre sur une page claire,
      // plus clair sur une page sombre. Assombrir toujours le rendait invisible.
      expect(relativeLuminance(clair)).toBeLessThan(relativeLuminance(couleur));
      expect(relativeLuminance(sombre)).toBeGreaterThan(relativeLuminance(couleur));
    });
  });

  describe('softForeground', () => {
    it('atteint le seuil AA sur le fond doux correspondant, en clair', () => {
      for (const teinte of ['#2563d6', '#00806a', '#b31d1d', '#5a8df0']) {
        const couleur = parseHex(teinte)!;
        const fondDoux = mix({ r: 255, g: 255, b: 255 }, couleur, 0.12);

        expect(contrastRatio(softForeground(couleur, false), fondDoux))
          .toBeGreaterThanOrEqual(4.5);
      }
    });

    it('atteint le seuil AA sur le fond doux correspondant, en sombre', () => {
      for (const teinte of ['#5a8df0', '#2563d6', '#ffd400']) {
        const couleur = parseHex(teinte)!;
        const fondDoux = mix({ r: 17, g: 24, b: 27 }, couleur, 0.18);

        expect(contrastRatio(softForeground(couleur, true), fondDoux))
          .toBeGreaterThanOrEqual(4.5);
      }
    });

    it('reste au plus près de la marque : ne pousse pas plus que nécessaire', () => {
      const couleur = parseHex('#2563d6')!;

      const doux = softForeground(couleur, false);

      // Elle est poussée vers l'encre, mais pas JUSQU'À l'encre : la marque doit
      // rester reconnaissable dans un lien.
      expect(toHex(doux)).not.toBe('#11181b');
      expect(relativeLuminance(doux)).toBeLessThanOrEqual(relativeLuminance(couleur));
    });
  });
});
