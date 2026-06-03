import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { ChangesApproverDialogComponent } from './pages/changes-approver-dialog/changes-approver-dialog.component';
import { ChangesCreateDialogComponent } from './pages/changes-create-dialog/changes-create-dialog.component';
import { ChangesDecisionDialogComponent } from './pages/changes-decision-dialog/changes-decision-dialog.component';
import { ChangesDetailComponent } from './pages/changes-detail/changes-detail.component';
import { ChangesImpactDialogComponent } from './pages/changes-impact-dialog/changes-impact-dialog.component';
import { ChangesImplementDialogComponent } from './pages/changes-implement-dialog/changes-implement-dialog.component';
import { ChangesListComponent } from './pages/changes-list/changes-list.component';

const routes: Routes = [
  { path: '', component: ChangesListComponent },
  { path: ':id', component: ChangesDetailComponent }
];

@NgModule({
  declarations: [
    ChangesListComponent,
    ChangesDetailComponent,
    ChangesCreateDialogComponent,
    ChangesApproverDialogComponent,
    ChangesDecisionDialogComponent,
    ChangesImpactDialogComponent,
    ChangesImplementDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class ChangesModule {}
