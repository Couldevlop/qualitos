import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { TenantModulesComponent } from './pages/tenant-modules/tenant-modules.component';
import { TenantTeamComponent } from './pages/tenant-team/tenant-team.component';

/**
 * Console d'administration du tenant.
 *
 * Premier écran livré : l'activation des modules (§10.4). Les autres surfaces
 * d'administration déjà exposées par l'API (clés d'API, webhooks, quotas, journal
 * d'audit) viendront s'ajouter ici sous le même préfixe `/admin`.
 */
const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'modules' },
  { path: 'modules', component: TenantModulesComponent },
  { path: 'team', component: TenantTeamComponent },
  // Les autres surfaces d'administration sont des modules paresseux distincts,
  // déclarés ICI plutôt qu'à la racine : sans cela, la route `admin` de
  // app-routing.module.ts capterait `/admin/api-keys` par correspondance de préfixe
  // et chercherait `api-keys` dans ce module-ci, qui ne le connaît pas.
  {
    path: 'api-keys',
    loadChildren: () => import('../admin-api-keys/admin-api-keys.module')
      .then(m => m.AdminApiKeysModule)
  },
  {
    path: 'webhooks',
    loadChildren: () => import('../admin-webhooks/admin-webhooks.module')
      .then(m => m.AdminWebhooksModule)
  },
  {
    path: 'quotas',
    loadChildren: () => import('../admin-rate-limits/admin-rate-limits.module')
      .then(m => m.AdminRateLimitsModule)
  },
  {
    path: 'audit-log',
    loadChildren: () => import('../admin-audit-log/audit-log.module')
      .then(m => m.AuditLogModule)
  }
];

@NgModule({
  declarations: [TenantModulesComponent, TenantTeamComponent],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class AdminModule {}
