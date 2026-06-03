import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

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
  { path: 'paths/:id', component: TrainingPathDetailComponent }
];

@NgModule({
  declarations: [
    TrainingHomeComponent,
    TrainingPathDetailComponent,
    TrainingPathDialogComponent,
    TrainingSkillDialogComponent,
    TrainingEnrollDialogComponent,
    TrainingRequirementDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class TrainingModule {}
