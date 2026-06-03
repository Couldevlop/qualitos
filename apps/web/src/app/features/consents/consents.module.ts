import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { ConsentsDetailComponent } from './pages/consents-detail/consents-detail.component';
import { ConsentsGrantDialogComponent } from './pages/consents-grant-dialog/consents-grant-dialog.component';
import { ConsentsSearchComponent } from './pages/consents-search/consents-search.component';
import { ConsentsWithdrawDialogComponent } from './pages/consents-withdraw-dialog/consents-withdraw-dialog.component';

const routes: Routes = [
  { path: '', component: ConsentsSearchComponent },
  { path: ':id', component: ConsentsDetailComponent }
];

@NgModule({
  declarations: [
    ConsentsSearchComponent,
    ConsentsDetailComponent,
    ConsentsGrantDialogComponent,
    ConsentsWithdrawDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class ConsentsModule {}
