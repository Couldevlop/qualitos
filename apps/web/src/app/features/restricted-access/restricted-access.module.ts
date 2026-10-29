import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { RestrictedAccessComponent } from './pages/restricted-access/restricted-access.component';

const routes: Routes = [
  { path: '', component: RestrictedAccessComponent }
];

@NgModule({
  declarations: [RestrictedAccessComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class RestrictedAccessModule {}
