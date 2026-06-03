import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { EhsDetailComponent } from './pages/ehs-detail/ehs-detail.component';
import { EhsEditDialogComponent } from './pages/ehs-edit-dialog/ehs-edit-dialog.component';
import { EhsLinkDialogComponent } from './pages/ehs-link-dialog/ehs-link-dialog.component';
import { EhsListComponent } from './pages/ehs-list/ehs-list.component';
import { EhsMitigateDialogComponent } from './pages/ehs-mitigate-dialog/ehs-mitigate-dialog.component';
import { EhsReportDialogComponent } from './pages/ehs-report-dialog/ehs-report-dialog.component';

const routes: Routes = [
  { path: '', component: EhsListComponent },
  { path: ':id', component: EhsDetailComponent }
];

@NgModule({
  declarations: [
    EhsListComponent,
    EhsDetailComponent,
    EhsReportDialogComponent,
    EhsEditDialogComponent,
    EhsMitigateDialogComponent,
    EhsLinkDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class EhsModule {}
