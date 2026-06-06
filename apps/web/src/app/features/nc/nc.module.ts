import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { NcCreateDialogComponent } from './pages/nc-create-dialog/nc-create-dialog.component';
import { NcDetailComponent } from './pages/nc-detail/nc-detail.component';
import { NcListComponent } from './pages/nc-list/nc-list.component';
import { NcResolveDialogComponent } from './pages/nc-resolve-dialog/nc-resolve-dialog.component';

const routes: Routes = [
  { path: '', component: NcListComponent },
  { path: ':id', component: NcDetailComponent }
];

@NgModule({
  declarations: [
    NcListComponent,
    NcDetailComponent,
    NcCreateDialogComponent,
    NcResolveDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class NcModule {}
