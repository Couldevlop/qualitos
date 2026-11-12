import { NgModule } from '@angular/core';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { CapaActionPanelModule } from './capa-action-panel.module';
import { CapaActionsDialogComponent } from './pages/capa-actions-dialog/capa-actions-dialog.component';

/**
 * La fenêtre qui liste les actions d'un dossier, ouverte depuis un écran qui
 * n'est pas la fiche CAPA — aujourd'hui la fiche d'un écart.
 *
 * <p>Elle n'embarque aucun tableau à elle : elle réutilise
 * {@code qos-capa-action-panel}, qui tient déjà les droits, l'avancement et la
 * fenêtre d'une action.
 *
 * <p>Module à part et non {@code CapaModule} : celui-ci porte ses routes via
 * {@code RouterModule.forChild}, et l'importer ailleurs grefferait les écrans
 * CAPA à une adresse qui n'est pas la leur.
 */
@NgModule({
  declarations: [CapaActionsDialogComponent],
  imports: [SharedModule, UiModule, CapaActionPanelModule],
  exports: [CapaActionsDialogComponent]
})
export class CapaActionsDialogModule {}
