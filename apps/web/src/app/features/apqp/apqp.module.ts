import { NgModule } from '@angular/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import {
  ApqpDeliverableDetailDialogComponent
} from './pages/apqp-deliverable-detail-dialog/apqp-deliverable-detail-dialog.component';
import {
  ApqpDeliverableDialogComponent
} from './pages/apqp-deliverable-dialog/apqp-deliverable-dialog.component';
import { ApqpOverviewComponent } from './pages/apqp-overview/apqp-overview.component';
import { ApqpPhaseDialogComponent } from './pages/apqp-phase-dialog/apqp-phase-dialog.component';
import {
  ApqpPpapPageComponent
} from './pages/apqp-ppap-page/apqp-ppap-page.component';
import {
  ApqpPpapSummaryComponent
} from './pages/apqp-ppap-summary/apqp-ppap-summary.component';
import {
  ApqpProjectDialogComponent
} from './pages/apqp-project-dialog/apqp-project-dialog.component';
import {
  ApqpProjectListComponent
} from './pages/apqp-project-list/apqp-project-list.component';

// La racine liste les PROJETS : le cycle n'appartient plus au client mais a un
// projet, et un client en mene plusieurs de front.
//
// Sous un projet, le schema EST l'interface et la phase ouverte vit dans l'URL.
// Deux chemins pour un meme composant, donc, et non deux pages -- un lien vers
// une phase reste partageable sans qu'on quitte la vue d'ensemble. Le segment
// est le RANG de la phase : un mot tire du titre aurait casse les liens deja
// partages au premier renommage, et le cycle se renomme desormais.
const routes: Routes = [
  { path: '', component: ApqpProjectListComponent },
  // AVANT `:projetId/:phase`, qui capterait « ppap » comme s'il etait un rang de
  // phase. Le dossier PPAP a son propre ecran parce qu'on le travaille pour
  // lui-meme a l'approche d'une soumission, et pas seulement en marge du cycle.
  { path: ':projetId/ppap', component: ApqpPpapPageComponent },
  { path: ':projetId', component: ApqpOverviewComponent },
  { path: ':projetId/:phase', component: ApqpOverviewComponent }
];

@NgModule({
  declarations: [
    ApqpProjectListComponent,
    ApqpProjectDialogComponent,
    ApqpOverviewComponent,
    ApqpPhaseDialogComponent,
    ApqpDeliverableDialogComponent,
    ApqpDeliverableDetailDialogComponent,
    ApqpPpapPageComponent,
    ApqpPpapSummaryComponent
  ],
  // Le selecteur de date n'est cable QUE sur ce module, pas dans le module
  // partage : il traine un adaptateur de date, et l'imposer a toute
  // l'application pour un champ d'echeance chargerait chaque page qui n'en a pas
  // l'usage. APQP etant charge a la demande, le cout reste ou il sert.
  //
  // `MatNativeDateModule` prend la langue du build via `LOCALE_ID` : le
  // calendrier s'ouvre donc en francais ou en japonais sans reglage de plus.
  imports: [
    SharedModule, UiModule,
    MatDatepickerModule, MatNativeDateModule,
    RouterModule.forChild(routes)
  ]
})
export class ApqpModule {}
