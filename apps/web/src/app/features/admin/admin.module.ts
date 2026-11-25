import { DragDropModule } from '@angular/cdk/drag-drop';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatMenuModule } from '@angular/material/menu';
import { MatTabsModule } from '@angular/material/tabs';
import { RouterModule, Routes } from '@angular/router';

import { roleGuard } from '../../core/auth/role.guard';
import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { UserPickerModule } from '../../shared/user-picker/user-picker.module';
import { PersonNameModule } from '../../shared/person-name/person-name.module';
import { CredentialRevealModule } from '../../shared/credential-reveal/credential-reveal.module';
import { AdminShellComponent } from './pages/admin-shell/admin-shell.component';
import { CatalogsComponent } from './pages/catalogs/catalogs.component';
import { CircuitsComponent } from './pages/circuits/circuits.component';
import { LicenseComponent } from './pages/license/license.component';
import { MocSettingsComponent } from './pages/moc-settings/moc-settings.component';
import { DocumentSettingsComponent } from './pages/document-settings/document-settings.component';
import { MailSettingsModule } from './mail/mail-settings.module';
import { MailSettingsComponent } from './pages/mail-settings/mail-settings.component';
import { SpcSettingsComponent } from './pages/spc-settings/spc-settings.component';
import { AiPolicyComponent } from './pages/ai-policy/ai-policy.component';
import { DmaicSettingsComponent } from './pages/dmaic-settings/dmaic-settings.component';
import { NcSettingsComponent } from './pages/nc-settings/nc-settings.component';
import { PartnerAccessComponent } from './pages/partner-access/partner-access.component';
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
/**
 * Les ecrans d'administration, tous enfants de l'enveloppe a deux onglets.
 *
 * <p>Chacun garde SON adresse : les onglets sont des liens, pas des panneaux.
 * Un signet sur `/admin/messagerie` continue d'ouvrir la messagerie, et la
 * barre d'onglets s'allume sur « Parametrage » grace au `data.onglet` que porte
 * la route.</p>
 */
const ecrans: Routes = [
  // La console ouvre sur l'equipe, premiere entree du premier onglet. Elle
  // ouvrait sur les modules : un reglage de facturation n'est pas ce qu'un
  // administrateur vient faire le plus souvent, et c'est maintenant le second
  // onglet -- y atterrir aurait donne a croire que l'onglet des utilisateurs
  // etait secondaire.
  { path: '', pathMatch: 'full', redirectTo: 'team' },
  { path: 'modules', component: TenantModulesComponent, data: { onglet: 'parametrage' } },
  { path: 'team', component: TenantTeamComponent, data: { onglet: 'utilisateurs' } },
  // Rôles et droits par client (ADR 0078) : la matrice, et l'équipe qu'on y glisse.
  { path: 'roles', component: RolesMatrixComponent, data: { onglet: 'utilisateurs' } },
  // Les circuits de validation (ADR 0080) : qui approuve, et dans quel ordre.
  { path: 'circuits', component: CircuitsComponent },
  // Les paramètres de la gestion des changements (ADR 0087) : seuils, poids, rappel, validateurs.
  { path: 'moc', component: MocSettingsComponent, data: { onglet: 'parametrage' } },
  // La documentation qualité (ADR 0094) : revues, fichiers, arborescence et processus.
  { path: 'documents', component: DocumentSettingsComponent, data: { onglet: 'parametrage' } },
  // Les seuils de Cpk des études de capabilité SPC (ADR 0096).
  { path: 'spc', component: SpcSettingsComponent, data: { onglet: 'parametrage' } },
  // Le questionnaire, les types de défaillance et les sévérités de la fiche DMAIC (ADR 0101).
  { path: 'dmaic', component: DmaicSettingsComponent, data: { onglet: 'parametrage' } },
  // Les natures d'écart, sous chaque catégorie de non-conformité (ADR 0103).
  { path: 'nc', component: NcSettingsComponent, data: { onglet: 'parametrage' } },
  // Les listes déroulantes de l'application, dans les mots du client (ADR 0111).
  { path: 'listes', component: CatalogsComponent, data: { onglet: 'parametrage' } },
  // Le fournisseur des appels LLM du tenant, et sa souveraineté (ADR 0102).
  // La messagerie de l'organisation (ADR 0127) : l'adresse d'ou partent ses
  // courriels. Sans reglage, ils partent de celle de la plateforme.
  { path: 'messagerie', component: MailSettingsComponent, data: { onglet: 'parametrage' } },
  { path: 'ia', component: AiPolicyComponent, data: { onglet: 'parametrage' } },
  // Les cabinets partenaires et leurs accès (ADR 0106). L'écran est ICI, chez le
  // client, et non dans la console de l'éditeur : l'éditeur propose un cabinet,
  // seul l'administrateur du client lui ouvre — et le reprend.
  { path: 'cabinets', component: PartnerAccessComponent, data: { onglet: 'utilisateurs' } },
  // Les ateliers de la matrice de compétences (ADR 0098).
  { path: 'competences', component: CompetencyWorkshopsComponent, data: { onglet: 'parametrage' } },
  // La licence d'une installation on-premise (ADR 0082).
  { path: 'licence', component: LicenseComponent, data: { onglet: 'parametrage' } },
  // Les clients de la plateforme et leur création (éditeur, ADR 0079).
  // Les autres surfaces d'administration sont des modules paresseux distincts,
  // déclarés ICI plutôt qu'à la racine : sans cela, la route `admin` de
  // app-routing.module.ts capterait `/admin/api-keys` par correspondance de préfixe
  // et chercherait `api-keys` dans ce module-ci, qui ne le connaît pas.
  {
    path: 'api-keys',
    data: { onglet: 'parametrage' },
    loadChildren: () => import('../admin-api-keys/admin-api-keys.module')
      .then(m => m.AdminApiKeysModule)
  },
  {
    path: 'webhooks',
    data: { onglet: 'parametrage' },
    loadChildren: () => import('../admin-webhooks/admin-webhooks.module')
      .then(m => m.AdminWebhooksModule)
  },
  {
    path: 'quotas',
    data: { onglet: 'parametrage' },
    loadChildren: () => import('../admin-rate-limits/admin-rate-limits.module')
      .then(m => m.AdminRateLimitsModule)
  },
  {
    path: 'audit-log',
    data: { onglet: 'parametrage' },
    loadChildren: () => import('../admin-audit-log/audit-log.module')
      .then(m => m.AuditLogModule)
  }
];

const routes: Routes = [
  { path: '', component: AdminShellComponent, children: ecrans }
];

@NgModule({
  declarations: [AdminShellComponent, TenantModulesComponent, TenantTeamComponent, RolesMatrixComponent, CircuitsComponent, LicenseComponent,
    MocSettingsComponent, DocumentSettingsComponent, SpcSettingsComponent, DmaicSettingsComponent, CompetencyWorkshopsComponent,
    AiPolicyComponent, NcSettingsComponent, PartnerAccessComponent, CatalogsComponent],
  // UserPickerModule : les titulaires des rôles MOC se choisissent dans l'annuaire (ADR 0091).
  imports: [SharedModule, UiModule, FormsModule, DragDropModule, MatMenuModule, MatTabsModule, MailSettingsModule,
    UserPickerModule, PersonNameModule, CredentialRevealModule, RouterModule.forChild(routes)]
})
export class AdminModule {}
