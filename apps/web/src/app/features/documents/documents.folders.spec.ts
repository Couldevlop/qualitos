import { buildFolderRows } from './documents.folders';
import { DocumentFolder } from './documents.types';

/**
 * L'arbre des dossiers mis à plat : l'ordre d'affichage, la profondeur et la
 * présence d'enfants pilotent l'indentation et le chevron de la GED (ADR 0094).
 */
describe('buildFolderRows', () => {
  const folder = (id: string, parentId: string | null, name: string, position = 0): DocumentFolder =>
    ({ id, parentId, name, position });

  it('rend une liste vide pour aucun dossier', () => {
    expect(buildFolderRows([])).toEqual([]);
  });

  it('ordonne par position puis par nom, chaque dossier suivi de ses enfants', () => {
    const rows = buildFolderRows([
      folder('b', null, 'Procédures', 1),
      folder('a2', null, 'Bravo', 0),
      folder('a1', null, 'Alpha', 0),
      folder('b2', 'b', 'Support', 1),
      folder('b1', 'b', 'Réalisation', 0)
    ]);
    expect(rows.map(r => r.folder.id)).toEqual(['a1', 'a2', 'b', 'b1', 'b2']);
  });

  it('calcule la profondeur et la présence d’enfants', () => {
    const rows = buildFolderRows([
      folder('root', null, 'Racine'),
      folder('child', 'root', 'Enfant'),
      folder('leaf', 'child', 'Feuille')
    ]);
    expect(rows.map(r => [r.folder.id, r.depth, r.hasChildren])).toEqual([
      ['root', 0, true],
      ['child', 1, true],
      ['leaf', 2, false]
    ]);
  });

  it('traite un parentId indéfini comme une racine', () => {
    const rows = buildFolderRows([{ id: 'x', name: 'X', position: 0 } as unknown as DocumentFolder]);
    expect(rows.length).toBe(1);
    expect(rows[0].depth).toBe(0);
  });

  it('ne montre pas un dossier dont le parent est absent', () => {
    const rows = buildFolderRows([
      folder('root', null, 'Racine'),
      folder('orphan', 'disparu', 'Orphelin')
    ]);
    expect(rows.map(r => r.folder.id)).toEqual(['root']);
  });

  it('ne boucle pas sur un cycle et n’en montre rien sans racine', () => {
    const rows = buildFolderRows([
      folder('a', 'b', 'A'),
      folder('b', 'a', 'B')
    ]);
    expect(rows).toEqual([]);
  });

  it('ne montre chaque dossier qu’une fois, même s’il se désigne lui-même', () => {
    const rows = buildFolderRows([
      folder('root', null, 'Racine'),
      folder('self', 'self', 'Boucle'),
      folder('dup', 'root', 'Doublon'),
      folder('dup', 'root', 'Doublon')
    ]);
    expect(rows.map(r => r.folder.id)).toEqual(['root', 'dup']);
  });
});
