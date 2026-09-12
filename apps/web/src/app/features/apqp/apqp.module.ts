import { NgModule } from '@angular/core';
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
  ApqpPpapSummaryComponent
} from './pages/apqp-ppap-summary/apqp-ppap-summary.component';

// Un seul ecran : le schema EST l'interface, et la phase ouverte vit dans
// l'URL. Deux chemins pour un meme composant, donc, et non deux pages -- un
// lien vers une phase reste partageable sans qu'on quitte la vue d'ensemble.
//
// Le segment est le RANG de la phase. Un mot tire du titre aurait casse les
// liens deja partages au premier renommage, et le cycle se renomme desormais.
const routes: Routes = [
  { path: '', component: ApqpOverviewComponent },
  { path: ':phase', component: ApqpOverviewComponent }
];

@NgModule({
  declarations: [
    ApqpOverviewComponent,
    ApqpPhaseDialogComponent,
    ApqpDeliverableDialogComponent,
    ApqpDeliverableDetailDialogComponent,
    ApqpPpapSummaryComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class ApqpModule {}
