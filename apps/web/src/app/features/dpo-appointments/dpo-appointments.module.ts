import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { DpoActivateDialogComponent } from './pages/dpo-activate-dialog/dpo-activate-dialog.component';
import { DpoDetailComponent } from './pages/dpo-detail/dpo-detail.component';
import { DpoEditDialogComponent } from './pages/dpo-edit-dialog/dpo-edit-dialog.component';
import { DpoEndDialogComponent } from './pages/dpo-end-dialog/dpo-end-dialog.component';
import { DpoListComponent } from './pages/dpo-list/dpo-list.component';
import { DpoProposeDialogComponent } from './pages/dpo-propose-dialog/dpo-propose-dialog.component';

const routes: Routes = [
  { path: '', component: DpoListComponent },
  { path: ':id', component: DpoDetailComponent }
];

@NgModule({
  declarations: [
    DpoListComponent,
    DpoDetailComponent,
    DpoProposeDialogComponent,
    DpoEditDialogComponent,
    DpoActivateDialogComponent,
    DpoEndDialogComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class DpoAppointmentsModule {}
