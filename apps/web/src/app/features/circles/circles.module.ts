import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { CirclesListComponent } from './pages/circles-list/circles-list.component';

const routes: Routes = [{ path: '', component: CirclesListComponent }];

@NgModule({
  declarations: [CirclesListComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class CirclesModule {}
