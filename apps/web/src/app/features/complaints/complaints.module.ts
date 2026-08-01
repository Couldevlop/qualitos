import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { ComplaintActionDialogComponent } from './pages/complaint-action-dialog/complaint-action-dialog.component';
import { ComplaintCreateDialogComponent } from './pages/complaint-create-dialog/complaint-create-dialog.component';
import { ComplaintDetailComponent } from './pages/complaint-detail/complaint-detail.component';
import { ComplaintEditDialogComponent } from './pages/complaint-edit-dialog/complaint-edit-dialog.component';
import { ComplaintListComponent } from './pages/complaint-list/complaint-list.component';
import { ComplaintResponseDialogComponent } from './pages/complaint-response-dialog/complaint-response-dialog.component';

const routes: Routes = [
  { path: '', component: ComplaintListComponent },
  { path: ':id', component: ComplaintDetailComponent }
];

/**
 * Réclamations clients / Voice of Customer (§4.9).
 *
 * Écran CRUD et suivi du cycle de vie ; l'analyse NLP des réclamations reste sur
 * son propre module (/complaints-nlp) et n'est pas dupliquée ici.
 */
@NgModule({
  declarations: [
    ComplaintListComponent,
    ComplaintDetailComponent,
    ComplaintCreateDialogComponent,
    ComplaintEditDialogComponent,
    ComplaintResponseDialogComponent,
    ComplaintActionDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class ComplaintsModule {}
