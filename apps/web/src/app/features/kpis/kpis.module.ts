import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { KpisDetailComponent } from './pages/kpis-detail/kpis-detail.component';
import { KpisDialogComponent } from './pages/kpis-dialog/kpis-dialog.component';
import { KpisListComponent } from './pages/kpis-list/kpis-list.component';
import { KpisMeasurementDialogComponent } from './pages/kpis-measurement-dialog/kpis-measurement-dialog.component';

const routes: Routes = [
  { path: '', component: KpisListComponent },
  { path: ':id', component: KpisDetailComponent }
];

@NgModule({
  declarations: [
    KpisListComponent,
    KpisDetailComponent,
    KpisDialogComponent,
    KpisMeasurementDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class KpisModule {}
