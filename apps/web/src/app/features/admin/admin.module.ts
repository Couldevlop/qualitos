import { DragDropModule } from '@angular/cdk/drag-drop';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatMenuModule } from '@angular/material/menu';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { UserPickerModule } from '../../shared/user-picker/user-picker.module';
import { CredentialRevealComponent } from './components/credential-reveal/credential-reveal.component';
import { CircuitsComponent } from './pages/circuits/circuits.component';
import { ClientsComponent } from './pages/clients/clients.component';
import { LicenseComponent } from './pages/license/license.component';
import { MocSettingsComponent } from './pages/moc-settings/moc-settings.component';
import { DocumentSettingsComponent } from './pages/document-settings/document-settings.component';
import { SpcSettingsComponent } from './pages/spc-settings/spc-settings.component';
import { DmaicSettingsComponent } from './pages/dmaic-settings/dmaic-settings.component';
import { CompetencyWorkshopsComponent } from './pages/competency-workshops/competency-workshops.component';
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
  // Les paramètres de la gestion des changements (ADR 0087) : seuils, poids, rappel, validateurs.
  { path: 'moc', component: MocSettingsComponent },
  // La documentation qualité (ADR 0094) : revues, fichiers, arborescence et processus.
  { path: 'documents', component: DocumentSettingsComponent },
  // Les seuils de Cpk des études de capabilité SPC (ADR 0096).
  { path: 'spc', component: SpcSettingsComponent },
  // Le questionnaire, les types de défaillance et les sévérités de la fiche DMAIC (ADR 0101).
  { path: 'dmaic', component: DmaicSettingsComponent },
  // Les ateliers de la matrice de compétences (ADR 0098).
  { path: 'competences', component: CompetencyWorkshopsComponent },
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
    MocSettingsComponent, DocumentSettingsComponent, SpcSettingsComponent, DmaicSettingsComponent, CompetencyWorkshopsComponent,
    CredentialRevealComponent],
  // UserPickerModule : les titulaires des rôles MOC se choisissent dans l'annuaire (ADR 0091).
  imports: [SharedModule, UiModule, FormsModule, DragDropModule, MatMenuModule,
    UserPickerModule, RouterModule.forChild(routes)]
})
export class AdminModule {}
