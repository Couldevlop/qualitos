import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { FivesListComponent } from './pages/fives-list/fives-list.component';

const routes: Routes = [{ path: '', component: FivesListComponent }];

@NgModule({
  declarations: [FivesListComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class FivesModule {}
