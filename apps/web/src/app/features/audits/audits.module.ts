import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { AuditsCreateDialogComponent } from './pages/audits-create-dialog/audits-create-dialog.component';
import { AuditsListComponent } from './pages/audits-list/audits-list.component';

const routes: Routes = [{ path: '', component: AuditsListComponent }];

@NgModule({
  declarations: [AuditsListComponent, AuditsCreateDialogComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AuditsModule {}
