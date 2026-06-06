import { Component, Input, OnInit } from '@angular/core';

import { ActivityFeedService } from '../activity-feed.service';
import { ActivityEntry } from '../activity-feed.types';

/**
 * Widget « activité récente » : affiche les derniers événements d'audit du tenant
 * (read-model projeté par le consommateur Kafka). Présentationnel ; l'isolation
 * tenant et l'auth sont gérées côté service/serveur (OWASP A01/A07).
 */
@Component({
  selector: 'qos-activity-feed-widget',
  templateUrl: './activity-feed-widget.component.html',
  styleUrls: ['./activity-feed-widget.component.scss'],
  standalone: false
})
export class ActivityFeedWidgetComponent implements OnInit {

  /** Nombre d'éléments affichés. */
  @Input() limit = 8;

  entries: ActivityEntry[] = [];
  loading = true;
  errored = false;

  constructor(private readonly service: ActivityFeedService) {}

  ngOnInit(): void {
    this.service.recent(this.limit).subscribe({
      next: items => {
        this.entries = items;
        this.loading = false;
      },
      error: () => {
        this.errored = true;
        this.loading = false;
      }
    });
  }

  /** Icône Material en fonction du type de ressource (puis de l'action). */
  iconFor(entry: ActivityEntry): string {
    const key = (entry.resourceType || entry.action.split('.')[0] || '').toLowerCase();
    const map: Record<string, string> = {
      capa: 'engineering',
      audit: 'fact_check',
      audits: 'fact_check',
      documents: 'description',
      document: 'description',
      pdca: 'autorenew',
      ishikawa: 'account_tree',
      fives: 'check_circle',
      dmaic: 'analytics',
      circles: 'groups',
      fmea: 'warning',
      kpi: 'monitoring',
      standards: 'workspace_premium',
      demo: 'bolt'
    };
    return map[key] ?? 'bookmark_border';
  }

  /** Libellé court affiché (résumé, sinon l'action). */
  labelFor(entry: ActivityEntry): string {
    return entry.summary && entry.summary.trim().length > 0 ? entry.summary : entry.action;
  }

  /** Temps relatif « il y a … » à partir d'un ISO-8601 (pur, testable). */
  timeAgo(iso: string | null, now: Date = new Date()): string {
    if (!iso) {
      return '';
    }
    const then = Date.parse(iso);
    if (Number.isNaN(then)) {
      return '';
    }
    const sec = Math.max(0, Math.floor((now.getTime() - then) / 1000));
    if (sec < 60) {
      return $localize`:@@activity-feed.widget.time-now:à l'instant`;
    }
    const min = Math.floor(sec / 60);
    if (min < 60) {
      return $localize`:@@activity-feed.widget.time-min:il y a ${min}:count: min`;
    }
    const hrs = Math.floor(min / 60);
    if (hrs < 24) {
      return $localize`:@@activity-feed.widget.time-hrs:il y a ${hrs}:count: h`;
    }
    const days = Math.floor(hrs / 24);
    return $localize`:@@activity-feed.widget.time-days:il y a ${days}:count: j`;
  }

  trackById(_index: number, entry: ActivityEntry): string {
    return entry.id;
  }
}
