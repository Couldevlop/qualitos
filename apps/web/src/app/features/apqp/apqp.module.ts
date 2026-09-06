import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { ApqpOverviewComponent } from './pages/apqp-overview/apqp-overview.component';
import { ApqpPhaseComponent } from './pages/apqp-phase/apqp-phase.component';

// Le cycle d'abord, la phase ensuite. Le segment de phase est un MOT
// (`validation`) et non un rang : un lien partagé survit à une renumérotation
// du référentiel, et se lit dans la barre d'adresse.
const routes: Routes = [
  { path: '', component: ApqpOverviewComponent },
  { path: ':phase', component: ApqpPhaseComponent }
];

@NgModule({
  declarations: [ApqpOverviewComponent, ApqpPhaseComponent],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class ApqpModule {}
