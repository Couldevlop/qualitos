import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { StandardsListComponent } from './pages/standards-list/standards-list.component';

const routes: Routes = [{ path: '', component: StandardsListComponent }];

@NgModule({
  declarations: [StandardsListComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class StandardsModule {}
