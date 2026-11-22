import { NgModule } from '@angular/core';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { CapaActionDialogModule } from './capa-action-dialog.module';
import { CapaCreateDialogModule } from './capa-create-dialog.module';
import {
  CapaActionVerificationDialogModule
} from './capa-action-verification-dialog.module';
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
  // La fenêtre d'une action, pour la CORRIGER : l'ajout, lui, ouvre une CAPA.
  // `CapaActionVerificationDialogModule` : juger l'efficacite d'une action se
  // fait depuis sa ligne, la ou on vient de la terminer -- et non sur un ecran
  // separe qu'il faudrait penser a visiter (ADR 0132).
  // `CapaCreateDialogModule` : dans une discipline, ajouter une action OUVRE UNE
  // CAPA -- une CAPA porte une action, et une seule. Son propre module, et non
  // `CapaModule` : celui-ci porte ses routes.
  imports: [SharedModule, UiModule, CapaActionDialogModule,
            CapaActionVerificationDialogModule, CapaCreateDialogModule],
  exports: [CapaActionPanelComponent]
})
export class CapaActionPanelModule {}
