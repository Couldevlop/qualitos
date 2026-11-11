import { NgModule } from '@angular/core';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { CapaCreateDialogModule } from './capa-create-dialog.module';
import { CapaActionPanelComponent } from './components/capa-action-panel/capa-action-panel.component';

/**
 * Le panneau d'action CAPA, à importer par les écrans qui montrent où en est
 * le traitement sans être la fiche CAPA — aujourd'hui la fiche d'un écart.
 *
 * <p>Module à part et non {@code CapaModule} : celui-ci porte ses routes via
 * {@code RouterModule.forChild}, et l'importer ailleurs grefferait les écrans
 * CAPA à une adresse qui n'est pas la leur.
 */
@NgModule({
  declarations: [CapaActionPanelComponent],
  // La fenêtre de création sert aussi à définir l'action d'un dossier qui n'en
  // a pas (ADR 0088) : c'est la même, jamais une jumelle.
  imports: [SharedModule, UiModule, CapaCreateDialogModule],
  exports: [CapaActionPanelComponent]
})
export class CapaActionPanelModule {}
