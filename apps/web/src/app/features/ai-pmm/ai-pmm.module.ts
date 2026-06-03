import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { PmmDetailComponent } from './pages/pmm-detail/pmm-detail.component';
import { PmmDraftDialogComponent } from './pages/pmm-draft-dialog/pmm-draft-dialog.component';
import { PmmEditDialogComponent } from './pages/pmm-edit-dialog/pmm-edit-dialog.component';
import { PmmListComponent } from './pages/pmm-list/pmm-list.component';
import { PmmReasonDialogComponent } from './pages/pmm-reason-dialog/pmm-reason-dialog.component';

const routes: Routes = [
  { path: '', component: PmmListComponent },
  { path: ':id', component: PmmDetailComponent }
];

@NgModule({
  declarations: [
    PmmListComponent,
    PmmDetailComponent,
    PmmDraftDialogComponent,
    PmmEditDialogComponent,
    PmmReasonDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class AiPmmModule {}
