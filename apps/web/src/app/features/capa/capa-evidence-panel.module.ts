import { NgModule } from '@angular/core';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { CapaEvidencePanelComponent } from './components/capa-evidence-panel/capa-evidence-panel.component';

/**
 * Les preuves DU DOSSIER, seules, pour que le D6 de la fiche d'un écart les
 * montre sans tirer derrière lui les écrans et les routes du module CAPA.
 *
 * <p>DETTE CONNUE : la fiche CAPA porte encore sa propre carte « Preuves », qui
 * fait la même chose. Les deux doivent converger sur ce composant ; ce n'est pas
 * fait ici pour ne pas retoucher un écran partagé dans le même lot.
 */
@NgModule({
  declarations: [CapaEvidencePanelComponent],
  imports: [SharedModule, UiModule],
  exports: [CapaEvidencePanelComponent]
})
export class CapaEvidencePanelModule {}
