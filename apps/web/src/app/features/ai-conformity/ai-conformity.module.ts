import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { CnfCertifyDialogComponent } from './pages/cnf-certify-dialog/cnf-certify-dialog.component';
import { CnfDetailComponent } from './pages/cnf-detail/cnf-detail.component';
import { CnfListComponent } from './pages/cnf-list/cnf-list.component';
import { CnfPlanDialogComponent } from './pages/cnf-plan-dialog/cnf-plan-dialog.component';
import { CnfReasonDialogComponent } from './pages/cnf-reason-dialog/cnf-reason-dialog.component';

const routes: Routes = [
  { path: '', component: CnfListComponent },
  { path: ':id', component: CnfDetailComponent }
];

@NgModule({
  declarations: [
    CnfListComponent,
    CnfDetailComponent,
    CnfPlanDialogComponent,
    CnfCertifyDialogComponent,
    CnfReasonDialogComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AiConformityModule {}
