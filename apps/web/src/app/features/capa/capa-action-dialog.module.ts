import { NgModule } from '@angular/core';
import { MatRadioModule } from '@angular/material/radio';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { UserPickerModule } from '../../shared/user-picker/user-picker.module';
import { CapaActionDialogComponent } from './pages/capa-action-dialog/capa-action-dialog.component';

/**
 * LA fenêtre d'une action CAPA, seule, pour que chaque écran l'ouvre : le
 * tableau d'actions d'un dossier, la fiche CAPA, la fiche d'un écart. Une seule
 * fenêtre pour l'ajout et pour la correction — deux jumelles divergeraient au
 * premier champ ajouté d'un seul côté.
 *
 * <p>Module à part et non {@code CapaModule} : celui-ci porte ses routes via
 * {@code RouterModule.forChild}, et l'importer ailleurs grefferait les écrans
 * CAPA à une adresse qui n'est pas la leur.
 */
@NgModule({
  declarations: [CapaActionDialogComponent],
  // MatRadioModule n'est pas réexporté par SharedModule : la nature de l'action
  // est une question fermée à trois réponses (endiguement, corrective,
  // préventive). UserPickerModule : le responsable se choisit dans l'annuaire
  // (ADR 0091), jamais par son identifiant.
  imports: [SharedModule, UiModule, UserPickerModule, MatRadioModule],
  exports: [CapaActionDialogComponent]
})
export class CapaActionDialogModule {}
