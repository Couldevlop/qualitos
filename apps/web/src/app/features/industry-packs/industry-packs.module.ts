import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { PacksDetailComponent } from './pages/packs-detail/packs-detail.component';
import { PacksListComponent } from './pages/packs-list/packs-list.component';

const routes: Routes = [
  { path: '', component: PacksListComponent },
  { path: ':code', component: PacksDetailComponent }
];

@NgModule({
  declarations: [PacksListComponent, PacksDetailComponent],
  imports: [SharedModule, FormsModule, RouterModule.forChild(routes)]
})
export class IndustryPacksModule {}
