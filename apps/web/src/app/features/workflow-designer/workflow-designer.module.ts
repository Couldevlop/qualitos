import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { WorkflowEditorComponent } from './pages/workflow-editor/workflow-editor.component';
import { WorkflowListComponent } from './pages/workflow-list/workflow-list.component';

// NB performance (§15.2 / docs perf) : ce module NE référence PAS bpmn-js.
// La lib lourde est chargée par import() dynamique DANS WorkflowEditorComponent,
// elle part donc dans un chunk asynchrone séparé — jamais dans le chunk de cette
// feature ni dans le bundle initial atteint par le shell.
const routes: Routes = [
  { path: '', component: WorkflowListComponent },
  { path: 'new', component: WorkflowEditorComponent },
  { path: ':id', component: WorkflowEditorComponent }
];

@NgModule({
  declarations: [WorkflowListComponent, WorkflowEditorComponent],
  imports: [SharedModule, UiModule, FormsModule, RouterModule.forChild(routes)]
})
export class WorkflowDesignerModule {}
