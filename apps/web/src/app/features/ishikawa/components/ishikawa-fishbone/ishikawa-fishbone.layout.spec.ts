import { buildFishboneLayout, FishboneBranchInput } from './ishikawa-fishbone.layout';

/**
 * L'arête se calcule à partir du nombre RÉEL de branches — 6M, 7M ou 8M — et
 * du nombre de causes portées par chacune. Ce qui se vérifie ici, c'est
 * qu'aucun « six » n'est resté figé dans le tracé, et que la figure garde sa
 * lisibilité quand la configuration change.
 */
describe('buildFishboneLayout', () => {

  const cause = (n: number, descendants = 0) =>
    ({ id: `c-${n}`, label: `Cause ${n}`, descendants });

  const branch = (n: number, causes = 0, descendants = 0): FishboneBranchInput => ({
    key: `b-${n}`,
    label: `Branche ${n}`,
    causes: Array.from({ length: causes }, (_, i) => cause(n * 10 + i, descendants))
  });

  const branchesOf = (n: number, causes = 2) =>
    Array.from({ length: n }, (_, i) => branch(i + 1, causes));

  const ltr = { rtl: false };

  it('ne dessine rien sans aucune catégorie', () => {
    const layout = buildFishboneLayout('Rebuts en hausse', [], ltr);

    expect(layout.bones).toEqual([]);
    expect(layout.width).toBe(0);
    expect(layout.height).toBe(0);
  });

  // --- le nombre de catégories est configurable ---------------------------------

  it('trace une branche par catégorie, en 6M comme en 7M et 8M', () => {
    expect(buildFishboneLayout('P', branchesOf(6), ltr).bones.length).toBe(6);
    expect(buildFishboneLayout('P', branchesOf(7), ltr).bones.length).toBe(7);
    expect(buildFishboneLayout('P', branchesOf(8), ltr).bones.length).toBe(8);
  });

  it('alterne les branches au-dessus et au-dessous de l\'épine', () => {
    const layout = buildFishboneLayout('P', branchesOf(7), ltr);

    expect(layout.bones.map(b => b.side))
      .toEqual(['top', 'bottom', 'top', 'bottom', 'top', 'bottom', 'top']);
    layout.bones.forEach(b => {
      if (b.side === 'top') {
        expect(b.y2).toBeLessThan(layout.spineY);
      } else {
        expect(b.y2).toBeGreaterThan(layout.spineY);
      }
    });
  });

  it('allonge l\'épine quand une colonne de plus est nécessaire', () => {
    const six = buildFishboneLayout('P', branchesOf(6), ltr);
    const sept = buildFishboneLayout('P', branchesOf(7), ltr);
    const huit = buildFishboneLayout('P', branchesOf(8), ltr);

    // Une colonne accueille deux branches : 6 tiennent en 3 colonnes, 7 et 8 en 4.
    expect(sept.width).toBeGreaterThan(six.width);
    expect(huit.width).toBe(sept.width);
  });

  it('place chaque paire de branches dans sa propre colonne', () => {
    const layout = buildFishboneLayout('P', branchesOf(6), ltr);
    const hauts = layout.bones.filter(b => b.side === 'top').map(b => b.x1);

    for (let i = 1; i < hauts.length; i++) {
      expect(hauts[i]).toBeGreaterThan(hauts[i - 1]);
    }
    // La branche du haut et celle du bas d'une même colonne partagent l'abscisse.
    expect(layout.bones[1].x1).toBe(layout.bones[0].x1);
  });

  // --- causes ---------------------------------------------------------------------

  it('accroche un trait par cause de premier niveau', () => {
    const layout = buildFishboneLayout('P', [branch(1, 4), branch(2, 1)], ltr);

    expect(layout.bones[0].causes.length).toBe(4);
    expect(layout.bones[1].causes.length).toBe(1);
  });

  it('allonge la branche à mesure qu\'elle porte des causes', () => {
    const courte = buildFishboneLayout('P', [branch(1, 1)], ltr).bones[0];
    const longue = buildFishboneLayout('P', [branch(1, 6)], ltr).bones[0];

    expect(Math.abs(longue.y2 - longue.y1)).toBeGreaterThan(Math.abs(courte.y2 - courte.y1));
  });

  it('signale une branche encore vide sans la faire disparaître', () => {
    const layout = buildFishboneLayout('P', [branch(1, 0), branch(2, 2)], ltr);

    // Une famille non explorée est une information : voir la branche vide,
    // c'est voir le trou dans l'analyse.
    expect(layout.bones[0].empty).toBeTrue();
    expect(layout.bones[0].causes).toEqual([]);
    expect(layout.bones[1].empty).toBeFalse();
  });

  it('répartit les causes entre l\'épine et la pointe, bornes exclues', () => {
    const bone = buildFishboneLayout('P', [branch(1, 3)], ltr).bones[0];

    bone.causes.forEach(c => {
      expect(c.tickY).toBeLessThan(bone.y1);      // au-dessus de l'épine
      expect(c.tickY).toBeGreaterThan(bone.y2);   // en deçà de la pointe
    });
    // Elles se suivent le long de l'oblique, sans se superposer.
    expect(bone.causes[1].tickY).toBeLessThan(bone.causes[0].tickY);
  });

  it('reporte le nombre de sous-causes sans les tracer', () => {
    const bone = buildFishboneLayout('P', [branch(1, 2, 3)], ltr).bones[0];

    // Le dessin ne représente que le premier niveau : trois niveaux d'obliques
    // se croisent et deviennent illisibles dès la deuxième cause.
    expect(bone.causes[0].descendants).toBe(3);
    expect(bone.causes.length).toBe(2);
  });

  it('tronque un libellé de cause trop long plutôt que de le laisser déborder', () => {
    const bone = buildFishboneLayout('P', [{
      key: 'b', label: 'Une catégorie au nom interminable',
      causes: [{ id: 'c', label: 'Un libellé de cause bien trop long pour tenir sur son trait', descendants: 0 }]
    }], ltr).bones[0];

    expect(bone.causes[0].label.endsWith('…')).toBeTrue();
    expect(bone.label.endsWith('…')).toBeTrue();
  });

  // --- tête ------------------------------------------------------------------------

  it('porte l\'énoncé du problème dans la tête, replié sur plusieurs lignes', () => {
    const layout = buildFishboneLayout(
      'Taux de rebut en hausse continue sur la ligne 3 depuis mars', branchesOf(6), ltr);

    expect(layout.head.lines.length).toBeGreaterThan(1);
    expect(layout.head.lines[0].text.length).toBeGreaterThan(0);
    // La tête est centrée sur l'épine : c'est là que tout converge.
    expect(layout.head.y).toBeLessThan(layout.spineY);
    expect(layout.head.y + layout.head.height).toBeGreaterThan(layout.spineY);
  });

  it('garde une tête dessinable même sans énoncé', () => {
    const layout = buildFishboneLayout('', branchesOf(6), ltr);

    expect(layout.head.lines).toEqual([]);
    expect(layout.head.height).toBeGreaterThan(0);
  });

  it('pose la tête au bout de l\'épine, à droite', () => {
    const layout = buildFishboneLayout('P', branchesOf(6), ltr);

    expect(layout.spineX2).toBeGreaterThan(layout.spineX1);
    expect(layout.head.x).toBe(layout.spineX2);
    expect(layout.head.anchor).toBe('start');
  });

  // --- boîte -------------------------------------------------------------------------

  it('contient tout le dessin dans la boîte annoncée', () => {
    const layout = buildFishboneLayout('Un problème', branchesOf(8, 5), ltr);

    expect(layout.head.x + layout.head.width).toBeLessThanOrEqual(layout.width);
    layout.bones.forEach(b => {
      expect(b.y2).toBeGreaterThan(0);
      expect(b.y2).toBeLessThan(layout.height);
      expect(b.labelBoxY).toBeGreaterThanOrEqual(0);
      expect(b.labelBoxY + b.labelBoxHeight).toBeLessThanOrEqual(layout.height);
    });
  });

  it('centre l\'épine sur le débordement réel du haut et du bas', () => {
    // Branche 1 (haut) chargée, branche 2 (bas) légère : l'épine descend.
    const layout = buildFishboneLayout('P', [branch(1, 8), branch(2, 1)], ltr);

    const hautExtent = layout.spineY - layout.bones[0].y2;
    const basExtent = layout.bones[1].y2 - layout.spineY;
    expect(hautExtent).toBeGreaterThan(basExtent);
  });

  // --- sens de lecture ------------------------------------------------------------------

  it('bascule la tête à gauche et les obliques vers la droite en RTL', () => {
    const layout = buildFishboneLayout('Un problème', branchesOf(6), { rtl: true });

    // L'effet est le point d'arrivée de la lecture : en arabe il est à gauche.
    expect(layout.spineX2).toBeLessThan(layout.spineX1);
    expect(layout.head.x).toBeLessThan(layout.spineX1);
    expect(layout.head.anchor).toBe('end');
    layout.bones.forEach(b => {
      expect(b.x2).toBeGreaterThan(b.x1);
      b.causes.forEach(c => {
        expect(c.anchor).toBe('end');
        // Miroir complet : le trait d'attache part vers la tête, donc vers la
        // gauche, et le libellé s'écrit dans le même sens.
        expect(c.tickX2).toBeLessThan(c.tickX1);
      });
    });
  });

  it('garde la même boîte et les mêmes ordonnées dans les deux sens', () => {
    const gauche = buildFishboneLayout('Un problème', branchesOf(7, 3), { rtl: false });
    const droite = buildFishboneLayout('Un problème', branchesOf(7, 3), { rtl: true });

    expect(droite.width).toBe(gauche.width);
    expect(droite.height).toBe(gauche.height);
    expect(droite.spineY).toBe(gauche.spineY);
    expect(droite.bones.map(b => b.y2)).toEqual(gauche.bones.map(b => b.y2));
  });

  it('maintient tout le dessin dans la boîte, y compris miroité', () => {
    const layout = buildFishboneLayout('Un problème', branchesOf(8, 4), { rtl: true });

    expect(layout.head.x).toBeGreaterThanOrEqual(0);
    layout.bones.forEach(b => {
      expect(b.labelBoxX).toBeGreaterThanOrEqual(0);
      expect(b.labelBoxX + b.labelBoxWidth).toBeLessThanOrEqual(layout.width);
    });
  });
});
