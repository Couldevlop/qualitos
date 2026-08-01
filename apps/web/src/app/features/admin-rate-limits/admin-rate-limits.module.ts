import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { QuotasComponent } from './pages/quotas/quotas.component';

/**
 * Administration des quotas et limites d'API du tenant (§13.1).
 *
 * Chargé en lazy sous `/admin/quotas` : un tenant qui ne règle jamais ses quotas ne
 * télécharge pas ce bundle.
 */
const routes: Routes = [
  { path: '', component: QuotasComponent }
];

@NgModule({
  declarations: [QuotasComponent],
  imports: [SharedModule, UiModule, ReactiveFormsModule, RouterModule.forChild(routes)]
})
export class AdminRateLimitsModule {}
