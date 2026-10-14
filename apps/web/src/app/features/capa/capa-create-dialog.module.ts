import { NgModule } from '@angular/core';
import { MatRadioModule } from '@angular/material/radio';

import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { UserPickerModule } from '../../shared/user-picker/user-picker.module';
import { CapaCreateDialogComponent } from './pages/capa-create-dialog/capa-create-dialog.component';

/**
 * LA fenêtre de création d'une CAPA, seule, pour que chaque section l'ouvre
 * (ADR 0088) : la liste CAPA, une non-conformité, un constat d'audit, une fiche
 * risque, une case de la matrice SMI. Une seule fenêtre : des jumelles
 * divergeraient au premier champ ajouté d'un seul côté.
 *
 * <p>Module à part et non {@code CapaModule} : celui-ci porte ses routes via
 * {@code RouterModule.forChild}, et l'importer ailleurs grefferait les écrans
 * CAPA à une adresse qui n'est pas la leur.
 */
@NgModule({
  declarations: [CapaCreateDialogComponent],
  // MatRadioModule n'est pas réexporté par SharedModule : la nature, corrective
  // ou préventive, est une question fermée à deux réponses.
  // UserPickerModule : le responsable se choisit dans l'annuaire (ADR 0091).
  imports: [SharedModule, UiModule, UserPickerModule, MatRadioModule],
  exports: [CapaCreateDialogComponent]
})
export class CapaCreateDialogModule {}
