import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { CapaActionDialogComponent } from './pages/capa-action-dialog/capa-action-dialog.component';
import { CapaCreateDialogComponent } from './pages/capa-create-dialog/capa-create-dialog.component';
import { CapaDetailComponent } from './pages/capa-detail/capa-detail.component';
import { CapaEditDialogComponent } from './pages/capa-edit-dialog/capa-edit-dialog.component';
import { CapaListComponent } from './pages/capa-list/capa-list.component';

const routes: Routes = [
  { path: '', component: CapaListComponent },
  { path: ':id', component: CapaDetailComponent }
];

@NgModule({
  declarations: [
    CapaListComponent,
    CapaDetailComponent,
    CapaCreateDialogComponent,
    CapaActionDialogComponent,
    CapaEditDialogComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class CapaModule {}
