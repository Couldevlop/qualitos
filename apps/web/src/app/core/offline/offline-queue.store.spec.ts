import {
  compareQueued,
  InMemoryQueueStore,
  offlineQueueStoreFactory,
  QueuedOperation
} from './offline-queue.store';

function op(partial: Partial<QueuedOperation>): QueuedOperation {
  return {
    id: partial.id ?? 'op-' + Math.random().toString(36).slice(2, 8),
    method: partial.method ?? 'POST',
    url: partial.url ?? '/api/v1/fives/audits',
    body: partial.body ?? {},
    label: partial.label ?? 'Op',
    queuedAt: partial.queuedAt ?? '2026-06-22T10:00:00.000Z',
    seq: partial.seq
  };
}

describe('compareQueued', () => {
  it('orders by seq first when both have a seq', () => {
    const a = op({ seq: 1, queuedAt: '2026-06-22T10:00:05.000Z' });
    const b = op({ seq: 2, queuedAt: '2026-06-22T10:00:00.000Z' });
    expect(compareQueued(a, b)).toBeLessThan(0);
    expect(compareQueued(b, a)).toBeGreaterThan(0);
  });

  it('falls back to queuedAt when seq is missing (legacy operations)', () => {
    const earlier = op({ seq: undefined, queuedAt: '2026-06-22T10:00:00.000Z' });
    const later = op({ seq: undefined, queuedAt: '2026-06-22T10:00:01.000Z' });
    expect(compareQueued(earlier, later)).toBeLessThan(0);
  });

  it('uses queuedAt when only one operation has a seq', () => {
    const withSeq = op({ seq: 5, queuedAt: '2026-06-22T10:00:00.000Z' });
    const legacy = op({ seq: undefined, queuedAt: '2026-06-22T10:00:09.000Z' });
    // seq alone is ignored when the other lacks it → queuedAt decides
    expect(compareQueued(withSeq, legacy)).toBeLessThan(0);
  });

  it('tie-breaks identical queuedAt by seq fallback (0 when missing)', () => {
    const a = op({ seq: undefined, queuedAt: '2026-06-22T10:00:00.000Z' });
    const b = op({ seq: 3, queuedAt: '2026-06-22T10:00:00.000Z' });
    expect(compareQueued(a, b)).toBeLessThan(0);
  });

  it('returns 0 for two equivalent operations', () => {
    const a = op({ seq: 7, queuedAt: '2026-06-22T10:00:00.000Z' });
    const b = op({ seq: 7, queuedAt: '2026-06-22T10:00:00.000Z' });
    expect(compareQueued(a, b)).toBe(0);
  });

  it('sorts a mixed list into exact enqueue order', () => {
    const list = [
      op({ id: 'c', seq: 3 }),
      op({ id: 'a', seq: 1 }),
      op({ id: 'b', seq: 2 })
    ];
    const sorted = [...list].sort(compareQueued).map(o => o.id);
    expect(sorted).toEqual(['a', 'b', 'c']);
  });
});

describe('InMemoryQueueStore', () => {
  let store: InMemoryQueueStore;

  beforeEach(() => {
    store = new InMemoryQueueStore();
  });

  it('returns an empty list initially', async () => {
    expect(await store.loadAll()).toEqual([]);
  });

  it('appends operations and returns them sorted by canonical order', async () => {
    await store.append(op({ id: 'b', seq: 2 }));
    await store.append(op({ id: 'a', seq: 1 }));
    const all = await store.loadAll();
    expect(all.map(o => o.id)).toEqual(['a', 'b']);
  });

  it('overwrites an operation with the same id (idempotent append)', async () => {
    await store.append(op({ id: 'x', label: 'First', seq: 1 }));
    await store.append(op({ id: 'x', label: 'Second', seq: 1 }));
    const all = await store.loadAll();
    expect(all.length).toBe(1);
    expect(all[0].label).toBe('Second');
  });

  it('removes an operation by id', async () => {
    await store.append(op({ id: 'x', seq: 1 }));
    await store.append(op({ id: 'y', seq: 2 }));
    await store.remove('x');
    const all = await store.loadAll();
    expect(all.map(o => o.id)).toEqual(['y']);
  });

  it('ignores removal of an unknown id', async () => {
    await store.append(op({ id: 'x', seq: 1 }));
    await store.remove('nope');
    expect((await store.loadAll()).length).toBe(1);
  });
});

describe('offlineQueueStoreFactory', () => {
  it('returns an OfflineQueueStore implementation', () => {
    const created = offlineQueueStoreFactory();
    expect(created).toBeTruthy();
    expect(typeof created.loadAll).toBe('function');
    expect(typeof created.append).toBe('function');
    expect(typeof created.remove).toBe('function');
  });
});
