import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterModule } from '@angular/router';

import { EchartComponent } from './echart/echart.component';
import { FormDialogComponent } from './form-dialog/form-dialog.component';
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
  EchartComponent,
  FormDialogComponent
];

/**
 * Design system QualitOS — composants UI premium réutilisables.
 * Import dans un feature module via `imports: [UiModule]`.
 */
@NgModule({
  declarations: COMPONENTS,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatButtonModule, MatDialogModule, MatIconModule, MatProgressSpinnerModule
  ],
  exports: COMPONENTS
})
export class UiModule {}
