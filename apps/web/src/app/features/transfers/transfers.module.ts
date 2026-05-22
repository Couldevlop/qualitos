import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { TrxDetailComponent } from './pages/trx-detail/trx-detail.component';
import { TrxDialogComponent } from './pages/trx-dialog/trx-dialog.component';
import { TrxListComponent } from './pages/trx-list/trx-list.component';
import { TrxReasonDialogComponent } from './pages/trx-reason-dialog/trx-reason-dialog.component';

const routes: Routes = [
  { path: '', component: TrxListComponent },
  { path: ':id', component: TrxDetailComponent }
];

@NgModule({
  declarations: [
    TrxListComponent,
    TrxDetailComponent,
    TrxDialogComponent,
    TrxReasonDialogComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class TransfersModule {}
