import { NgModule } from '@angular/core';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { KpiForecastPanelComponent } from './components/kpi-forecast-panel/kpi-forecast-panel.component';

/**
 * Le panneau de prévision, sans les routes de la page d'essai.
 *
 * <p>La fiche de l'indicateur a besoin du panneau, pas de la route `/forecast` : importer
 * le module de la fonctionnalité y déclarerait une seconde fois ses routes enfants.
 */
@NgModule({
  declarations: [KpiForecastPanelComponent],
  imports: [SharedModule, UiModule],
  exports: [KpiForecastPanelComponent]
})
export class ForecastSharedModule {}
