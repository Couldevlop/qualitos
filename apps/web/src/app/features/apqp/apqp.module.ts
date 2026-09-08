import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { ApqpOverviewComponent } from './pages/apqp-overview/apqp-overview.component';

// Un seul ecran : le schema EST l'interface, et la phase ouverte vit dans
// l'URL. Deux chemins pour un meme composant, donc, et non deux pages -- un
// lien vers une phase reste partageable sans qu'on quitte la vue d'ensemble.
//
// Le segment est un MOT (`validation`) et non un rang : renumeroter le
// referentiel ne casse pas les liens deja partages.
const routes: Routes = [
  { path: '', component: ApqpOverviewComponent },
  { path: ':phase', component: ApqpOverviewComponent }
];

@NgModule({
  declarations: [ApqpOverviewComponent],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class ApqpModule {}
