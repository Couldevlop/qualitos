import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { UserPickerModule } from '../../shared/user-picker/user-picker.module';
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
  // Le calendrier porte l'échéance des étapes de la roadmap ; `MatNativeDateModule`
  // prend la langue du build via `LOCALE_ID`. Le sélecteur d'annuaire y désigne
  // le responsable : une personne, jamais un texte libre.
  imports: [
    SharedModule, UiModule, FormsModule, UserPickerModule,
    MatDatepickerModule, MatNativeDateModule, RouterModule.forChild(routes)
  ]
})
export class StandardsModule {}
