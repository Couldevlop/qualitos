import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { AuditLogComponent } from './pages/audit-log/audit-log.component';

/**
 * Journal d'audit (§11.5) — consultation et vérification d'intégrité.
 *
 * Module lazy dédié plutôt qu'un écran de plus dans `AdminModule` : le journal est une
 * surface de lecture volumineuse (tableau paginé + vérification de chaîne) que les
 * tenants sans besoin d'audit ne doivent pas télécharger.
 */
const routes: Routes = [
  { path: '', component: AuditLogComponent }
];

@NgModule({
  declarations: [AuditLogComponent],
  imports: [SharedModule, UiModule, RouterModule.forChild(routes)]
})
export class AuditLogModule {}
