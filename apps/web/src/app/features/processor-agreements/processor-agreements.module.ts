import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { PaDetailComponent } from './pages/pa-detail/pa-detail.component';
import { PaDialogComponent } from './pages/pa-dialog/pa-dialog.component';
import { PaListComponent } from './pages/pa-list/pa-list.component';
import { PaTerminateDialogComponent } from './pages/pa-terminate-dialog/pa-terminate-dialog.component';

const routes: Routes = [
  { path: '', component: PaListComponent },
  { path: ':id', component: PaDetailComponent }
];

@NgModule({
  declarations: [
    PaListComponent,
    PaDetailComponent,
    PaDialogComponent,
    PaTerminateDialogComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class ProcessorAgreementsModule {}
