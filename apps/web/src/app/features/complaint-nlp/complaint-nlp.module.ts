import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { ComplaintNlpComponent } from './pages/complaint-nlp/complaint-nlp.component';

const routes: Routes = [
  { path: '', component: ComplaintNlpComponent }
];

@NgModule({
  declarations: [ComplaintNlpComponent],
  imports: [SharedModule, UiModule, ReactiveFormsModule, RouterModule.forChild(routes)]
})
export class ComplaintNlpModule {}
