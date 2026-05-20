import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { FivesCreateDialogComponent } from './pages/fives-create-dialog/fives-create-dialog.component';
import { FivesDetailComponent } from './pages/fives-detail/fives-detail.component';
import { FivesListComponent } from './pages/fives-list/fives-list.component';

const routes: Routes = [
  { path: '', component: FivesListComponent },
  { path: ':id', component: FivesDetailComponent }
];

@NgModule({
  declarations: [
    FivesListComponent,
    FivesDetailComponent,
    FivesCreateDialogComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class FivesModule {}
