import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { CapaListComponent } from './pages/capa-list/capa-list.component';

const routes: Routes = [{ path: '', component: CapaListComponent }];

@NgModule({
  declarations: [CapaListComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class CapaModule {}
