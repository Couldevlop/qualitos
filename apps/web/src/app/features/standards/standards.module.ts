import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { ProcedureCreateDialogComponent } from './pages/procedure-create-dialog/procedure-create-dialog.component';
import { StandardsListComponent } from './pages/standards-list/standards-list.component';
import { StandardsDetailComponent } from './pages/standards-detail/standards-detail.component';
import { TreeNodeDialogComponent } from './pages/tree-node-dialog/tree-node-dialog.component';

const routes: Routes = [
  { path: '', component: StandardsListComponent },
  { path: 'adoptions/:id', component: StandardsDetailComponent }
];

@NgModule({
  declarations: [
    StandardsListComponent, StandardsDetailComponent,
    ProcedureCreateDialogComponent, TreeNodeDialogComponent
  ],
  // UiModule fournit qos-form-dialog, dont vivent les deux boîtes de saisie.
  imports: [SharedModule, UiModule, FormsModule, RouterModule.forChild(routes)]
})
export class StandardsModule {}
