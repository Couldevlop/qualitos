import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { ComplianceHubComponent } from './compliance-hub.component';

const routes: Routes = [{ path: '', component: ComplianceHubComponent }];

/**
 * Module Conformité (GRC) — page hub lazy-loadée sur /compliance (Travail 2).
 * Regroupe IA Act / RGPD / NIS 2 et renvoie vers les routes GRC existantes.
 */
@NgModule({
  declarations: [ComplianceHubComponent],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class ComplianceModule {}
