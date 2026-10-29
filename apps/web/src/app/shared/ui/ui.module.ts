import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { IconNoTranslateModule } from '../icon-no-translate/icon-no-translate.module';
import { RouterModule } from '@angular/router';

import { EchartComponent } from './echart/echart.component';
import { EnumLabelPipe, OpenLabelPipe } from './enum-label/enum-label.pipe';
import { FormDialogComponent } from './form-dialog/form-dialog.component';
import { KpiCardComponent } from './kpi-card/kpi-card.component';
import { ListSearchComponent } from './list-search/list-search.component';
import { LicenseBannerComponent } from './license-banner/license-banner.component';
import { LocaleSwitcherComponent } from './locale-switcher/locale-switcher.component';
import { PageHeaderComponent } from './page-header/page-header.component';
import { PanelComponent } from './panel/panel.component';
import { ScopeNoteComponent } from './scope-note/scope-note.component';
import { NcCategoryLabelPipe, SeverityLabelPipe } from './severity-label/severity-label.pipe';
import { StatusLabelPipe } from './status-label/status-label.pipe';
import { StatusPillComponent } from './status-pill/status-pill.component';
import { ThemeToggleComponent } from './theme-toggle/theme-toggle.component';

const COMPONENTS = [
  KpiCardComponent,
  PageHeaderComponent,
  PanelComponent,
  StatusPillComponent,
  ThemeToggleComponent,
  LocaleSwitcherComponent,
  EchartComponent,
  FormDialogComponent,
  ScopeNoteComponent,
  LicenseBannerComponent,
  ListSearchComponent,
  StatusLabelPipe,
  SeverityLabelPipe,
  NcCategoryLabelPipe,
  EnumLabelPipe,
  OpenLabelPipe
];

/**
 * Design system QualitOS — composants UI premium réutilisables.
 * Import dans un feature module via `imports: [UiModule]`.
 */
@NgModule({
  declarations: COMPONENTS,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatButtonModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatInputModule, MatMenuModule,
    MatProgressSpinnerModule,
    IconNoTranslateModule
  ],
  exports: [...COMPONENTS, IconNoTranslateModule]
})
export class UiModule {}
