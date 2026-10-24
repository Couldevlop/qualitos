import { NgModule } from '@angular/core';
import { MatRadioModule } from '@angular/material/radio';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { UserPickerModule } from '../../shared/user-picker/user-picker.module';
import {
  CapaVerificationFieldsComponent
} from './components/capa-verification-fields/capa-verification-fields.component';

/**
 * Le bloc « vérification d'efficacité » (ADR 0073), partagé par la fenêtre
 * d'ouverture d'une CAPA (ADR 0088) et par sa fenêtre d'édition : un seul
 * formulaire, pas deux qui divergent.
 *
 * <p>MatRadioModule n'est pas réexporté par SharedModule : « exigée : oui /
 * non » est une question fermée, où un groupe de boutons radio dit mieux
 * qu'une case à cocher qu'AUCUNE réponse n'est donnée tant qu'on n'a pas
 * tranché. UserPickerModule : le vérificateur se choisit dans l'annuaire
 * (ADR 0091).
 */
@NgModule({
  declarations: [CapaVerificationFieldsComponent],
  imports: [SharedModule, UiModule, UserPickerModule, MatRadioModule],
  exports: [CapaVerificationFieldsComponent]
})
export class CapaVerificationFieldsModule {}
