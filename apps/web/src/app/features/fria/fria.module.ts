import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { FriaApproveDialogComponent } from './pages/fria-approve-dialog/fria-approve-dialog.component';
import { FriaDetailComponent } from './pages/fria-detail/fria-detail.component';
import { FriaDraftDialogComponent } from './pages/fria-draft-dialog/fria-draft-dialog.component';
import { FriaEditDialogComponent } from './pages/fria-edit-dialog/fria-edit-dialog.component';
import { FriaListComponent } from './pages/fria-list/fria-list.component';
import { FriaReasonDialogComponent } from './pages/fria-reason-dialog/fria-reason-dialog.component';

const routes: Routes = [
  { path: '', component: FriaListComponent },
  { path: ':id', component: FriaDetailComponent }
];

@NgModule({
  declarations: [
    FriaListComponent,
    FriaDetailComponent,
    FriaDraftDialogComponent,
    FriaEditDialogComponent,
    FriaApproveDialogComponent,
    FriaReasonDialogComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class FriaModule {}
