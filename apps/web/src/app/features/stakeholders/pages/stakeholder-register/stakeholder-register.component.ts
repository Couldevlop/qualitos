import { Component, EventEmitter, Input, Output } from '@angular/core';

import {
  categoryLabel,
  impactChip,
  impactLabel,
  relevanceLabel,
  sideLabel
} from '../../stakeholders.labels';
import { StakeholderRow, StakeholderSide } from '../../stakeholders.types';

/**
 * Le registre, en tableau : qui, de quelle famille, dedans ou dehors, quelle
 * criticité, quand a eu lieu la dernière revue.
 *
 * <p>Les lignes arrivent DÉJÀ TRIÉES par le serveur, du plus critique au moins :
 * l'ordre porte du sens, et deux écrans ne doivent pas en proposer deux.
 */
@Component({
  selector: 'qos-stakeholder-register',
  templateUrl: './stakeholder-register.component.html',
  styleUrls: ['./stakeholder-register.component.scss'],
  standalone: false
})
export class StakeholderRegisterComponent {

  @Input() rows: StakeholderRow[] = [];
  @Input() loading = false;
  @Input() side: StakeholderSide | '' = '';
  @Input() search = '';

  @Output() readonly filtersChange = new EventEmitter<{ side: StakeholderSide | ''; search: string }>();
  @Output() readonly opened = new EventEmitter<string>();

  readonly columns = ['name', 'category', 'side', 'criticality', 'relevance', 'requirements', 'review'];

  readonly categoryLabel = categoryLabel;
  readonly sideLabel = sideLabel;
  readonly impactLabel = impactLabel;
  readonly relevanceLabel = relevanceLabel;
  readonly impactChip = impactChip;

  get hasFilters(): boolean {
    return !!this.side || !!this.search.trim();
  }

  onSide(side: StakeholderSide | ''): void {
    this.filtersChange.emit({ side, search: this.search });
  }

  onSearch(search: string): void {
    this.filtersChange.emit({ side: this.side, search });
  }

  clear(): void {
    this.filtersChange.emit({ side: '', search: '' });
  }

  trackById(_index: number, row: StakeholderRow): string {
    return row.id;
  }
}
