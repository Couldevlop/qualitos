import { NgModule } from '@angular/core';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { FiveWhysCascadeComponent } from './components/five-whys-cascade/five-whys-cascade.component';

/**
 * La cascade des 5 Pourquoi, sans les routes du module.
 *
 * <p>Même raison que pour l'arête de poisson : la fiche de la non-conformité
 * montre la chaîne déroulée sur l'écart, et importer `FiveWhysModule` y
 * déclarerait `/five-whys` une seconde fois.
 */
@NgModule({
  declarations: [FiveWhysCascadeComponent],
  imports: [SharedModule, UiModule],
  exports: [FiveWhysCascadeComponent]
})
export class FiveWhysSharedModule {}
