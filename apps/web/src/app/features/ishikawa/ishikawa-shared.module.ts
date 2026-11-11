import { NgModule } from '@angular/core';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { IshikawaFishboneComponent } from './components/ishikawa-fishbone/ishikawa-fishbone.component';

/**
 * L'arête de poisson, sans les routes du module Ishikawa.
 *
 * <p>La figure se lit aussi ailleurs que dans son écran : la fiche de la
 * non-conformité montre le diagramme rattaché à l'écart, là où l'analyse de
 * cause se discute. Importer `IshikawaModule` pour cela déclarerait ses routes
 * une seconde fois — `/ishikawa` et `/ishikawa/:id` deviendraient ambigus.
 */
@NgModule({
  declarations: [IshikawaFishboneComponent],
  imports: [SharedModule, UiModule],
  exports: [IshikawaFishboneComponent]
})
export class IshikawaSharedModule {}
