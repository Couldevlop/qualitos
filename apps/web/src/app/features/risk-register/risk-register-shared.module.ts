import { NgModule } from '@angular/core';

import { SharedModule } from '../../shared/shared.module';
import { CapaCreateDialogModule } from '../capa/capa-create-dialog.module';
import { ActionDialogComponent } from './pages/action-dialog/action-dialog.component';

/**
 * Ce que le registre prête aux autres écrans : la fenêtre d'action
 * d'opportunité et, réexportée, LA fenêtre de création CAPA (ADR 0088), que le
 * tableau de bord SMI ouvre depuis le panneau d'une case de la matrice. Sans
 * les routes du registre — les importer ailleurs les dupliquerait.
 */
@NgModule({
  declarations: [ActionDialogComponent],
  imports: [SharedModule, CapaCreateDialogModule],
  exports: [ActionDialogComponent, CapaCreateDialogModule]
})
export class RiskRegisterSharedModule {}
