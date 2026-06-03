import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { AiQmsApproveDialogComponent } from './pages/ai-qms-approve-dialog/ai-qms-approve-dialog.component';
import { AiQmsDetailComponent } from './pages/ai-qms-detail/ai-qms-detail.component';
import { AiQmsDialogComponent } from './pages/ai-qms-dialog/ai-qms-dialog.component';
import { AiQmsListComponent } from './pages/ai-qms-list/ai-qms-list.component';

const routes: Routes = [
  { path: '', component: AiQmsListComponent },
  { path: ':id', component: AiQmsDetailComponent }
];

@NgModule({
  declarations: [
    AiQmsListComponent,
    AiQmsDetailComponent,
    AiQmsDialogComponent,
    AiQmsApproveDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class AiQmsModule {}
