import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';

import { ThemeMode, ThemePreference, ThemeService } from '../../../core/theme/theme.service';

/**
 * Toggle 3 états : light / dark / system.
 * Affiche l'état actif et permet le cycle au clic.
 */
@Component({
  selector: 'qos-theme-toggle',
  templateUrl: './theme-toggle.component.html',
  styleUrls: ['./theme-toggle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class ThemeToggleComponent implements OnInit {

  preference$!: Observable<ThemePreference>;
  mode$!: Observable<ThemeMode>;

  constructor(private readonly theme: ThemeService) {}

  ngOnInit(): void {
    this.preference$ = this.theme.preference();
    this.mode$ = this.theme.mode();
  }

  set(pref: ThemePreference): void { this.theme.setPreference(pref); }
}
