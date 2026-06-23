import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { CirclesCreateDialogComponent } from './pages/circles-create-dialog/circles-create-dialog.component';
import { CirclesDetailComponent } from './pages/circles-detail/circles-detail.component';
import { CirclesEditDialogComponent } from './pages/circles-edit-dialog/circles-edit-dialog.component';
import { CirclesListComponent } from './pages/circles-list/circles-list.component';
import { CirclesMeetingDialogComponent } from './pages/circles-meeting-dialog/circles-meeting-dialog.component';
import { CirclesMemberDialogComponent } from './pages/circles-member-dialog/circles-member-dialog.component';
import { CirclesProposalDialogComponent } from './pages/circles-proposal-dialog/circles-proposal-dialog.component';
import { CirclesMinutesDialogComponent } from './pages/circles-minutes-dialog/circles-minutes-dialog.component';
import { CirclesRejectDialogComponent } from './pages/circles-reject-dialog/circles-reject-dialog.component';
import { CirclesImpactDialogComponent } from './pages/circles-impact-dialog/circles-impact-dialog.component';

const routes: Routes = [
  { path: '', component: CirclesListComponent },
  { path: ':id', component: CirclesDetailComponent }
];

@NgModule({
  declarations: [
    CirclesListComponent,
    CirclesDetailComponent,
    CirclesCreateDialogComponent,
    CirclesMemberDialogComponent,
    CirclesEditDialogComponent,
    CirclesMeetingDialogComponent,
    CirclesProposalDialogComponent,
    CirclesMinutesDialogComponent,
    CirclesRejectDialogComponent,
    CirclesImpactDialogComponent
  ],
  imports: [SharedModule, UiModule, FormsModule, RouterModule.forChild(routes)]
})
export class CirclesModule {}
