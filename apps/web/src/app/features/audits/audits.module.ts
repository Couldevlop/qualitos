import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { AuditsChecklistDialogComponent } from './pages/audits-checklist-dialog/audits-checklist-dialog.component';
import { AuditsCreateDialogComponent } from './pages/audits-create-dialog/audits-create-dialog.component';
import { AuditsDetailComponent } from './pages/audits-detail/audits-detail.component';
import { AuditsListComponent } from './pages/audits-list/audits-list.component';
import { AuditsResponseDialogComponent } from './pages/audits-response-dialog/audits-response-dialog.component';

const routes: Routes = [
  { path: '', component: AuditsListComponent },
  { path: ':id', component: AuditsDetailComponent }
];

@NgModule({
  declarations: [
    AuditsListComponent,
    AuditsDetailComponent,
    AuditsCreateDialogComponent,
    AuditsChecklistDialogComponent,
    AuditsResponseDialogComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AuditsModule {}
