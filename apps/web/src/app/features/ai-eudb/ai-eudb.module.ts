import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { EudbDetailComponent } from './pages/eudb-detail/eudb-detail.component';
import { EudbDraftDialogComponent } from './pages/eudb-draft-dialog/eudb-draft-dialog.component';
import { EudbEditDialogComponent } from './pages/eudb-edit-dialog/eudb-edit-dialog.component';
import { EudbListComponent } from './pages/eudb-list/eudb-list.component';
import { EudbReasonDialogComponent } from './pages/eudb-reason-dialog/eudb-reason-dialog.component';
import { EudbRegisterDialogComponent } from './pages/eudb-register-dialog/eudb-register-dialog.component';
import { EudbUpdateDialogComponent } from './pages/eudb-update-dialog/eudb-update-dialog.component';

const routes: Routes = [
  { path: '', component: EudbListComponent },
  { path: ':id', component: EudbDetailComponent }
];

@NgModule({
  declarations: [
    EudbListComponent,
    EudbDetailComponent,
    EudbDraftDialogComponent,
    EudbEditDialogComponent,
    EudbRegisterDialogComponent,
    EudbUpdateDialogComponent,
    EudbReasonDialogComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AiEudbModule {}
