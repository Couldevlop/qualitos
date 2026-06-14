import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { ForecastKpiComponent } from './pages/forecast-kpi/forecast-kpi.component';

const routes: Routes = [
  { path: '', component: ForecastKpiComponent }
];

@NgModule({
  declarations: [ForecastKpiComponent],
  imports: [SharedModule, UiModule, ReactiveFormsModule, RouterModule.forChild(routes)]
})
export class ForecastModule {}
