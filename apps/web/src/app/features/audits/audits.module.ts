import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { AuditsChecklistDialogComponent } from './pages/audits-checklist-dialog/audits-checklist-dialog.component';
import { AuditsCreateDialogComponent } from './pages/audits-create-dialog/audits-create-dialog.component';
import { AuditsDetailComponent } from './pages/audits-detail/audits-detail.component';
import { AuditsEditDialogComponent } from './pages/audits-edit-dialog/audits-edit-dialog.component';
import { AuditsFindingDialogComponent } from './pages/audits-finding-dialog/audits-finding-dialog.component';
import { ChecklistFromStandardDialogComponent } from './pages/checklist-from-standard-dialog/checklist-from-standard-dialog.component';
import { AuditsListComponent } from './pages/audits-list/audits-list.component';
import { AuditsPlanningComponent } from './pages/audits-planning/audits-planning.component';
import { AuditsResponseDialogComponent } from './pages/audits-response-dialog/audits-response-dialog.component';

const routes: Routes = [
  { path: '', component: AuditsListComponent },
  // AVANT ':id', impérativement : une route paramétrée placée plus haut avalerait
  // « planning » comme identifiant d'audit et le détail afficherait un 404.
  { path: 'planning', component: AuditsPlanningComponent },
  { path: ':id', component: AuditsDetailComponent }
];

@NgModule({
  declarations: [
    AuditsListComponent,
    AuditsPlanningComponent,
    AuditsDetailComponent,
    AuditsCreateDialogComponent,
    AuditsChecklistDialogComponent,
    AuditsResponseDialogComponent,
    AuditsEditDialogComponent,
    AuditsFindingDialogComponent,
    ChecklistFromStandardDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class AuditsModule {}
