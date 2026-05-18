/**
 * Dashboard-builder module — NgModule + separate HTML/SCSS, NEVER standalone.
 * (CLAUDE.md project memory: load-bearing rule.)
 */
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { DashboardBuilderService } from './application/dashboard-builder.service';
import { DASHBOARD_LAYOUT_REPOSITORY } from './domain/dashboard-builder.tokens';
import { DashboardHttpRepository } from './infrastructure/dashboard-http.repository';
import { DashboardEditorComponent } from './presentation/dashboard-editor/dashboard-editor.component';
import { DashboardListComponent } from './presentation/dashboard-list/dashboard-list.component';
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
    WidgetHostComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)],
  providers: [
    DashboardBuilderService,
    DashboardHttpRepository,
    {
      provide: DASHBOARD_LAYOUT_REPOSITORY,
      useExisting: DashboardHttpRepository
    }
  ]
})
export class DashboardBuilderModule {}
