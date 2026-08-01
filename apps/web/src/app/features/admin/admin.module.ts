import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { TenantModulesComponent } from './pages/tenant-modules/tenant-modules.component';

/**
 * Console d'administration du tenant.
 *
 * Premier écran livré : l'activation des modules (§10.4). Les autres surfaces
 * d'administration déjà exposées par l'API (clés d'API, webhooks, quotas, journal
 * d'audit) viendront s'ajouter ici sous le même préfixe `/admin`.
 */
const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'modules' },
  { path: 'modules', component: TenantModulesComponent }
];

@NgModule({
  declarations: [TenantModulesComponent],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class AdminModule {}
