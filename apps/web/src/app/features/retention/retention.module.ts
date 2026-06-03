import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { RetDetailComponent } from './pages/ret-detail/ret-detail.component';
import { RetEvaluateDialogComponent } from './pages/ret-evaluate-dialog/ret-evaluate-dialog.component';
import { RetListComponent } from './pages/ret-list/ret-list.component';
import { RetRuleDialogComponent } from './pages/ret-rule-dialog/ret-rule-dialog.component';

const routes: Routes = [
  { path: '', component: RetListComponent },
  { path: ':id', component: RetDetailComponent }
];

@NgModule({
  declarations: [
    RetListComponent,
    RetDetailComponent,
    RetRuleDialogComponent,
    RetEvaluateDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class RetentionModule {}
