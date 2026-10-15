import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { TrxDetailComponent } from './pages/trx-detail/trx-detail.component';
import { TrxDialogComponent } from './pages/trx-dialog/trx-dialog.component';
import { TrxListComponent } from './pages/trx-list/trx-list.component';
import { TrxReasonDialogComponent } from './pages/trx-reason-dialog/trx-reason-dialog.component';
import { PersonNameModule } from '../../shared/person-name/person-name.module';

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
  imports: [PersonNameModule, SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class TransfersModule {}
