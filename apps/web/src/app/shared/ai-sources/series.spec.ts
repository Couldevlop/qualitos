import { alignMatrix, complaintLine, toSeries } from './series';

describe('sources des analyses IA', () => {
  it('ordonne une série par période et écarte les valeurs non numériques', () => {
    const s = toSeries([
      { periodStart: '2026-03-01', value: 3 },
      { periodStart: '2026-01-01', value: 1 },
      { periodStart: '2026-02-01', value: Number.NaN },
      { periodStart: '2026-02-15', value: 2 }
    ]);
    expect(s.values).toEqual([1, 2, 3]);
    expect(s.from).toBe('2026-01-01');
    expect(s.to).toBe('2026-03-01');
  });

  it('rend une série vide sans dates', () => {
    expect(toSeries([])).toEqual({ values: [], from: null, to: null });
  });

  it('aligne les indicateurs sur leurs seules périodes communes', () => {
    const m = alignMatrix([
      [{ periodStart: 'b', value: 2 }, { periodStart: 'a', value: 1 }, { periodStart: 'c', value: 3 }],
      [{ periodStart: 'a', value: 10 }, { periodStart: 'c', value: 30 }]
    ]);
    expect(m.periods).toEqual(['a', 'c']);
    expect(m.rows).toEqual([[1, 10], [3, 30]]);
  });

  it('ne fabrique rien sans colonne', () => {
    expect(alignMatrix([])).toEqual({ rows: [], periods: [] });
  });

  it('met une réclamation sur une ligne, titre puis description', () => {
    expect(complaintLine('Colis abîmé', ' Carton\nécrasé  à la livraison ')).toBe('Colis abîmé — Carton écrasé à la livraison');
    expect(complaintLine('Retard', null)).toBe('Retard');
  });
});
