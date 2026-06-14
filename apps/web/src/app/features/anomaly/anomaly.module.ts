import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { AnomalyDetectComponent } from './pages/anomaly-detect/anomaly-detect.component';

const routes: Routes = [
  { path: '', component: AnomalyDetectComponent }
];

@NgModule({
  declarations: [AnomalyDetectComponent],
  imports: [SharedModule, UiModule, ReactiveFormsModule, RouterModule.forChild(routes)]
})
export class AnomalyModule {}
