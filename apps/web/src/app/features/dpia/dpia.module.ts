import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { DpiaCreateDialogComponent } from './pages/dpia-create-dialog/dpia-create-dialog.component';
import { DpiaDetailComponent } from './pages/dpia-detail/dpia-detail.component';
import { DpiaEditDialogComponent } from './pages/dpia-edit-dialog/dpia-edit-dialog.component';
import { DpiaListComponent } from './pages/dpia-list/dpia-list.component';
import { DpiaOpinionDialogComponent } from './pages/dpia-opinion-dialog/dpia-opinion-dialog.component';

const routes: Routes = [
  { path: '', component: DpiaListComponent },
  { path: ':id', component: DpiaDetailComponent }
];

@NgModule({
  declarations: [
    DpiaListComponent,
    DpiaDetailComponent,
    DpiaCreateDialogComponent,
    DpiaEditDialogComponent,
    DpiaOpinionDialogComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class DpiaModule {}
