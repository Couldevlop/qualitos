import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { CyiDetailComponent } from './pages/cyi-detail/cyi-detail.component';
import { CyiDetectDialogComponent } from './pages/cyi-detect-dialog/cyi-detect-dialog.component';
import { CyiLinkBreachDialogComponent } from './pages/cyi-link-breach-dialog/cyi-link-breach-dialog.component';
import { CyiListComponent } from './pages/cyi-list/cyi-list.component';
import { CyiMitigateDialogComponent } from './pages/cyi-mitigate-dialog/cyi-mitigate-dialog.component';
import { CyiNotificationDialogComponent } from './pages/cyi-notification-dialog/cyi-notification-dialog.component';
import { CyiSeverityDialogComponent } from './pages/cyi-severity-dialog/cyi-severity-dialog.component';
import { CyiTextDialogComponent } from './pages/cyi-text-dialog/cyi-text-dialog.component';

const routes: Routes = [
  { path: '', component: CyiListComponent },
  { path: ':id', component: CyiDetailComponent }
];

@NgModule({
  declarations: [
    CyiListComponent,
    CyiDetailComponent,
    CyiDetectDialogComponent,
    CyiMitigateDialogComponent,
    CyiNotificationDialogComponent,
    CyiTextDialogComponent,
    CyiSeverityDialogComponent,
    CyiLinkBreachDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class CyberIncidentsModule {}
