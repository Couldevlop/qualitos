import { statusLabel, csvCell, fmt, statusTone } from './spc-capability.labels';

describe('spc-capability.labels', () => {

  it('nomme les trois statuts comme les trois couleurs du Cpk', () => {
    expect(statusLabel('CAPABLE')).toBe('Maîtrisé');
    expect(statusLabel('WATCH')).toBe('À surveiller');
    expect(statusLabel('NOT_CAPABLE')).toBe('Non maîtrisé');
  });

  it('colore le Cpk : vert, jaune, rouge', () => {
    expect(statusTone('CAPABLE')).toBe('ok');
    expect(statusTone('WATCH')).toBe('warn');
    expect(statusTone('NOT_CAPABLE')).toBe('bad');
  });

  it('formate un nombre, ou un tiret sans valeur', () => {
    expect(fmt(1.7689)).toBe('1.769');
    expect(fmt(1.7689, 2)).toBe('1.77');
    expect(fmt(null)).toBe('–');
    expect(fmt(undefined)).toBe('–');
    expect(fmt(Number.NaN)).toBe('–');
  });

  it('neutralise les formules dans les cellules CSV (injection CSV)', () => {
    expect(csvCell('=SUM(A1)')).toBe(`"'=SUM(A1)"`);
    expect(csvCell('+1')).toBe(`"'+1"`);
    expect(csvCell('-1')).toBe(`"'-1"`);
    expect(csvCell('@x')).toBe(`"'@x"`);
    expect(csvCell('Pièce "AX"')).toBe('"Pièce ""AX"""');
    expect(csvCell(null)).toBe('""');
    expect(csvCell(-0.5)).toBe('"-0.5"');
  });
});
