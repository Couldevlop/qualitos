import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { NlqAskComponent } from './pages/nlq-ask/nlq-ask.component';

const routes: Routes = [
  { path: '', component: NlqAskComponent }
];

@NgModule({
  declarations: [NlqAskComponent],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class NlqModule {}
