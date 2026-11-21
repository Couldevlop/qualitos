import { NgModule } from '@angular/core';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import {
  CapaActionVerificationDialogComponent
} from './pages/capa-action-verification-dialog/capa-action-verification-dialog.component';

/**
 * La fenêtre qui JUGE l'efficacité d'une action (ADR 0132).
 *
 * <p>Module à part, comme la fenêtre d'une action : le tableau d'actions vit
 * dans la fiche d'un écart et dans le rapport 8D, et {@code CapaModule} porte
 * ses routes via {@code RouterModule.forChild} — l'importer ailleurs grefferait
 * les écrans CAPA à une adresse qui n'est pas la leur.
 *
 * <p>Aucun {@code MatRadioModule} ni sélecteur d'annuaire ici : le verdict se
 * donne par trois boutons à état pressé, et le vérificateur est celui qui agit,
 * pas quelqu'un qu'on désigne.
 */
@NgModule({
  declarations: [CapaActionVerificationDialogComponent],
  imports: [SharedModule, UiModule],
  exports: [CapaActionVerificationDialogComponent]
})
export class CapaActionVerificationDialogModule {}
