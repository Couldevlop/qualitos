import { of, throwError } from 'rxjs';

import { ActivityFeedService } from '../activity-feed.service';
import { ActivityEntry } from '../activity-feed.types';
import { ActivityFeedWidgetComponent } from './activity-feed-widget.component';

function entry(partial: Partial<ActivityEntry> = {}): ActivityEntry {
  return {
    id: 'id', sequenceNo: 1, occurredAt: null, recordedAt: null,
    action: 'capa.created', resourceType: 'capa', resourceId: null,
    actorUserId: null, summary: 'résumé', ...partial
  };
}

describe('ActivityFeedWidgetComponent', () => {

  function make(service: Partial<ActivityFeedService>): ActivityFeedWidgetComponent {
    return new ActivityFeedWidgetComponent(service as ActivityFeedService);
  }

  it('charge les entrées et termine le loading (succès)', () => {
    const items = [entry(), entry({ id: 'b', sequenceNo: 2 })];
    const c = make({ recent: () => of(items) });
    c.ngOnInit();
    expect(c.loading).toBeFalse();
    expect(c.errored).toBeFalse();
    expect(c.entries.length).toBe(2);
  });

  it('passe la limite au service', () => {
    const spy = jasmine.createSpy('recent').and.returnValue(of([]));
    const c = make({ recent: spy });
    c.limit = 5;
    c.ngOnInit();
    expect(spy).toHaveBeenCalledWith(5);
  });

  it('gère l\'erreur (errored=true, loading=false)', () => {
    const c = make({ recent: () => throwError(() => new Error('x')) });
    c.ngOnInit();
    expect(c.errored).toBeTrue();
    expect(c.loading).toBeFalse();
    expect(c.entries.length).toBe(0);
  });

  describe('iconFor', () => {
    const c = make({ recent: () => of([]) });
    it('mappe par resourceType connu', () => {
      expect(c.iconFor(entry({ resourceType: 'audit' }))).toBe('fact_check');
      expect(c.iconFor(entry({ resourceType: 'CAPA' }))).toBe('engineering');
    });
    it('retombe sur le préfixe de l\'action si pas de resourceType', () => {
      expect(c.iconFor(entry({ resourceType: null, action: 'pdca.cycle.created' }))).toBe('autorenew');
    });
    it('icône par défaut si inconnu', () => {
      expect(c.iconFor(entry({ resourceType: 'inconnu', action: 'x.y' }))).toBe('bookmark_border');
    });
    it('icône par défaut si resourceType nul ET action vide (fallback "")', () => {
      expect(c.iconFor(entry({ resourceType: null, action: '' }))).toBe('bookmark_border');
    });
  });

  describe('labelFor', () => {
    const c = make({ recent: () => of([]) });
    it('renvoie le résumé si présent', () => {
      expect(c.labelFor(entry({ summary: 'Hello' }))).toBe('Hello');
    });
    it('retombe sur l\'action si résumé vide/absent', () => {
      expect(c.labelFor(entry({ summary: '   ', action: 'a.b' }))).toBe('a.b');
      expect(c.labelFor(entry({ summary: null, action: 'a.b' }))).toBe('a.b');
    });
  });

  describe('timeAgo', () => {
    const c = make({ recent: () => of([]) });
    const now = new Date('2026-06-04T12:00:00Z');
    it('vide si null ou invalide', () => {
      expect(c.timeAgo(null, now)).toBe('');
      expect(c.timeAgo('pas-une-date', now)).toBe('');
    });
    it('"à l\'instant" sous 60 s', () => {
      expect(c.timeAgo('2026-06-04T11:59:30Z', now)).toBe("à l'instant");
    });
    it('minutes / heures / jours', () => {
      expect(c.timeAgo('2026-06-04T11:30:00Z', now)).toBe('il y a 30 min');
      expect(c.timeAgo('2026-06-04T09:00:00Z', now)).toBe('il y a 3 h');
      expect(c.timeAgo('2026-06-01T12:00:00Z', now)).toBe('il y a 3 j');
    });
    it('utilise la date courante par défaut (sans argument now)', () => {
      // date très ancienne → forcément "il y a … j", quel que soit "maintenant"
      expect(c.timeAgo('2000-01-01T00:00:00Z')).toContain('il y a');
    });
  });

  it('trackById renvoie l\'id', () => {
    const c = make({ recent: () => of([]) });
    expect(c.trackById(0, entry({ id: 'z' }))).toBe('z');
  });
});
