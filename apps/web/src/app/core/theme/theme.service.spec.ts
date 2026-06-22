import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';

import { ThemeService } from './theme.service';

const STORAGE_KEY = 'qos.theme';

/** MediaQueryList contrôlable par les tests. */
class FakeMediaQueryList {
  matches = false;
  private readonly listeners: Array<(e: MediaQueryListEvent) => void> = [];
  addEventListener(_type: 'change', cb: (e: MediaQueryListEvent) => void): void {
    this.listeners.push(cb);
  }
  emit(matches: boolean): void {
    this.matches = matches;
    this.listeners.forEach(l => l({ matches } as MediaQueryListEvent));
  }
}

/** localStorage en mémoire. */
class FakeStorage {
  private readonly map = new Map<string, string>();
  getItem(k: string): string | null { return this.map.has(k) ? this.map.get(k)! : null; }
  setItem(k: string, v: string): void { this.map.set(k, v); }
}

function makeDocument(opts: { stored?: string; prefersDark?: boolean } = {}): {
  doc: Document;
  classes: Set<string>;
  media: FakeMediaQueryList;
  storage: FakeStorage;
} {
  const classes = new Set<string>();
  const media = new FakeMediaQueryList();
  media.matches = !!opts.prefersDark;
  const storage = new FakeStorage();
  if (opts.stored) storage.setItem(STORAGE_KEY, opts.stored);

  const documentElement = {
    classList: {
      add: (c: string) => classes.add(c),
      remove: (...cs: string[]) => cs.forEach(c => classes.delete(c))
    }
  };
  const doc = {
    documentElement,
    defaultView: {
      localStorage: storage,
      matchMedia: (_q: string) => media
    }
  } as unknown as Document;
  return { doc, classes, media, storage };
}

function build(opts: { stored?: string; prefersDark?: boolean } = {}): {
  service: ThemeService;
  ctx: ReturnType<typeof makeDocument>;
} {
  const ctx = makeDocument(opts);
  TestBed.configureTestingModule({
    providers: [{ provide: DOCUMENT, useValue: ctx.doc }]
  });
  const service = TestBed.inject(ThemeService);
  return { service, ctx };
}

describe('ThemeService', () => {

  afterEach(() => TestBed.resetTestingModule());

  it('defaults to system preference and applies the light class when system is light', () => {
    const { service, ctx } = build({ prefersDark: false });
    expect(service.currentPreference()).toBe('system');
    expect(service.currentMode()).toBe('light');
    expect(ctx.classes.has('qos-theme-light')).toBeTrue();
  });

  it('resolves system preference to dark when prefers-color-scheme is dark', () => {
    const { service, ctx } = build({ prefersDark: true });
    expect(service.currentMode()).toBe('dark');
    expect(ctx.classes.has('qos-theme-dark')).toBeTrue();
  });

  it('reads a stored explicit preference on construction', () => {
    const { service, ctx } = build({ stored: 'dark', prefersDark: false });
    expect(service.currentPreference()).toBe('dark');
    expect(service.currentMode()).toBe('dark');
    expect(ctx.classes.has('qos-theme-dark')).toBeTrue();
    expect(ctx.classes.has('qos-theme-light')).toBeFalse();
  });

  it('setPreference persists the choice and swaps the html class', () => {
    const { service, ctx } = build({ prefersDark: false });
    service.setPreference('dark');
    expect(service.currentMode()).toBe('dark');
    expect(ctx.classes.has('qos-theme-dark')).toBeTrue();
    expect(ctx.storage.getItem(STORAGE_KEY)).toBe('dark');
  });

  it('setPreference is a no-op when the preference is unchanged', () => {
    const { service } = build({ stored: 'light', prefersDark: false });
    const emissions: string[] = [];
    service.preference().subscribe(p => emissions.push(p));
    emissions.length = 0; // drop the replayed initial value
    service.setPreference('light');
    expect(emissions).toEqual([]);
  });

  it('toggle flips light to dark and back', () => {
    const { service } = build({ stored: 'light', prefersDark: false });
    service.toggle();
    expect(service.currentMode()).toBe('dark');
    service.toggle();
    expect(service.currentMode()).toBe('light');
  });

  it('follows prefers-color-scheme changes while in system mode', () => {
    const { service, ctx } = build({ prefersDark: false });
    expect(service.currentMode()).toBe('light');
    ctx.media.emit(true);
    expect(service.currentMode()).toBe('dark');
    expect(ctx.classes.has('qos-theme-dark')).toBeTrue();
  });

  it('ignores system media changes once an explicit preference is set', () => {
    const { service, ctx } = build({ prefersDark: false });
    service.setPreference('light');
    ctx.media.emit(true); // system goes dark, but user pinned light
    expect(service.currentMode()).toBe('light');
  });

  it('mode() emits the current mode to subscribers', () => {
    const { service } = build({ stored: 'dark', prefersDark: false });
    let mode: string | undefined;
    service.mode().subscribe(m => (mode = m));
    expect(mode).toBe('dark');
  });
});
