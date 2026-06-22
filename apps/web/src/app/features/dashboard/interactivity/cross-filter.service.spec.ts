import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { take, toArray } from 'rxjs/operators';

import { CrossFilterService } from './cross-filter.service';

/**
 * GOLDEN front (§7.3) — fige le comportement de cross-filtering :
 * un clic pose un filtre partagé ; recliquer le même point le retire ;
 * clear() l'annule. C'est l'invariant central de l'interactivité dashboards.
 */
describe('CrossFilterService (golden cross-filtering)', () => {
  let svc: CrossFilterService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [CrossFilterService] });
    svc = TestBed.inject(CrossFilterService);
  });

  it('starts with no filter', () => {
    expect(svc.snapshot()).toBeNull();
  });

  it('GOLDEN: click → filter propagated → re-click clears → apply other → clear()', async () => {
    const emissions = firstValueFrom(svc.filter$.pipe(take(5), toArray()));

    // 1) clic sur "Machine" → filtre propagé
    svc.apply({ dimension: 'category', value: 'Machine', label: 'Machine' });
    expect(svc.snapshot()?.value).toBe('Machine');

    // 2) re-clic identique → toggle off (retiré)
    svc.apply({ dimension: 'category', value: 'Machine', label: 'Machine' });
    expect(svc.snapshot()).toBeNull();

    // 3) clic sur une autre catégorie
    svc.apply({ dimension: 'category', value: 'Matière', label: 'Matière' });
    expect(svc.snapshot()?.value).toBe('Matière');

    // 4) clear explicite
    svc.clear();
    expect(svc.snapshot()).toBeNull();

    const seq = await emissions;
    expect(seq.map(f => f?.value ?? null))
      .toEqual([null, 'Machine', null, 'Matière', null]);
  });

  it('active$ reflects whether a filter is set', async () => {
    const states = firstValueFrom(svc.active$.pipe(take(3), toArray()));
    svc.apply({ dimension: 'category', value: 'X', label: 'X' });
    svc.clear();
    expect(await states).toEqual([false, true, false]);
  });

  it('isHighlighted: everything highlighted without a filter', () => {
    expect(svc.isHighlighted('category', 'A')).toBe(true);
  });

  it('isHighlighted: only the selected value of the active dimension', () => {
    svc.apply({ dimension: 'category', value: 'Machine', label: 'Machine' });
    expect(svc.isHighlighted('category', 'Machine')).toBe(true);
    expect(svc.isHighlighted('category', 'Matière')).toBe(false);
    // a different dimension is unaffected
    expect(svc.isHighlighted('site', 'Lyon')).toBe(true);
  });

  it('clear() on empty state does not emit again', async () => {
    const emissions = firstValueFrom(svc.filter$.pipe(take(2), toArray()));
    svc.clear(); // no-op
    svc.apply({ dimension: 'category', value: 'Z', label: 'Z' });
    expect((await emissions).map(f => f?.value ?? null)).toEqual([null, 'Z']);
  });
});
