import { NgModule } from '@angular/core';
import { MatRadioModule } from '@angular/material/radio';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { CapaActionDialogModule } from '../capa/capa-action-dialog.module';
import { UiModule } from '../../shared/ui/ui.module';
import { NcCreateDialogComponent } from './pages/nc-create-dialog/nc-create-dialog.component';
import { NcDetailComponent } from './pages/nc-detail/nc-detail.component';
import { NcEightDComponent } from './pages/nc-eightd/nc-eightd.component';
import { NcListComponent } from './pages/nc-list/nc-list.component';
import { NcRejectDialogComponent } from './pages/nc-reject-dialog/nc-reject-dialog.component';
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
  // Deux segments : `:id` ne capte pas « 8d », mais la route est posée avant
  // pour que l'ordre reste lisible à qui ajoutera la suivante.
  { path: ':id/8d', component: NcEightDComponent },
  { path: ':id', component: NcDetailComponent }
];

@NgModule({
  declarations: [
    NcListComponent,
    NcDetailComponent,
    NcCreateDialogComponent,
    NcRejectDialogComponent,
    NcResolveDialogComponent,
    NcEightDComponent
  ],
  // MatRadioModule n'est pas réexporté par SharedModule : le choix du mode de
  // défaillance a besoin de boutons radio, un pour chaque suggestion et un pour
  // « aucun ne correspond ».
  // `CapaActionDialogModule` : « Ajouter une action » sur une NC ouvre LE MEME
  // formulaire que dans une CAPA. On importe ce module minuscule et non
  // `CapaModule`, qui aurait greffe les routes /capa sous /nc.
  imports: [SharedModule, UiModule, MatRadioModule, CapaActionDialogModule,
            RouterModule.forChild(routes)]
})
export class NcModule {}
