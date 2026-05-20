import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { FmeaCreateDialogComponent } from './pages/fmea-create-dialog/fmea-create-dialog.component';
import { FmeaDetailComponent } from './pages/fmea-detail/fmea-detail.component';
import { FmeaEditDialogComponent } from './pages/fmea-edit-dialog/fmea-edit-dialog.component';
import { FmeaItemDialogComponent } from './pages/fmea-item-dialog/fmea-item-dialog.component';
import { FmeaListComponent } from './pages/fmea-list/fmea-list.component';

const routes: Routes = [
  { path: '', component: FmeaListComponent },
  { path: ':id', component: FmeaDetailComponent }
];

@NgModule({
  declarations: [
    FmeaListComponent,
    FmeaDetailComponent,
    FmeaCreateDialogComponent,
    FmeaEditDialogComponent,
    FmeaItemDialogComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class FmeaModule {}
