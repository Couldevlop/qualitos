import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { LearningHomeComponent } from './pages/learning-home/learning-home.component';

const routes: Routes = [
  { path: '', component: LearningHomeComponent }
];

/**
 * Module lazy « Mon apprentissage » (gamification — CLAUDE.md §19.3).
 * Séparé de TrainingModule (administration des parcours) : un apprenant
 * consulte sa progression sans charger le bundle d'administration.
 */
@NgModule({
  declarations: [LearningHomeComponent],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class LearningModule {}
