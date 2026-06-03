import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { ItsmConnectionDialogComponent } from './pages/itsm-connection-dialog/itsm-connection-dialog.component';
import { ItsmDetailComponent } from './pages/itsm-detail/itsm-detail.component';
import { ItsmListComponent } from './pages/itsm-list/itsm-list.component';

const routes: Routes = [
  { path: '', component: ItsmListComponent },
  { path: ':id', component: ItsmDetailComponent }
];

@NgModule({
  declarations: [
    ItsmListComponent,
    ItsmDetailComponent,
    ItsmConnectionDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class ItsmModule {}
