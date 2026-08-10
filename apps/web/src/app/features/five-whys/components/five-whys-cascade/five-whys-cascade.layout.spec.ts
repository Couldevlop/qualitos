import { buildCascadeLayout, CascadeStepInput } from './five-whys-cascade.layout';

/**
 * Le tracé de la cascade se calcule ; il ne se dessine pas en dur. Ce qui se
 * vérifie ici, c'est justement qu'aucune constante « cinq » ne s'est glissée
 * dans la géométrie : une chaîne de trois et une chaîne de sept doivent toutes
 * deux produire un escalier complet, de la première teinte à la dernière.
 */
describe('buildCascadeLayout', () => {

  const step = (position: number, answer = `Réponse ${position}`): CascadeStepInput =>
    ({ id: `s-${position}`, position, answer });

  const chainOf = (n: number) =>
    Array.from({ length: n }, (_, i) => step(i + 1));

  const ltr = { rtl: false };

  it('ne dessine rien tant que la chaîne est vide', () => {
    const layout = buildCascadeLayout([], ltr);

    expect(layout.cards).toEqual([]);
    expect(layout.connectors).toEqual([]);
    expect(layout.width).toBe(0);
    expect(layout.height).toBe(0);
  });

  it('trace un encart par pourquoi et une liaison entre chaque paire', () => {
    const layout = buildCascadeLayout(chainOf(3), ltr);

    expect(layout.cards.length).toBe(3);
    expect(layout.connectors.length).toBe(2);
  });

  // --- le nombre de pourquoi n'est pas figé -------------------------------------

  it('s\'adapte à une chaîne de trois comme à une chaîne de sept', () => {
    const courte = buildCascadeLayout(chainOf(3), ltr);
    const longue = buildCascadeLayout(chainOf(7), ltr);

    expect(courte.cards.length).toBe(3);
    expect(longue.cards.length).toBe(7);
    expect(longue.connectors.length).toBe(6);
    // Sept marches occupent plus de place que trois, en largeur comme en hauteur.
    expect(longue.width).toBeGreaterThan(courte.width);
    expect(longue.height).toBeGreaterThan(courte.height);
  });

  it('répartit la rampe rouge→vert sur la longueur RÉELLE de la chaîne', () => {
    [3, 4, 5, 6, 7].forEach(n => {
      const tones = buildCascadeLayout(chainOf(n), ltr).cards.map(c => c.tone);

      expect(tones[0]).withContext(`chaîne de ${n}`).toBe(0);
      expect(tones[tones.length - 1]).withContext(`chaîne de ${n}`).toBe(3);
      // La rampe ne recule jamais : la progression EST le propos.
      for (let i = 1; i < tones.length; i++) {
        expect(tones[i]).withContext(`chaîne de ${n}`).toBeGreaterThanOrEqual(tones[i - 1]);
      }
    });
  });

  it('tolère un unique pourquoi sans liaison ni teinte finale forcée', () => {
    const layout = buildCascadeLayout(chainOf(1), ltr);

    expect(layout.cards.length).toBe(1);
    expect(layout.connectors).toEqual([]);
    expect(layout.cards[0].tone).toBe(0);
  });

  // --- l'escalier ---------------------------------------------------------------

  it('décale chaque encart vers le bas et vers la droite', () => {
    const layout = buildCascadeLayout(chainOf(5), ltr);

    for (let i = 1; i < layout.cards.length; i++) {
      expect(layout.cards[i].x).toBeGreaterThan(layout.cards[i - 1].x);
      expect(layout.cards[i].y).toBeGreaterThan(layout.cards[i - 1].y);
    }
  });

  it('empile les encarts sans chevauchement, quelle que soit la longueur du texte', () => {
    const layout = buildCascadeLayout([
      step(1, 'court'),
      step(2, 'une réponse nettement plus longue qui occupera plusieurs lignes dans son encart'),
      step(3, 'court aussi')
    ], ltr);

    for (let i = 1; i < layout.cards.length; i++) {
      const precedent = layout.cards[i - 1];
      expect(layout.cards[i].y).toBeGreaterThan(precedent.y + precedent.height);
    }
    // L'encart au texte long est plus haut : la hauteur suit le contenu.
    expect(layout.cards[1].height).toBeGreaterThan(layout.cards[0].height);
  });

  it('contient tout le dessin dans la boîte annoncée', () => {
    const layout = buildCascadeLayout(chainOf(7), ltr);

    layout.cards.forEach(c => {
      expect(c.x).toBeGreaterThanOrEqual(0);
      expect(c.x + c.width).toBeLessThanOrEqual(layout.width);
      expect(c.y + c.height).toBeLessThanOrEqual(layout.height);
    });
  });

  it('remet la chaîne en rang même si les étapes arrivent en désordre', () => {
    const layout = buildCascadeLayout([step(3), step(1), step(2)], ltr);

    expect(layout.cards.map(c => c.position)).toEqual([1, 2, 3]);
  });

  it('relie les pastilles par une flèche coudée à trois points', () => {
    const layout = buildCascadeLayout(chainOf(2), ltr);
    const points = layout.connectors[0].points.split(' ');

    expect(points.length).toBe(3);
    // Descente verticale puis avancée latérale : le coude, pas la diagonale.
    expect(points[0].split(',')[0]).toBe(points[1].split(',')[0]);
    expect(points[1].split(',')[1]).toBe(points[2].split(',')[1]);
    expect(layout.connectors[0].points).toContain(String(layout.cards[1].badgeCx));
  });

  // --- encart terminal « cause racine » ------------------------------------------

  it('ferme la descente par un encart de cause racine quand elle est conclue', () => {
    const layout = buildCascadeLayout(chainOf(3), { rtl: false, rootCause: 'Presse mal réglée' });

    expect(layout.cards.length).toBe(4);
    const dernier = layout.cards[3];
    expect(dernier.isRoot).toBeTrue();
    expect(dernier.tone).toBe(3);
    expect(dernier.lines[0].text).toBe('Presse mal réglée');
    expect(layout.connectors.length).toBe(3);
  });

  it('n\'ajoute pas d\'encart terminal pour une cause racine vide ou absente', () => {
    expect(buildCascadeLayout(chainOf(3), { rtl: false, rootCause: null }).cards.length).toBe(3);
    expect(buildCascadeLayout(chainOf(3), { rtl: false, rootCause: '   ' }).cards.length).toBe(3);
  });

  // --- sens de lecture ------------------------------------------------------------

  it('inverse l\'escalier en lecture de droite à gauche', () => {
    const rtl = buildCascadeLayout(chainOf(4), { rtl: true });

    for (let i = 1; i < rtl.cards.length; i++) {
      // La descente part de la droite : sinon la progression contredirait le
      // sens de lecture arabe au lieu de l'accompagner.
      expect(rtl.cards[i].x).toBeLessThan(rtl.cards[i - 1].x);
      expect(rtl.cards[i].y).toBeGreaterThan(rtl.cards[i - 1].y);
    }
    rtl.cards.forEach(c => {
      expect(c.anchor).toBe('end');
      expect(c.x).toBeGreaterThanOrEqual(0);
      expect(c.x + c.width).toBeLessThanOrEqual(rtl.width);
    });
  });

  it('garde exactement la même boîte dans les deux sens de lecture', () => {
    const gauche = buildCascadeLayout(chainOf(5), { rtl: false });
    const droite = buildCascadeLayout(chainOf(5), { rtl: true });

    expect(droite.width).toBe(gauche.width);
    expect(droite.height).toBe(gauche.height);
    // Le miroir ne touche pas aux ordonnées : la descente reste une descente.
    expect(droite.cards.map(c => c.y)).toEqual(gauche.cards.map(c => c.y));
  });
});
