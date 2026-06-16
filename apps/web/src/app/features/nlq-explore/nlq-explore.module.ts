import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { NlqExploreComponent } from './pages/nlq-explore/nlq-explore.component';

const routes: Routes = [
  { path: '', component: NlqExploreComponent }
];

@NgModule({
  declarations: [NlqExploreComponent],
  imports: [SharedModule, UiModule, ReactiveFormsModule, RouterModule.forChild(routes)]
})
export class NlqExploreModule {}
