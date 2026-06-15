import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { NcClusterComponent } from './pages/nc-cluster/nc-cluster.component';

const routes: Routes = [
  { path: '', component: NcClusterComponent }
];

@NgModule({
  declarations: [NcClusterComponent],
  imports: [SharedModule, UiModule, ReactiveFormsModule, RouterModule.forChild(routes)]
})
export class NcClusterModule {}
