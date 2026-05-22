import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { PnDetailComponent } from './pages/pn-detail/pn-detail.component';
import { PnDialogComponent } from './pages/pn-dialog/pn-dialog.component';
import { PnListComponent } from './pages/pn-list/pn-list.component';

const routes: Routes = [
  { path: '', component: PnListComponent },
  { path: ':id', component: PnDetailComponent }
];

@NgModule({
  declarations: [
    PnListComponent,
    PnDetailComponent,
    PnDialogComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class PrivacyNoticesModule {}
