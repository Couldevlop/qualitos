import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { ImsHubComponent } from './pages/ims-hub/ims-hub.component';
import { MockAuditReportComponent } from './pages/mock-audit-report/mock-audit-report.component';

/**
 * Routes RELATIVES : le chemin de montage est décidé par le routage racine.
 * Le rapport porte l'adoption ET l'exécution parce que l'API les exige toutes
 * deux (`/adoptions/{adoptionId}/audit-blanc-ia/{runId}`) — et parce qu'un
 * rapport doit pouvoir se partager par simple URL.
 */
const routes: Routes = [
  { path: '', component: ImsHubComponent },
  { path: 'audit-blanc-ia/:adoptionId/:runId', component: MockAuditReportComponent }
];

/**
 * Feature lazy-loaded réunissant trois capacités du Standards Hub livrées sans
 * interface : matrice de co-couverture IMS (§8.9), audit blanc IA persisté
 * (§8.4 onglet 7) et ancrage blockchain des preuves (§11.3).
 */
@NgModule({
  declarations: [ImsHubComponent, MockAuditReportComponent],
  imports: [SharedModule, UiModule, ReactiveFormsModule, RouterModule.forChild(routes)]
})
export class StandardsExtrasModule {}
