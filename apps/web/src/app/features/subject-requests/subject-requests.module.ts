import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { SrCompleteDialogComponent } from './pages/sr-complete-dialog/sr-complete-dialog.component';
import { SrDetailComponent } from './pages/sr-detail/sr-detail.component';
import { SrExtendDialogComponent } from './pages/sr-extend-dialog/sr-extend-dialog.component';
import { SrListComponent } from './pages/sr-list/sr-list.component';
import { SrReceiveDialogComponent } from './pages/sr-receive-dialog/sr-receive-dialog.component';
import { SrRejectDialogComponent } from './pages/sr-reject-dialog/sr-reject-dialog.component';

const routes: Routes = [
  { path: '', component: SrListComponent },
  { path: ':id', component: SrDetailComponent }
];

@NgModule({
  declarations: [
    SrListComponent,
    SrDetailComponent,
    SrReceiveDialogComponent,
    SrCompleteDialogComponent,
    SrRejectDialogComponent,
    SrExtendDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class SubjectRequestsModule {}
