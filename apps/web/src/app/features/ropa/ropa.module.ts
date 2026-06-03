import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { RopaDetailComponent } from './pages/ropa-detail/ropa-detail.component';
import { RopaDialogComponent } from './pages/ropa-dialog/ropa-dialog.component';
import { RopaListComponent } from './pages/ropa-list/ropa-list.component';

const routes: Routes = [
  { path: '', component: RopaListComponent },
  { path: ':id', component: RopaDetailComponent }
];

@NgModule({
  declarations: [
    RopaListComponent,
    RopaDetailComponent,
    RopaDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class RopaModule {}
