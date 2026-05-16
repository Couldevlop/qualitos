import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { AuditsListComponent } from './pages/audits-list/audits-list.component';

const routes: Routes = [{ path: '', component: AuditsListComponent }];

@NgModule({
  declarations: [AuditsListComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AuditsModule {}
