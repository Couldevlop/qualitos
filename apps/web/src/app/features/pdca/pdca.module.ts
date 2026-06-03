import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { PdcaCreateDialogComponent } from './pages/pdca-create-dialog/pdca-create-dialog.component';
import { PdcaDetailComponent } from './pages/pdca-detail/pdca-detail.component';
import { PdcaListComponent } from './pages/pdca-list/pdca-list.component';
import { PdcaStepDialogComponent } from './pages/pdca-step-dialog/pdca-step-dialog.component';

const routes: Routes = [
  { path: '', component: PdcaListComponent },
  { path: ':id', component: PdcaDetailComponent }
];

@NgModule({
  declarations: [
    PdcaListComponent,
    PdcaDetailComponent,
    PdcaCreateDialogComponent,
    PdcaStepDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class PdcaModule {}
