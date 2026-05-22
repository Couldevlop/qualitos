import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { IncCloseDialogComponent } from './pages/inc-close-dialog/inc-close-dialog.component';
import { IncDetailComponent } from './pages/inc-detail/inc-detail.component';
import { IncDetectDialogComponent } from './pages/inc-detect-dialog/inc-detect-dialog.component';
import { IncListComponent } from './pages/inc-list/inc-list.component';
import { IncNotifyDialogComponent } from './pages/inc-notify-dialog/inc-notify-dialog.component';

const routes: Routes = [
  { path: '', component: IncListComponent },
  { path: ':id', component: IncDetailComponent }
];

@NgModule({
  declarations: [
    IncListComponent,
    IncDetailComponent,
    IncDetectDialogComponent,
    IncNotifyDialogComponent,
    IncCloseDialogComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AiIncidentsModule {}
