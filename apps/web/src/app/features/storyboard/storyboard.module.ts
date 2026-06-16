import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { StoryboardComponent } from './pages/storyboard/storyboard.component';

const routes: Routes = [
  { path: '', component: StoryboardComponent }
];

@NgModule({
  declarations: [StoryboardComponent],
  imports: [SharedModule, UiModule, ReactiveFormsModule, RouterModule.forChild(routes)]
})
export class StoryboardModule {}
