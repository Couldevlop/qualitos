import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { RouterModule, Routes } from '@angular/router';

import {
  CompetencyMatrixComponent
} from './pages/competency-matrix/competency-matrix.component';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { TrainingEnrollDialogComponent } from './pages/training-enroll-dialog/training-enroll-dialog.component';
import { TrainingHomeComponent } from './pages/training-home/training-home.component';
import { TrainingPathDetailComponent } from './pages/training-path-detail/training-path-detail.component';
import { TrainingPathDialogComponent } from './pages/training-path-dialog/training-path-dialog.component';
import { TrainingRequirementDialogComponent } from './pages/training-requirement-dialog/training-requirement-dialog.component';
import { TrainingSkillDialogComponent } from './pages/training-skill-dialog/training-skill-dialog.component';

const routes: Routes = [
  { path: '', component: TrainingHomeComponent },
  { path: 'competences', component: CompetencyMatrixComponent },
  { path: 'paths/:id', component: TrainingPathDetailComponent }
];

@NgModule({
  declarations: [
    CompetencyMatrixComponent,
    TrainingHomeComponent,
    TrainingPathDetailComponent,
    TrainingPathDialogComponent,
    TrainingSkillDialogComponent,
    TrainingEnrollDialogComponent,
    TrainingRequirementDialogComponent
  ],
  // FormsModule et MatSlideToggleModule ne sont re-exportes ni par Shared ni
  // par Ui : les specs les importent d'elles-memes, seul le build de
  // production voit l'oubli.
  imports: [SharedModule, UiModule, FormsModule, MatSlideToggleModule,
    RouterModule.forChild(routes)]
})
export class TrainingModule {}
