import { NgModule } from '@angular/core';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { CapaActionsDialogComponent } from './pages/capa-actions-dialog/capa-actions-dialog.component';

/**
 * La fenêtre qui liste ce qui reste OUVERT sur un écart, ouverte depuis un écran
 * qui n'est pas la fiche CAPA — aujourd'hui la fiche d'un écart.
 *
 * <p>Elle portait le tableau complet ({@code qos-capa-action-panel}) ; la fiche
 * de l'écart le porte désormais elle-même, et deux fois le même tableau sur le
 * même écran — dont un dans une fenêtre qui recouvre l'autre — faisait ajouter
 * une action par-dessus le tableau qui l'aurait montrée. Elle n'a donc plus
 * besoin du module du tableau.
 *
 * <p>Module à part et non {@code CapaModule} : celui-ci porte ses routes via
 * {@code RouterModule.forChild}, et l'importer ailleurs grefferait les écrans
 * CAPA à une adresse qui n'est pas la leur.
 */
@NgModule({
  declarations: [CapaActionsDialogComponent],
  imports: [SharedModule, UiModule],
  exports: [CapaActionsDialogComponent]
})
export class CapaActionsDialogModule {}
