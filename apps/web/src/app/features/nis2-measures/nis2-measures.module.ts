import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { Nis2mDetailComponent } from './pages/nis2m-detail/nis2m-detail.component';
import { Nis2mEditDialogComponent } from './pages/nis2m-edit-dialog/nis2m-edit-dialog.component';
import { Nis2mListComponent } from './pages/nis2m-list/nis2m-list.component';
import { Nis2mPlanDialogComponent } from './pages/nis2m-plan-dialog/nis2m-plan-dialog.component';
import { Nis2mReviewDialogComponent } from './pages/nis2m-review-dialog/nis2m-review-dialog.component';

const routes: Routes = [
  { path: '', component: Nis2mListComponent },
  { path: ':id', component: Nis2mDetailComponent }
];

@NgModule({
  declarations: [
    Nis2mListComponent,
    Nis2mDetailComponent,
    Nis2mPlanDialogComponent,
    Nis2mEditDialogComponent,
    Nis2mReviewDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class Nis2MeasuresModule {}
