import { truncateSvgText, wrapSvgText } from './svg-text';

/**
 * Le repli de texte commande la géométrie des deux diagrammes : c'est lui qui
 * donne la hauteur d'un encart, donc la position de tout ce qui suit. Un repli
 * qui rend une ligne de trop décale silencieusement le dessin entier.
 */
describe('svg-text', () => {

  describe('wrapSvgText', () => {

    it('rend une seule ligne quand le libellé tient', () => {
      expect(wrapSvgText('Presse mal réglée', 40, 3)).toEqual(['Presse mal réglée']);
    });

    it('replie sur les espaces sans jamais dépasser la largeur demandée', () => {
      const lines = wrapSvgText('le convoyeur a dérivé pendant la nuit sans alarme', 20, 4);

      expect(lines.length).toBeGreaterThan(1);
      lines.forEach(l => expect(l.length).toBeLessThanOrEqual(20));
      expect(lines.join(' ')).toBe('le convoyeur a dérivé pendant la nuit sans alarme');
    });

    it('normalise les espaces multiples et les retours à la ligne', () => {
      expect(wrapSvgText('  deux   mots\nsuivants  ', 40, 3)).toEqual(['deux mots suivants']);
    });

    it('coupe net un mot plus long que la ligne plutôt que de le laisser déborder', () => {
      const lines = wrapSvgText('ABCDEFGHIJKLMNOP', 5, 4);

      lines.forEach(l => expect(l.length).toBeLessThanOrEqual(5));
      expect(lines[0]).toBe('ABCDE');
    });

    it('borne le nombre de lignes et signale la coupe par une ellipse', () => {
      const lines = wrapSvgText('un deux trois quatre cinq six sept huit neuf dix', 10, 2);

      expect(lines.length).toBe(2);
      expect(lines[1].endsWith('…')).toBeTrue();
      // L'ellipse compte DANS le budget : sans cette assertion, une ligne à
      // `maxChars + 1` passait, et le glyphe débordait de l'encart.
      lines.forEach(l => expect(l.length).toBeLessThanOrEqual(10));
    });

    it('borne aussi le nombre de lignes quand la coupe vient d\'un mot imprenable', () => {
      const lines = wrapSvgText('AAAAAAAAAAAAAAAAAAAAAAAA', 4, 2);

      expect(lines.length).toBe(2);
      expect(lines[1].endsWith('…')).toBeTrue();
      lines.forEach(l => expect(l.length).toBeLessThanOrEqual(4));
    });

    it('rend une liste vide pour un texte absent — l\'encart n\'aura pas de ligne', () => {
      expect(wrapSvgText('', 40, 3)).toEqual([]);
      expect(wrapSvgText('   ', 40, 3)).toEqual([]);
      expect(wrapSvgText(undefined as unknown as string, 40, 3)).toEqual([]);
    });

    it('rend une liste vide quand la boîte n\'a aucune place', () => {
      expect(wrapSvgText('texte', 0, 3)).toEqual([]);
      expect(wrapSvgText('texte', 40, 0)).toEqual([]);
    });
  });

  describe('truncateSvgText', () => {

    it('laisse intact un libellé qui tient', () => {
      expect(truncateSvgText('Méthodes', 20)).toBe('Méthodes');
    });

    it('tronque ellipse comprise dans le budget de largeur', () => {
      const out = truncateSvgText('Moyens financiers du site', 12);

      expect(out.length).toBeLessThanOrEqual(12);
      expect(out.endsWith('…')).toBeTrue();
    });

    it('ne laisse pas d\'espace avant l\'ellipse', () => {
      expect(truncateSvgText('main d oeuvre', 7)).toBe('main d…');
    });

    it('rend une chaîne vide quand il n\'y a aucune place', () => {
      expect(truncateSvgText('Méthodes', 0)).toBe('');
    });
  });
});
