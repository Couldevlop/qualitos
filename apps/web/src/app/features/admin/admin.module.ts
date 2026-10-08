import { DragDropModule } from '@angular/cdk/drag-drop';
import { NgModule } from '@angular/core';
import { MatMenuModule } from '@angular/material/menu';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { CredentialRevealComponent } from './components/credential-reveal/credential-reveal.component';
import { CircuitsComponent } from './pages/circuits/circuits.component';
import { ClientsComponent } from './pages/clients/clients.component';
import { LicenseComponent } from './pages/license/license.component';
import { RolesMatrixComponent } from './pages/roles-matrix/roles-matrix.component';
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
  // Rôles et droits par client (ADR 0078) : la matrice, et l'équipe qu'on y glisse.
  { path: 'roles', component: RolesMatrixComponent },
  // Les circuits de validation (ADR 0080) : qui approuve, et dans quel ordre.
  { path: 'circuits', component: CircuitsComponent },
  // La licence d'une installation on-premise (ADR 0082).
  { path: 'licence', component: LicenseComponent },
  // Les clients de la plateforme et leur création (éditeur, ADR 0079).
  { path: 'clients', component: ClientsComponent },
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
  declarations: [TenantModulesComponent, TenantTeamComponent, RolesMatrixComponent, CircuitsComponent, ClientsComponent, LicenseComponent,
    CredentialRevealComponent],
  imports: [SharedModule, UiModule, DragDropModule, MatMenuModule, RouterModule.forChild(routes)]
})
export class AdminModule {}
