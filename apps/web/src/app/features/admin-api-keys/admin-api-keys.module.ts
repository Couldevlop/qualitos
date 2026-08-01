import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { ApiKeyCreateDialogComponent } from './pages/api-key-create-dialog/api-key-create-dialog.component';
import { ApiKeyRevealDialogComponent } from './pages/api-key-reveal-dialog/api-key-reveal-dialog.component';
import { ApiKeysListComponent } from './pages/api-keys-list/api-keys-list.component';

/**
 * Administration des clés d'API du tenant (§13.1).
 *
 * Module à part de `AdminModule` : les routes de `/api/v1/api-keys` sont réservées aux
 * rôles ADMIN / ADMIN_TENANT / SUPER_ADMIN (SecurityConfig), et le chargement paresseux
 * évite d'embarquer cet écran dans le bundle des utilisateurs qui n'y ont pas droit.
 */
const routes: Routes = [
  { path: '', component: ApiKeysListComponent }
];

@NgModule({
  declarations: [
    ApiKeysListComponent,
    ApiKeyCreateDialogComponent,
    ApiKeyRevealDialogComponent
  ],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class AdminApiKeysModule {}
