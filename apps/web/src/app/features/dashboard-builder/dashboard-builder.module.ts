/**
 * Dashboard-builder module — NgModule + separate HTML/SCSS, NEVER standalone.
 * (CLAUDE.md project memory: load-bearing rule.)
 *
 * Drag &amp; drop avancé (§7.3) : grille interactive angular-gridster2,
 * palette glisser-déposer, rendu ECharts via le design system (UiModule).
 */
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { GridsterModule } from 'angular-gridster2';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { DashboardBuilderService } from './application/dashboard-builder.service';
import { WidgetCatalogService } from './application/widget-catalog.service';
import { WidgetRenderService } from './application/widget-render.service';
import { DASHBOARD_LAYOUT_REPOSITORY } from './domain/dashboard-builder.tokens';
import { DashboardHttpRepository } from './infrastructure/dashboard-http.repository';
import { DashboardEditorComponent } from './presentation/dashboard-editor/dashboard-editor.component';
import { DashboardListComponent } from './presentation/dashboard-list/dashboard-list.component';
import { WidgetConfigPanelComponent } from './presentation/widget-config/widget-config-panel.component';
import { WidgetHostComponent } from './presentation/widgets/widget-host.component';

const routes: Routes = [
  { path: '', component: DashboardListComponent },
  { path: 'new', component: DashboardEditorComponent },
  { path: ':id', component: DashboardEditorComponent }
];

@NgModule({
  declarations: [
    DashboardListComponent,
    DashboardEditorComponent,
    WidgetHostComponent,
    WidgetConfigPanelComponent
  ],
  imports: [
    SharedModule,
    UiModule,
    FormsModule,
    GridsterModule,
    RouterModule.forChild(routes)
  ],
  providers: [
    DashboardBuilderService,
    WidgetCatalogService,
    WidgetRenderService,
    DashboardHttpRepository,
    {
      provide: DASHBOARD_LAYOUT_REPOSITORY,
      useExisting: DashboardHttpRepository
    }
  ]
})
export class DashboardBuilderModule {}
