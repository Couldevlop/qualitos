import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { BreachContainDialogComponent } from './pages/breach-contain-dialog/breach-contain-dialog.component';
import { BreachDetailComponent } from './pages/breach-detail/breach-detail.component';
import { BreachDetectDialogComponent } from './pages/breach-detect-dialog/breach-detect-dialog.component';
import { BreachDpaDialogComponent } from './pages/breach-dpa-dialog/breach-dpa-dialog.component';
import { BreachListComponent } from './pages/breach-list/breach-list.component';
import { BreachSeverityDialogComponent } from './pages/breach-severity-dialog/breach-severity-dialog.component';
import { BreachSubjectsDialogComponent } from './pages/breach-subjects-dialog/breach-subjects-dialog.component';
import { BreachTextDialogComponent } from './pages/breach-text-dialog/breach-text-dialog.component';

const routes: Routes = [
  { path: '', component: BreachListComponent },
  { path: ':id', component: BreachDetailComponent }
];

@NgModule({
  declarations: [
    BreachListComponent,
    BreachDetailComponent,
    BreachDetectDialogComponent,
    BreachContainDialogComponent,
    BreachDpaDialogComponent,
    BreachSubjectsDialogComponent,
    BreachTextDialogComponent,
    BreachSeverityDialogComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class BreachModule {}
