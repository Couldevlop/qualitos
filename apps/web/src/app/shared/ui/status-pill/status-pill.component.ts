import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

export type StatusTone = 'neutral' | 'accent' | 'success' | 'warn' | 'danger';

/**
 * Pill colorée pour les statuts métier (DRAFT, ACTIVE, NOTIFIED_REGULATOR…).
 */
@Component({
  selector: 'qos-status-pill',
  templateUrl: './status-pill.component.html',
  styleUrls: ['./status-pill.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class StatusPillComponent {
  @Input() label = '';
  @Input() tone: StatusTone = 'neutral';
  @Input() dot = true;
  @Input() icon?: string;
}
