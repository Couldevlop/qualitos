import { NgModule } from '@angular/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatRadioModule } from '@angular/material/radio';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { CapaActionPanelModule } from '../capa/capa-action-panel.module';
import { CapaActionsDialogModule } from '../capa/capa-actions-dialog.module';
import { CapaCreateDialogModule } from '../capa/capa-create-dialog.module';
import { FiveWhysSharedModule } from '../five-whys/five-whys-shared.module';
import { IshikawaSharedModule } from '../ishikawa/ishikawa-shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { UserPickerModule } from '../../shared/user-picker/user-picker.module';
import {
  NcCauseAnalysisComponent
} from './components/nc-cause-analysis/nc-cause-analysis.component';
import { NcConvertDialogComponent } from './pages/nc-convert-dialog/nc-convert-dialog.component';
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
    // Les figures d'analyse de cause sont les MEMES sur la fiche de l'ecart et
    // dans le D4 du rapport 8D : un composant, deux hotes (ADR 0115).
    NcCauseAnalysisComponent,
    NcConvertDialogComponent,
    NcCreateDialogComponent,
    NcRejectDialogComponent,
    NcResolveDialogComponent,
    NcEightDComponent
  ],
  // MatRadioModule n'est pas réexporté par SharedModule : le choix du mode de
  // défaillance a besoin de boutons radio, un pour chaque suggestion et un pour
  // « aucun ne correspond ».
  // `CapaCreateDialogModule` : « Créer une CAPA » sur une NC ouvre LA fenetre
  // commune (ADR 0088). On importe ce module minuscule et non `CapaModule`, qui
  // aurait greffe les routes /capa sous /nc.
  // `UserPickerModule` et le calendrier : ouvrir le cycle ou le projet qui
  // traitera un ecart demande une personne choisie dans l'annuaire (jamais un
  // texte libre) et, pour un projet DMAIC seul, une echeance visee.
  // Les deux figures d'analyse de cause se lisent SUR la fiche de l'ecart : on
  // importe leurs modules partages, sans routes, et non `IshikawaModule` /
  // `FiveWhysModule`, qui grefferaient `/ishikawa` et `/five-whys` sous `/nc`.
  // `CapaActionsDialogModule` : « Ajouter des actions » ouvre la fenetre qui
  // liste les actions du dossier (ADR 0115) -- le meme tableau que la fiche.
  imports: [SharedModule, UiModule, MatRadioModule, CapaCreateDialogModule, CapaActionPanelModule,
    CapaActionsDialogModule,
            UserPickerModule, FiveWhysSharedModule, IshikawaSharedModule,
            MatDatepickerModule, MatNativeDateModule,
            RouterModule.forChild(routes)]
})
export class NcModule {}
