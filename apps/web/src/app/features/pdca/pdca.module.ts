import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { PdcaCreateDialogComponent } from './pages/pdca-create-dialog/pdca-create-dialog.component';
import { PdcaListComponent } from './pages/pdca-list/pdca-list.component';

const routes: Routes = [
  { path: '', component: PdcaListComponent }
];

@NgModule({
  declarations: [PdcaListComponent, PdcaCreateDialogComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class PdcaModule {}
