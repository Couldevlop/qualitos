import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { EchartComponent } from './echart/echart.component';
import { KpiCardComponent } from './kpi-card/kpi-card.component';
import { PageHeaderComponent } from './page-header/page-header.component';
import { PanelComponent } from './panel/panel.component';
import { StatusPillComponent } from './status-pill/status-pill.component';
import { ThemeToggleComponent } from './theme-toggle/theme-toggle.component';

const COMPONENTS = [
  KpiCardComponent,
  PageHeaderComponent,
  PanelComponent,
  StatusPillComponent,
  ThemeToggleComponent,
  EchartComponent
];

/**
 * Design system QualitOS — composants UI premium réutilisables.
 * Import dans un feature module via `imports: [UiModule]`.
 */
@NgModule({
  declarations: COMPONENTS,
  imports: [CommonModule, RouterModule],
  exports: COMPONENTS
})
export class UiModule {}
