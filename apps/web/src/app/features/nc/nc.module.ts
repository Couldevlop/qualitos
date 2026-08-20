import { NgModule } from '@angular/core';
import { MatRadioModule } from '@angular/material/radio';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { NcCreateDialogComponent } from './pages/nc-create-dialog/nc-create-dialog.component';
import { NcDetailComponent } from './pages/nc-detail/nc-detail.component';
import { NcListComponent } from './pages/nc-list/nc-list.component';
import { NcResolveDialogComponent } from './pages/nc-resolve-dialog/nc-resolve-dialog.component';

// Deux entrées de navigation, un seul écran : l'origine est portée par la ROUTE
// (§4.3). Un filtre d'écran ferait converger les deux entrées au premier clic sur
// un menu déroulant. L'entrée historique `/nc` reste, et montre les deux origines.
//
// Les chemins nommés viennent AVANT `:id`, sans quoi « interne » serait pris pour
// l'identifiant d'une non-conformité.
const routes: Routes = [
  { path: '', component: NcListComponent },
  { path: 'interne', component: NcListComponent, data: { origin: 'INTERNAL' } },
  { path: 'externe', component: NcListComponent, data: { origin: 'EXTERNAL' } },
  { path: ':id', component: NcDetailComponent }
];

@NgModule({
  declarations: [
    NcListComponent,
    NcDetailComponent,
    NcCreateDialogComponent,
    NcResolveDialogComponent
  ],
  // MatRadioModule n'est pas réexporté par SharedModule : le choix du mode de
  // défaillance a besoin de boutons radio, un pour chaque suggestion et un pour
  // « aucun ne correspond ».
  imports: [SharedModule, UiModule, MatRadioModule, RouterModule.forChild(routes)]
})
export class NcModule {}
