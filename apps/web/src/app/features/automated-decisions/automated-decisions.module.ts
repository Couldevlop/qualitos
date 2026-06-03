import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { AdmCreateDialogComponent } from './pages/adm-create-dialog/adm-create-dialog.component';
import { AdmDetailComponent } from './pages/adm-detail/adm-detail.component';
import { AdmEditDialogComponent } from './pages/adm-edit-dialog/adm-edit-dialog.component';
import { AdmListComponent } from './pages/adm-list/adm-list.component';

const routes: Routes = [
  { path: '', component: AdmListComponent },
  { path: ':id', component: AdmDetailComponent }
];

@NgModule({
  declarations: [
    AdmListComponent,
    AdmDetailComponent,
    AdmCreateDialogComponent,
    AdmEditDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class AutomatedDecisionsModule {}
